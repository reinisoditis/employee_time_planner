package com.example.scheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.domain.ShiftAssignment;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints (H1-H5) with weight β = 10000
                minimumEmployeesPerShift(constraintFactory),           // H1
                noTwoShiftsSameDay(constraintFactory),                 // H2
                maxWorkDaysPerWeek(constraintFactory),                 // H3
                noWorkDuringUnavailability(constraintFactory),         // H4
                noDuplicateAssignmentSameShift(constraintFactory),     // H5

                // Soft constraints (P1-P5) with varying weights α
                evenShiftDistribution(constraintFactory),              // P1: α1 = 100
                weekendDayOff(constraintFactory),                      // P2: α2 = 50
                noMorningAfterEvening(constraintFactory),              // P3: α3 = 30
                respectShiftPreferences(constraintFactory),            // P4: α4 = 20
                avoidUnusedEmployees(constraintFactory),               // P5: a5 = 100
        };
    }

    // ==================== HARD CONSTRAINTS ====================

    /**
     * H1: Minimum 2 employees per shift
     * Penalty: (max(0, 2 - assignedCount))² × 10000 per shift-day combination
     */
    private Constraint minimumEmployeesPerShift(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> assignment.getEmployee() != null)
                .groupBy(ShiftAssignment::getDate, ShiftAssignment::getShiftType, ConstraintCollectors.count())
                .filter((date, shiftType, count) -> count < 2)
                .penalize(HardSoftScore.ONE_HARD,
                        (date, shiftType, count) -> {
                            int shortage = 2 - count.intValue();
                            return shortage * shortage * 10000;
                        })
                .asConstraint("H1: Minimum 2 employees per shift");
    }

    /**
     * H2: No two shifts on the same day for the same employee
     * Penalty: 10000 per violation
     */
    private Constraint noTwoShiftsSameDay(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getDate),
                        Joiners.equal(ShiftAssignment::getEmployee))
                .filter((assignment1, assignment2) ->
                        assignment1.getEmployee() != null && assignment2.getEmployee() != null &&
                        !assignment1.getShift().getId().equals(assignment2.getShift().getId()))
                .penalize(HardSoftScore.ONE_HARD, (assignment1, assignment2) -> 10000)
                .asConstraint("H2: No two shifts same day");
    }

    /**
     * H3: Maximum 5 work days per week per employee
     * Penalty: (max(0, workDays - 5))² × 10000 per employee per week
     */
    private Constraint maxWorkDaysPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> assignment.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee,
                        assignment -> getWeekOfYear(assignment.getDate()),
                        ConstraintCollectors.countDistinct(ShiftAssignment::getDate))
                .filter((employee, week, distinctDays) -> distinctDays > 5)
                .penalize(HardSoftScore.ONE_HARD,
                        (employee, week, distinctDays) -> {
                            int excess = distinctDays - 5;
                            return excess * excess * 10000;
                        })
                .asConstraint("H3: Max 5 work days per week");
    }

    /**
     * H4: No work during employee unavailability
     * Penalty: 10000 per violation
     */
    private Constraint noWorkDuringUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> {
                    if (assignment.getEmployee() == null) {
                        return false;
                    }
                    Employee employee = assignment.getEmployee();
                    if (employee.getUnavailableDates() == null) {
                        return false;
                    }
                    return employee.getUnavailableDates().contains(assignment.getDate());
                })
                .penalize(HardSoftScore.ONE_HARD, assignment -> 10000)
                .asConstraint("H4: No work during unavailability");
    }
    /**
     * H5: No duplicate assignment of employee for the same shift
     * Penalty: 10000 per violation
     */
    private Constraint noDuplicateAssignmentSameShift(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.equal(ShiftAssignment::getShift)
                )
                .filter((a1, a2) ->
                        a1.getEmployee() != null &&
                                a1.getShift() != null &&
                                a2.getEmployee() != null &&
                                a2.getShift() != null)
                .penalize(HardSoftScore.ONE_HARD, (a1,a2) -> 10000)
                .asConstraint("H5: No duplicate assignment to same shift");
    }

    // ==================== SOFT CONSTRAINTS ====================

    /**
     * P1: Even shift distribution among employees
     * Penalty: (totalShifts - 3.5)² × 100 per employee
     * Target: 21 shifts / 6 employees = 3.5 shifts per employee
     */
    private Constraint evenShiftDistribution(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> assignment.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee, ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT,
                        (employee, shiftCount) -> {
                            double deviation = shiftCount - 3.5;
                            return (int) (deviation * deviation * 100);
                        })
                .asConstraint("P1: Even shift distribution");
    }



    /**
     * P2: Respect employee's weekend day off preference
     * Penalty: 50 per employee who wants weekend day off but works both Saturday and Sunday
     */
    private Constraint weekendDayOff(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> assignment.getEmployee() != null &&
                               assignment.getEmployee().isWantsWeekendDayOff())
                .groupBy(ShiftAssignment::getEmployee,
                        assignment -> getWeekOfYear(assignment.getDate()),
                        ConstraintCollectors.toSet(assignment -> assignment.getDate().getDayOfWeek()))
                .filter((employee, week, daysWorked) ->
                        daysWorked.contains(DayOfWeek.SATURDAY) &&
                        daysWorked.contains(DayOfWeek.SUNDAY))
                .penalize(HardSoftScore.ONE_SOFT, (employee, week, daysWorked) -> 50)
                .asConstraint("P2: Weekend day off preference");
    }

    /**
     * P3: No morning shift after evening shift
     * Penalty: 30 per violation
     */
    private Constraint noMorningAfterEvening(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(ShiftAssignment.class)
                .filter((assignment1, assignment2) -> {
                    if (assignment1.getEmployee() == null || assignment2.getEmployee() == null) {
                        return false;
                    }
                    if (!assignment1.getEmployee().getId().equals(assignment2.getEmployee().getId())) {
                        return false;
                    }

                    // Check if assignment1 is evening and assignment2 is morning on consecutive days
                    if ("EVENING".equals(assignment1.getShiftType()) &&
                        "MORNING".equals(assignment2.getShiftType())) {
                        return assignment2.getDate().equals(assignment1.getDate().plusDays(1));
                    }

                    // Check if assignment2 is evening and assignment1 is morning on consecutive days
                    if ("EVENING".equals(assignment2.getShiftType()) &&
                        "MORNING".equals(assignment1.getShiftType())) {
                        return assignment1.getDate().equals(assignment2.getDate().plusDays(1));
                    }

                    return false;
                })
                .penalize(HardSoftScore.ONE_SOFT, (assignment1, assignment2) -> 30)
                .asConstraint("P3: No morning after evening shift");
    }

    /**
     * P4: Respect employee shift type preferences
     * Penalty: (1 - preference) × 20 per shift
     * preference: 1.0 = high desire, 0.0 = low desire
     */
    private Constraint respectShiftPreferences(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ShiftAssignment.class)
                .filter(assignment -> assignment.getEmployee() != null &&
                               assignment.getShiftType() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        assignment -> {
                            Employee employee = assignment.getEmployee();
                            if (employee.getShiftTypePreferences().isEmpty()) {
                                return 0;
                            }
                            Double preference = employee.getShiftTypePreferences()
                                    .getOrDefault(assignment.getShiftType(), 0.5);
                            return (int) ((1.0 - preference) * 20);
                        })
                .asConstraint("P4: Respect shift preferences");
    }

    /**
     * P5: Avoid not using all the employees for the schedule
     * Penalty: 100 per violation
     */
    private Constraint avoidUnusedEmployees(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Employee.class)
                .ifNotExists(ShiftAssignment.class,
                        Joiners.equal(e -> e, ShiftAssignment::getEmployee))
                .penalize(HardSoftScore.ONE_SOFT, e -> 100)
                .asConstraint("P5: Avoid unused employees");
    }

    // ==================== HELPER METHODS ====================

    private String getWeekOfYear(LocalDate date) {
        return date.getYear() + "-W" + date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}

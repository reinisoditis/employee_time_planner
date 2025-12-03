package com.example.scheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import com.example.scheduler.domain.Shift;

import java.time.Duration;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                noOverlappingShifts(constraintFactory),
                
                // Soft constraints
                minimizeUnassignedShifts(constraintFactory)
        };
    }

    // Hard constraint: An employee cannot work overlapping shifts
    private Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Shift.class)
                .filter((shift1, shift2) -> {
                    if (shift1.getEmployee() == null || shift2.getEmployee() == null) {
                        return false;
                    }
                    if (!shift1.getEmployee().getId().equals(shift2.getEmployee().getId())) {
                        return false;
                    }
                    // Check if shifts overlap
                    return shift1.getStartTime().isBefore(shift2.getEndTime()) 
                            && shift2.getStartTime().isBefore(shift1.getEndTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No overlapping shifts");
    }

    // Soft constraint: Minimize unassigned shifts
    private Constraint minimizeUnassignedShifts(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Shift.class)
                .filter(shift -> shift.getEmployee() == null)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Minimize unassigned shifts");
    }
}

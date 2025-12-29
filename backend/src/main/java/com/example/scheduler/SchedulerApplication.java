package com.example.scheduler;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.domain.ShiftAssignment;
import com.example.scheduler.solver.ScheduleConstraintProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//@SpringBootApplication
@Slf4j
public class SchedulerApplication {

    public static void main(String[] args) {
        // Uncomment to run as Spring Boot application
        // SpringApplication.run(SchedulerApplication.class, args);
        
        // Run Timefold optimizer test
        runTimefoldTest();
    }

    private static void runTimefoldTest() {
        System.out.println("=== Starting Timefold Scheduler Test ===\n");

        Schedule problem = generateTestData();
        printProblemDetails(problem);

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(Schedule.class)
                .withEntityClasses(ShiftAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofSeconds(10));

        SolverFactory<Schedule> solverFactory = SolverFactory.create(solverConfig);
        Solver<Schedule> solver = solverFactory.buildSolver();

        System.out.println("Solving... (max 10 seconds)\n");
        Schedule solution = solver.solve(problem);


        SolutionManager<Schedule, HardSoftScore> solutionManager = SolutionManager.create(solverFactory);
        log.info(solutionManager.explain(solution).getSummary());

        printSolution(solution);
    }

    private static void printSolution(Schedule solution) {
        System.out.println("=== Solution ===");
        System.out.println("Score: " + solution.getScore());
        System.out.println("\nShift Assignments:");

        int assigned = 0;
        int unassigned = 0;

        for (ShiftAssignment assignment : solution.getShiftAssignments()) {
            if (assignment.getEmployee() != null) {
                System.out.println("  ✓ " + assignment.getShift().getId() +
                        " -> " + assignment.getEmployee().getName() +
                        " [" + assignment.getShift().getStartTime().toLocalDate() +
                        " " + assignment.getShift().getStartTime().toLocalTime() +
                        " - " + assignment.getShift().getEndTime().toLocalTime() + "]");
                assigned++;
            } else {
                System.out.println("  ✗ " + assignment.getShift().getId() + " -> UNASSIGNED" +
                        " [" + assignment.getShift().getStartTime().toLocalDate() +
                        " " + assignment.getShift().getStartTime().toLocalTime() +
                        " - " + assignment.getShift().getEndTime().toLocalTime() + "]");
                unassigned++;
            }
        }

        System.out.println("\n--- Summary ---");
        System.out.println("Assigned shifts: " + assigned);
        System.out.println("Unassigned shifts: " + unassigned);
        System.out.println("Total assignments: " + solution.getShiftAssignments().size());
        System.out.println("\nFinal Score: " + solution.getScore());
        System.out.println("  Hard Score (violations): " + solution.getScore().hardScore());
        System.out.println("  Soft Score (optimization): " + solution.getScore().softScore());
    }


    private static Schedule generateTestData() {
        // Create 8 employees to allow 2 per shift with max 5 days/week constraint
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("emp1", "Alice"));
        employees.add(new Employee("emp2", "Bob"));
        employees.add(new Employee("emp3", "Charlie"));
        employees.add(new Employee("emp4", "Diana"));
        employees.add(new Employee("emp5", "Eve"));
        employees.add(new Employee("emp6", "Frank"));
        employees.add(new Employee("emp7", "Grace"));
        employees.add(new Employee("emp8", "Henry"));

        // Simplified shifts - one per day for testing
        LocalDateTime monday = LocalDateTime.of(2025, 1, 6, 9, 0);
        List<Shift> shifts = new ArrayList<>();
        List<ShiftAssignment> shiftAssignments = new ArrayList<>();

        for (int day = 0; day < 5; day++) {
            LocalDateTime dayStart = monday.plusDays(day);
            Shift shift = new Shift(
                    "shift_day" + (day + 1),
                    dayStart.withHour(9),
                    dayStart.withHour(17),
                    "DAY",
                    2 // Require 2 employees
            );
            shifts.add(shift);

            // Create 2 assignments for this shift
            shiftAssignments.add(new ShiftAssignment("assign_day" + (day + 1) + "_1", shift, null));
            shiftAssignments.add(new ShiftAssignment("assign_day" + (day + 1) + "_2", shift, null));
        }

        return new Schedule(employees, shifts, shiftAssignments);
    }



    private static void printProblemDetails(Schedule problem) {
        System.out.println("--- Problem Details ---");
        System.out.println("Employees: " + problem.getEmployees().size());
        for (Employee emp : problem.getEmployees()) {
            System.out.println("  - " + emp.getName() + " (" + emp.getId() + ")");
        }
        
        System.out.println("\nShifts: " + problem.getShifts().size());
        for (Shift shift : problem.getShifts()) {
            System.out.println("  - " + shift.getId() + 
                    " [" + shift.getStartTime() + " to " + shift.getEndTime() + "]");
        }
        System.out.println();
    }
}

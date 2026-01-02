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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class SchedulerApplication {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void main(String[] args) throws IOException {
        // Generate test data JSON files
        generateTestDataJsonFiles();

        // Run Timefold optimizer test with JSON input
        runTimefoldTest("data/medium-schedule.json");
    }

    private static void runTimefoldTest(String jsonFilePath) throws IOException {
        System.out.println("=== Starting Timefold Scheduler Test ===\n");

        Schedule problem = loadScheduleFromJson(jsonFilePath);
        printProblemDetails(problem);

        SolverFactory<Schedule> solverFactory = SolverFactory.createFromXmlResource("solverConfig.xml");
        Solver<Schedule> solver = solverFactory.buildSolver();

        System.out.println("Solving... (max 30 seconds)\n");
        Schedule solution = solver.solve(problem);

        SolutionManager<Schedule, HardSoftScore> solutionManager = SolutionManager.create(solverFactory);
        log.info(solutionManager.explain(solution).getSummary());

        printSolution(solution);
    }

    private static Schedule loadScheduleFromJson(String filePath) throws IOException {
        File file = new File(filePath);
        System.out.println("Loading schedule from: " + file.getAbsolutePath());
        return objectMapper.readValue(file, Schedule.class);
    }

    private static void saveScheduleToJson(Schedule schedule, String filePath) throws IOException {
        File file = new File(filePath);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, schedule);
        System.out.println("\nSolution saved to: " + file.getAbsolutePath());
    }

    private static void generateTestDataJsonFiles() throws IOException {
        Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
            System.out.println("Created data directory: " + dataDir.toAbsolutePath());
        }

        // Generate small problem (8 employees, 5 days, 2 per shift, 3 shifts per day)
        Schedule smallSchedule = generateTestData(8, 5, 2);
        saveScheduleToJson(smallSchedule, "data/small-schedule.json");
        System.out.println("Generated small-schedule.json (8 employees, 5 days, 3 shifts/day, 2 per shift = 30 assignments)\n");

        // Generate medium problem (20 employees, 14 days, 3 per shift, 3 shifts per day)
        Schedule mediumSchedule = generateTestData(20, 14, 3);
        saveScheduleToJson(mediumSchedule, "data/medium-schedule.json");
        System.out.println("Generated medium-schedule.json (20 employees, 14 days, 3 shifts/day, 3 per shift = 126 assignments)\n");

        // Generate large problem (50 employees, 28 days, 4 per shift, 3 shifts per day)
        Schedule largeSchedule = generateTestData(50, 28, 4);
        saveScheduleToJson(largeSchedule, "data/large-schedule.json");
        System.out.println("Generated large-schedule.json (50 employees, 28 days, 3 shifts/day, 4 per shift = 336 assignments)\n");
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

    private static Schedule generateTestData(int numEmployees, int numDays, int employeesPerShift) {
        String[] employeeNames = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry",
                "Ivy", "Jack", "Kate", "Leo", "Mia", "Noah", "Olivia", "Paul", "Quinn", "Rose", "Sam", "Tara",
                "Uma", "Victor", "Wendy", "Xander", "Yara", "Zane", "Amy", "Ben", "Cara", "Dan",
                "Emma", "Finn", "Gina", "Hugo", "Iris", "Jake", "Kira", "Liam", "Maya", "Nina",
                "Owen", "Piper", "Quill", "Rita", "Seth", "Tina", "Umar", "Vera", "Will", "Xena"};

        List<Employee> employees = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 6, 9, 0);

        for (int i = 0; i < numEmployees; i++) {
            String name = i < employeeNames.length ? employeeNames[i] : "Employee" + (i + 1);
            String empId = "emp" + (i + 1);

            // Generate shift type preferences (some employees prefer certain shifts)
            Map<String, Double> shiftPreferences = new HashMap<>();
            if (i % 3 == 0) {
                // Prefers morning shifts
                shiftPreferences.put("MORNING", 1.0);
                shiftPreferences.put("DAY", 0.5);
                shiftPreferences.put("EVENING", 0.2);
            } else if (i % 3 == 1) {
                // Prefers evening shifts
                shiftPreferences.put("MORNING", 0.3);
                shiftPreferences.put("DAY", 0.6);
                shiftPreferences.put("EVENING", 1.0);
            } else {
                // No strong preference
                shiftPreferences.put("MORNING", 0.7);
                shiftPreferences.put("DAY", 0.8);
                shiftPreferences.put("EVENING", 0.7);
            }

            // Generate unavailable dates (some employees have time off)
            Set<LocalDate> unavailableDates = new HashSet<>();
            if (i % 5 == 0 && numDays >= 7) {
                // Week off in the middle
                int weekOffStart = numDays / 3;
                for (int d = 0; d < 7; d++) {
                    unavailableDates.add(startDate.plusDays(weekOffStart + d).toLocalDate());
                }
            } else if (i % 7 == 0 && numDays >= 3) {
                // Random days off
                unavailableDates.add(startDate.plusDays(2).toLocalDate());
                unavailableDates.add(startDate.plusDays(5).toLocalDate());
                if (numDays >= 10) {
                    unavailableDates.add(startDate.plusDays(9).toLocalDate());
                }
            }

            // Some employees want weekend days off
            boolean wantsWeekendDayOff = i % 4 == 0;

            Employee employee = new Employee(empId, name, unavailableDates, shiftPreferences, wantsWeekendDayOff);
            employees.add(employee);
        }

        List<Shift> shifts = new ArrayList<>();
        List<ShiftAssignment> shiftAssignments = new ArrayList<>();

        // Create three shifts per day: MORNING, DAY, EVENING
        for (int day = 0; day < numDays; day++) {
            LocalDateTime dayStart = startDate.plusDays(day);

            // Morning shift: 6:00 - 14:00
            Shift morningShift = new Shift(
                    "shift_day" + (day + 1) + "_morning",
                    dayStart.withHour(6).withMinute(0),
                    dayStart.withHour(14).withMinute(0),
                    "MORNING",
                    employeesPerShift
            );
            shifts.add(morningShift);

            // Day shift: 9:00 - 17:00
            Shift dayShift = new Shift(
                    "shift_day" + (day + 1) + "_day",
                    dayStart.withHour(9).withMinute(0),
                    dayStart.withHour(17).withMinute(0),
                    "DAY",
                    employeesPerShift
            );
            shifts.add(dayShift);

            // Evening shift: 14:00 - 22:00
            Shift eveningShift = new Shift(
                    "shift_day" + (day + 1) + "_evening",
                    dayStart.withHour(14).withMinute(0),
                    dayStart.withHour(22).withMinute(0),
                    "EVENING",
                    employeesPerShift
            );
            shifts.add(eveningShift);

            // Create assignments for each shift
            for (int i = 0; i < employeesPerShift; i++) {
                shiftAssignments.add(new ShiftAssignment(
                        "assign_day" + (day + 1) + "_morning_" + (i + 1),
                        morningShift,
                        null
                ));
            }

            for (int i = 0; i < employeesPerShift; i++) {
                shiftAssignments.add(new ShiftAssignment(
                        "assign_day" + (day + 1) + "_day_" + (i + 1),
                        dayShift,
                        null
                ));
            }

            for (int i = 0; i < employeesPerShift; i++) {
                shiftAssignments.add(new ShiftAssignment(
                        "assign_day" + (day + 1) + "_evening_" + (i + 1),
                        eveningShift,
                        null
                ));
            }
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

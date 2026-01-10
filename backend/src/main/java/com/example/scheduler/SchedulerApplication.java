package com.example.scheduler;
import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.domain.ShiftAssignment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class SchedulerApplication {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void main(String[] args) throws IOException {
        // Generate test data JSON files
        //generateAllTestData();

        // Test with a specific problem
        runTimefoldTest("data/hard-large.json");

        //runBenchmark();
    }

    private static void runTimefoldTest(String jsonFilePath) throws IOException {
        System.out.println("=== Starting Timefold Scheduler Test ===\n");

        Schedule problem = loadScheduleFromJson(jsonFilePath);
        printProblemDetails(problem);

        SolverFactory<Schedule> solverFactory = SolverFactory.createFromXmlResource("solverConfig.xml");
        Solver<Schedule> solver = solverFactory.buildSolver();

        // Track scores at different stages
        final HardSoftScore[] constructionScore = {null};
        final long[] constructionEndTime = {0};
        final long startTime = System.currentTimeMillis();
        final int[] improvementCount = {0};

        solver.addEventListener(event -> {
            long elapsed = System.currentTimeMillis() - startTime;
            HardSoftScore newScore = (HardSoftScore) event.getNewBestScore();

            if (constructionScore[0] == null) {
                constructionScore[0] = newScore;
                constructionEndTime[0] = System.currentTimeMillis();
                System.out.println("\n>>> Construction Heuristic completed!");
                System.out.println("    Score: " + constructionScore[0] + " (at " + elapsed + "ms)");
            } else {
                improvementCount[0]++;
            }
            System.out.flush();
        });

        Schedule solution = solver.solve(problem);
        long endTime = System.currentTimeMillis();

        System.out.println("\n>>> Solving complete. Total LS improvements: " + improvementCount[0]);

        // Print phase comparison
        System.out.println("\n=== Phase Comparison ===");
        System.out.println("Construction Heuristic:");
        System.out.println("  Score: " + constructionScore[0]);
        System.out.println("  Time:  " + (constructionEndTime[0] - startTime) + "ms");
        System.out.println("\nLocal Search:");
        System.out.println("  Score: " + solution.getScore());
        System.out.println("  Time:  " + (endTime - constructionEndTime[0]) + "ms");
        System.out.println("\nImprovement:");
        if (constructionScore[0] != null) {
            int hardImproved = solution.getScore().hardScore() - constructionScore[0].hardScore();
            int softImproved = solution.getScore().softScore() - constructionScore[0].softScore();
            System.out.println("  Hard: " + (hardImproved >= 0 ? "+" : "") + hardImproved);
            System.out.println("  Soft: " + (softImproved >= 0 ? "+" : "") + softImproved);
        }
        System.out.println("  Total time: " + (endTime - startTime) + "ms");

        SolutionManager<Schedule, HardSoftScore> solutionManager = SolutionManager.create(solverFactory);
        log.info(solutionManager.explain(solution).getSummary());

        //printSolution(solution);
    }

    private static void runBenchmark() throws IOException {
        System.out.println("=== Starting Timefold Benchmark ===\n");

        List<Schedule> problemList = new ArrayList<>();
        problemList.add(loadScheduleFromJson("data/medium-small.json"));
        problemList.add(loadScheduleFromJson("data/hard-medium.json"));
        problemList.add(loadScheduleFromJson("data/hard-large.json"));

        PlannerBenchmarkFactory benchmarkFactory =
                PlannerBenchmarkFactory.createFromXmlResource("benchmarkConfig.xml");
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(
                problemList.toArray(new Schedule[0])
        );

        benchmark.benchmarkAndShowReportInBrowser();
    }

    private static Schedule generateProblem(int numEmployees, int numWeeks, int shiftsPerDay,
                                            int employeesPerShift, int difficulty) {

        int numDays = numWeeks * 7;
        int totalShiftSlots = numDays * shiftsPerDay * employeesPerShift;
        int maxShiftsPerEmployee = numWeeks * 5;
        int minEmployeesRequired = (int) Math.ceil((double) totalShiftSlots / maxShiftsPerEmployee);

        // Validate feasibility
        if (numEmployees < minEmployeesRequired) {
            throw new IllegalArgumentException(
                    String.format("Infeasible: need at least %d employees for %d shift slots (have %d)",
                            minEmployeesRequired, totalShiftSlots, numEmployees));
        }

        double utilizationRate = (double) totalShiftSlots / (numEmployees * maxShiftsPerEmployee);
        System.out.printf("Problem: %d employees, %d days, %d shifts total%n", numEmployees, numDays, totalShiftSlots);
        System.out.printf("Utilization: %.1f%% (higher = harder)%n", utilizationRate * 100);

        Random random = new Random(42); // Fixed seed for reproducibility
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 6, 0, 0); // Start on Monday

        List<Employee> employees = generateEmployees(numEmployees, numDays, startDate, difficulty, random);
        List<Shift> shifts = new ArrayList<>();
        List<ShiftAssignment> shiftAssignments = new ArrayList<>();

        generateShiftsAndAssignments(numDays, shiftsPerDay, employeesPerShift, startDate, shifts, shiftAssignments);

        return new Schedule(employees, shifts, shiftAssignments);
    }

    private static List<Employee> generateEmployees(int numEmployees, int numDays, LocalDateTime startDate,
                                                    int difficulty, Random random) {
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry",
                "Ivy", "Jack", "Kate", "Leo", "Mia", "Noah", "Olivia", "Paul", "Quinn", "Rose", "Sam", "Tara",
                "Uma", "Victor", "Wendy", "Xander", "Yara", "Zane", "Amy", "Ben", "Cara", "Dan",
                "Emma", "Finn", "Gina", "Hugo", "Iris", "Jake", "Kira", "Liam", "Maya", "Nina",
                "Owen", "Piper", "Quill", "Rita", "Seth", "Tina", "Umar", "Vera", "Will", "Xena"};

        List<Employee> employees = new ArrayList<>();

        // Calculate safe unavailability limits
        // Each week, employee can miss at most 2 days and still work 5 (meeting H3 capacity)
        int numWeeks = (int) Math.ceil(numDays / 7.0);
        int maxUnavailableDays = numWeeks; // Conservative: max 1 day off per week on average

        for (int i = 0; i < numEmployees; i++) {
            String name = i < names.length ? names[i] : "Employee" + (i + 1);
            String empId = "emp" + (i + 1);

            // Generate shift preferences based on difficulty
            Map<String, Double> preferences = generatePreferences(i, difficulty, random);

            // Generate unavailability based on difficulty
            Set<LocalDate> unavailableDates = generateUnavailability(
                    numDays, maxUnavailableDays, difficulty, startDate, random);

            // Weekend preference based on difficulty
            double weekendPrefChance = switch (difficulty) {
                case 1 -> 0.3;  // Easy: 30% want weekend off
                case 2 -> 0.5;  // Medium: 50%
                case 3 -> 0.7;  // Hard: 70%
                default -> 0.5;
            };
            boolean wantsWeekendDayOff = random.nextDouble() < weekendPrefChance;

            employees.add(new Employee(empId, name, unavailableDates, preferences, wantsWeekendDayOff));
        }

        return employees;
    }

    /**
     * Generates shift preferences based on employee index and difficulty.
     */
    private static Map<String, Double> generatePreferences(int employeeIndex, int difficulty, Random random) {
        Map<String, Double> preferences = new HashMap<>();

        if (difficulty == 1) {
            // Easy: Everyone is fairly flexible
            preferences.put("MORNING", 0.6 + random.nextDouble() * 0.4);  // 0.6-1.0
            preferences.put("DAY", 0.6 + random.nextDouble() * 0.4);      // 0.6-1.0
            preferences.put("EVENING", 0.6 + random.nextDouble() * 0.4);  // 0.6-1.0
        } else if (difficulty == 2) {
            // Medium: People have clear preferences but no absolutes
            int type = employeeIndex % 3;
            if (type == 0) {
                preferences.put("MORNING", 0.9);
                preferences.put("DAY", 0.5);
                preferences.put("EVENING", 0.2);
            } else if (type == 1) {
                preferences.put("MORNING", 0.2);
                preferences.put("DAY", 0.5);
                preferences.put("EVENING", 0.9);
            } else {
                preferences.put("MORNING", 0.4);
                preferences.put("DAY", 0.9);
                preferences.put("EVENING", 0.4);
            }
        } else {
            // Hard: Strong preferences with some absolute refusals (0.0)
            int type = employeeIndex % 5;
            switch (type) {
                case 0 -> { // Morning person
                    preferences.put("MORNING", 1.0);
                    preferences.put("DAY", 0.4);
                    preferences.put("EVENING", 0.1);
                }
                case 1 -> { // Evening person
                    preferences.put("MORNING", 0.1);
                    preferences.put("DAY", 0.4);
                    preferences.put("EVENING", 1.0);
                }
                case 2 -> { // Day only
                    preferences.put("MORNING", 0.2);
                    preferences.put("DAY", 1.0);
                    preferences.put("EVENING", 0.2);
                }
                case 3 -> { // Morning/Day, hates evening
                    preferences.put("MORNING", 0.8);
                    preferences.put("DAY", 0.8);
                    preferences.put("EVENING", 0.1);
                }
                case 4 -> { // Flexible
                    preferences.put("MORNING", 0.6);
                    preferences.put("DAY", 0.7);
                    preferences.put("EVENING", 0.6);
                }
            }
        }

        return preferences;
    }

    private static Set<LocalDate> generateUnavailability(int numDays, int maxDays, int difficulty,
                                                         LocalDateTime startDate, Random random) {
        Set<LocalDate> unavailable = new HashSet<>();

        // Probability of having any unavailability
        double hasUnavailabilityChance = switch (difficulty) {
            case 1 -> 0.2;  // Easy: 20% have days off
            case 2 -> 0.4;  // Medium: 40%
            case 3 -> 0.5;  // Hard: 50%
            default -> 0.3;
        };

        if (random.nextDouble() < hasUnavailabilityChance) {
            // Number of days off (1 to maxDays based on difficulty)
            int daysOff = switch (difficulty) {
                case 1 -> 1;
                case 2 -> 1 + random.nextInt(Math.min(2, maxDays));
                case 3 -> 1 + random.nextInt(Math.min(maxDays, 3));
                default -> 1;
            };

            for (int d = 0; d < daysOff && unavailable.size() < maxDays; d++) {
                int dayOffset = random.nextInt(numDays);
                unavailable.add(startDate.plusDays(dayOffset).toLocalDate());
            }
        }

        return unavailable;
    }

    private static void generateShiftsAndAssignments(int numDays, int shiftsPerDay, int employeesPerShift,
                                                     LocalDateTime startDate,
                                                     List<Shift> shifts, List<ShiftAssignment> assignments) {
        String[] shiftTypes = {"MORNING", "DAY", "EVENING"};
        int[][] shiftTimes = {{6, 14}, {9, 17}, {14, 22}}; // start hour, end hour

        for (int day = 0; day < numDays; day++) {
            LocalDateTime dayStart = startDate.plusDays(day);

            for (int s = 0; s < shiftsPerDay && s < 3; s++) {
                String shiftType = shiftTypes[s];
                Shift shift = new Shift(
                        "shift_d" + (day + 1) + "_" + shiftType.toLowerCase(),
                        dayStart.withHour(shiftTimes[s][0]).withMinute(0),
                        dayStart.withHour(shiftTimes[s][1]).withMinute(0),
                        shiftType,
                        employeesPerShift
                );
                shifts.add(shift);

                // Create empty assignments for this shift
                for (int e = 0; e < employeesPerShift; e++) {
                    assignments.add(new ShiftAssignment(
                            "assign_d" + (day + 1) + "_" + shiftType.toLowerCase() + "_" + (e + 1),
                            shift,
                            null  // Unassigned - solver will fill this
                    ));
                }
            }
        }
    }

    private static void generateAllTestData() throws IOException {
        Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        System.out.println("=== Generating Test Data ===\n");

        // EASY problems - lots of slack, flexible employees
        System.out.println("--- EASY Problems ---");
        saveScheduleToJson(generateProblem(12, 1, 3, 2, 1), "data/easy-small.json");   // 42 slots, 60 capacity
        saveScheduleToJson(generateProblem(20, 2, 3, 2, 1), "data/easy-medium.json");  // 84 slots, 200 capacity
        saveScheduleToJson(generateProblem(30, 4, 3, 2, 1), "data/easy-large.json");   // 168 slots, 600 capacity

        // MEDIUM problems - moderate pressure, some preference conflicts
        System.out.println("\n--- MEDIUM Problems ---");
        saveScheduleToJson(generateProblem(10, 1, 3, 2, 2), "data/medium-small.json");  // 42 slots, 50 capacity
        saveScheduleToJson(generateProblem(18, 2, 3, 2, 2), "data/medium-medium.json"); // 84 slots, 180 capacity
        saveScheduleToJson(generateProblem(40, 4, 3, 3, 2), "data/medium-large.json");  // 252 slots, 800 capacity

        // HARD problems - tight capacity, strong preference conflicts
        System.out.println("\n--- HARD Problems ---");
        saveScheduleToJson(generateProblem(10, 1, 3, 2, 3), "data/hard-small.json");   // 42 slots, 50 capacity (84%)
        saveScheduleToJson(generateProblem(20, 2, 3, 3, 3), "data/hard-medium.json");  // 126 slots, 200 capacity (63%)
        saveScheduleToJson(generateProblem(50, 4, 3, 4, 3), "data/hard-large.json");   // 336 slots, 1000 capacity (34%)

        System.out.println("\n=== Test Data Generation Complete ===");
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

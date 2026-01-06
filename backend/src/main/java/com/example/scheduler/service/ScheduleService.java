package com.example.scheduler.service;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.domain.ShiftAssignment;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.amqp.AbstractRabbitListenerContainerFactoryConfigurer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final SolverManager<Schedule, UUID> solverManager;
    private final List<Employee> employees = new ArrayList<>();
    private final List<Shift> shifts = new ArrayList<>();

    private final List<ShiftAssignment> shiftAssignments = new ArrayList<>();

    @PostConstruct
    private void initializeSampleData() {
        employees.clear();
        shifts.clear();
        shiftAssignments.clear();

        java.nio.file.Path jsonPath = java.nio.file.Paths.get("/data/small-schedule.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        try {
            if (java.nio.file.Files.exists(jsonPath)) {
                try (java.io.InputStream is = java.nio.file.Files.newInputStream(jsonPath)) {
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(is);

                    // parse employees
                    java.util.Map<String, com.example.scheduler.domain.Employee> empById = new java.util.HashMap<>();
                    com.fasterxml.jackson.databind.JsonNode employeesNode = root.path("employees");
                    if (employeesNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode en : employeesNode) {
                            String id = en.path("id").asText();
                            String name = en.path("name").asText();
                            com.example.scheduler.domain.Employee emp = new com.example.scheduler.domain.Employee(id, name);

                            // unavailableDates as [year,month,day]
                            java.util.Set<java.time.LocalDate> unavailable = new java.util.HashSet<>();
                            com.fasterxml.jackson.databind.JsonNode udates = en.path("unavailableDates");
                            if (udates.isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode ud : udates) {
                                    if (ud.isArray() && ud.size() >= 3) {
                                        int y = ud.get(0).asInt();
                                        int m = ud.get(1).asInt();
                                        int d = ud.get(2).asInt();
                                        unavailable.add(java.time.LocalDate.of(y, m, d));
                                    }
                                }
                            }
                            emp.setUnavailableDates(unavailable);

                            // shiftTypePreferences
                            java.util.Map<String, Double> prefs = new java.util.HashMap<>();
                            com.fasterxml.jackson.databind.JsonNode prefsNode = en.path("shiftTypePreferences");
                            if (prefsNode.isObject()) {
                                prefsNode.fields().forEachRemaining(entry -> prefs.put(entry.getKey(), entry.getValue().asDouble()));
                            }
                            emp.setShiftTypePreferences(prefs);

                            emp.setWantsWeekendDayOff(en.path("wantsWeekendDayOff").asBoolean(false));

                            employees.add(emp);
                            empById.put(id, emp);
                        }
                    }

                    // parse shifts
                    java.util.Map<String, com.example.scheduler.domain.Shift> shiftById = new java.util.HashMap<>();
                    com.fasterxml.jackson.databind.JsonNode shiftsNode = root.path("shifts");
                    if (shiftsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode sn : shiftsNode) {
                            String id = sn.path("id").asText();
                            com.fasterxml.jackson.databind.JsonNode st = sn.path("startTime");
                            com.fasterxml.jackson.databind.JsonNode et = sn.path("endTime");
                            java.time.LocalDateTime start = java.time.LocalDateTime.of(
                                    st.get(0).asInt(), st.get(1).asInt(), st.get(2).asInt(),
                                    st.size() > 3 ? st.get(3).asInt() : 0,
                                    st.size() > 4 ? st.get(4).asInt() : 0
                            );
                            java.time.LocalDateTime end = java.time.LocalDateTime.of(
                                    et.get(0).asInt(), et.get(1).asInt(), et.get(2).asInt(),
                                    et.size() > 3 ? et.get(3).asInt() : 0,
                                    et.size() > 4 ? et.get(4).asInt() : 0
                            );
                            String shiftType = sn.path("shiftType").asText();
                            int required = sn.path("requiredEmployees").asInt(1);
                            com.example.scheduler.domain.Shift shift = new com.example.scheduler.domain.Shift(id, start, end, shiftType, required);
                            shifts.add(shift);
                            shiftById.put(id, shift);
                        }
                    }

                    // parse shiftAssignments
                    com.fasterxml.jackson.databind.JsonNode assignsNode = root.path("shiftAssignments");
                    if (assignsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode an : assignsNode) {
                            String id = an.path("id").asText();
                            com.fasterxml.jackson.databind.JsonNode shiftNode = an.path("shift");
                            String sid = shiftNode.path("id").asText();
                            com.example.scheduler.domain.Shift sref = shiftById.get(sid);
                            com.example.scheduler.domain.ShiftAssignment sa = new com.example.scheduler.domain.ShiftAssignment(id, sref, null);
                            shiftAssignments.add(sa);
                        }
                    }

                    // success if data loaded
                    if (!employees.isEmpty() && !shifts.isEmpty() && !shiftAssignments.isEmpty()) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // fall through to fallback sample data
        }

        // fallback sample data
        employees.add(new com.example.scheduler.domain.Employee("1", "Alice"));
        employees.add(new com.example.scheduler.domain.Employee("2", "Bob"));
        employees.add(new com.example.scheduler.domain.Employee("3", "Charlie"));
        employees.add(new com.example.scheduler.domain.Employee("4", "Diana"));
        employees.add(new com.example.scheduler.domain.Employee("5", "Eve"));
        employees.add(new com.example.scheduler.domain.Employee("6", "Frank"));

        java.time.LocalDateTime base = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int days = 5;
        for (int d = 0; d < days; d++) {
            java.time.LocalDateTime day = base.plusDays(d);
            com.example.scheduler.domain.Shift morning = new com.example.scheduler.domain.Shift("shift_" + d + "_MORNING", day.withHour(6), day.withHour(14), "MORNING", 2);
            shifts.add(morning);
            com.example.scheduler.domain.Shift dayShift = new com.example.scheduler.domain.Shift("shift_" + d + "_DAY", day.withHour(9), day.withHour(17), "DAY", 2);
            shifts.add(dayShift);
            com.example.scheduler.domain.Shift evening = new com.example.scheduler.domain.Shift("shift_" + d + "_EVENING", day.withHour(14), day.withHour(22), "EVENING", 1);
            shifts.add(evening);

            for (int i = 0; i < morning.getRequiredEmployees(); i++) {
                shiftAssignments.add(new com.example.scheduler.domain.ShiftAssignment("assign_" + morning.getId() + "_" + (i + 1), morning, null));
            }
            for (int i = 0; i < dayShift.getRequiredEmployees(); i++) {
                shiftAssignments.add(new com.example.scheduler.domain.ShiftAssignment("assign_" + dayShift.getId() + "_" + (i + 1), dayShift, null));
            }
            for (int i = 0; i < evening.getRequiredEmployees(); i++) {
                shiftAssignments.add(new com.example.scheduler.domain.ShiftAssignment("assign_" + evening.getId() + "_" + (i + 1), evening, null));
            }
        }
    }

    public Schedule solve() throws ExecutionException, InterruptedException {
        UUID problemId = UUID.randomUUID();
        Schedule problem = new Schedule(new ArrayList<>(employees), new ArrayList<>(shifts), new ArrayList<>(shiftAssignments));

        SolverJob<Schedule, UUID> solverJob = solverManager.solve(problemId, problem);
        return solverJob.getFinalBestSolution();
    }

    public List<Employee> getEmployees() {
        return new ArrayList<>(employees);
    }

    public Employee addEmployee(Employee employee) {
        if (employee.getId() == null || employee.getId().isEmpty()) {
            employee.setId(UUID.randomUUID().toString());
        }
        employees.add(employee);
        return employee;
    }

    public List<Shift> getShifts() {
        return new ArrayList<>(shifts);
    }

    public Shift addShift(Shift shift) {
        if (shift.getId() == null || shift.getId().isEmpty()) {
            shift.setId(UUID.randomUUID().toString());
        }
        shifts.add(shift);
        return shift;
    }

    public void clearShifts() {
        shifts.clear();
    }

    public void clearEmployees() {
        employees.clear();
    }

    public Schedule getCurrentSchedule() {
        return new Schedule(new ArrayList<>(employees), new ArrayList<>(shifts), new ArrayList<>(shiftAssignments));
    }
}

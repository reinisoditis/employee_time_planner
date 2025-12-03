package com.example.scheduler.service;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ScheduleService {

    private final SolverManager<Schedule, UUID> solverManager;
    private List<Employee> employees = new ArrayList<>();
    private List<Shift> shifts = new ArrayList<>();

    public ScheduleService(SolverManager<Schedule, UUID> solverManager) {
        this.solverManager = solverManager;
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Add sample employees
        employees.add(new Employee("1", "Alice"));
        employees.add(new Employee("2", "Bob"));
        employees.add(new Employee("3", "Charlie"));
        employees.add(new Employee("4", "Diana"));
    }

    public Schedule solve() throws ExecutionException, InterruptedException {
        UUID problemId = UUID.randomUUID();
        Schedule problem = new Schedule(new ArrayList<>(employees), new ArrayList<>(shifts));
        
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
        return new Schedule(new ArrayList<>(employees), new ArrayList<>(shifts));
    }
}

package com.example.scheduler.controller;

import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/employees")
    public List<Employee> getEmployees() {
        return scheduleService.getEmployees();
    }

    @PostMapping("/employees")
    public Employee addEmployee(@RequestBody Employee employee) {
        return scheduleService.addEmployee(employee);
    }

    @DeleteMapping("/employees")
    public ResponseEntity<Void> clearEmployees() {
        scheduleService.clearEmployees();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shifts")
    public List<Shift> getShifts() {
        return scheduleService.getShifts();
    }

    @PostMapping("/shifts")
    public Shift addShift(@RequestBody Shift shift) {
        return scheduleService.addShift(shift);
    }

    @DeleteMapping("/shifts")
    public ResponseEntity<Void> clearShifts() {
        scheduleService.clearShifts();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/schedule")
    public Schedule getCurrentSchedule() {
        return scheduleService.getCurrentSchedule();
    }

    @PostMapping("/solve")
    public ResponseEntity<Schedule> solve() {
        try {
            Schedule solution = scheduleService.solve();
            return ResponseEntity.ok(solution);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

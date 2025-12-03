package com.example.scheduler.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import java.time.LocalDateTime;

@PlanningEntity
public class Shift {
    @PlanningId
    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String requiredRole;

    @PlanningVariable
    private Employee employee;

    public Shift() {
    }

    public Shift(String id, LocalDateTime startTime, LocalDateTime endTime, String requiredRole) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredRole = requiredRole;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @Override
    public String toString() {
        return "Shift{" +
                "id='" + id + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", requiredRole='" + requiredRole + '\'' +
                ", employee=" + employee +
                '}';
    }
}

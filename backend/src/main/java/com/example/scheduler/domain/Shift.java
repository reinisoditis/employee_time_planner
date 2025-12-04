package com.example.scheduler.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PlanningEntity
public class Shift {
    @PlanningId
    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String requiredRole;

    @PlanningVariable
    private Employee employee;

    public Shift(String id, LocalDateTime startTime, LocalDateTime endTime, String requiredRole) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredRole = requiredRole;
    }
}

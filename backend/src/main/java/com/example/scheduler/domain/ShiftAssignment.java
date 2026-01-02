package com.example.scheduler.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftAssignment {
    @PlanningId
    private String id;

    private Shift shift;

    @PlanningVariable
    private Employee employee;

    public LocalDate getDate() {
        return shift != null ? shift.getStartTime().toLocalDate() : null;
    }

    public String getShiftType() {
        return shift != null ? shift.getShiftType() : null;
    }
}

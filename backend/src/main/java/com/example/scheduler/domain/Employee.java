package com.example.scheduler.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @PlanningId
    private String id;
    private String name;

    // Unavailable dates for the employee
    private Set<LocalDate> unavailableDates;

    // Shift type preferences (1.0 = high preference, 0.0 = low preference)
    // Key: shift type (MORNING, DAY, EVENING)
    private Map<String, Double> shiftTypePreferences;

    // Whether employee wants at least one weekend day off
    private boolean wantsWeekendDayOff;

    // Convenience constructor for basic employee creation
    public Employee(String id, String name) {
        this.id = id;
        this.name = name;
        this.unavailableDates = Set.of();
        this.shiftTypePreferences = Map.of();
        this.wantsWeekendDayOff = false;
    }
}

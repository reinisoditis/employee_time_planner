package com.example.scheduler.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Employee> employees;

    @ProblemFactCollectionProperty
    private List<Shift> shifts;

    @PlanningEntityCollectionProperty
    private List<ShiftAssignment> shiftAssignments;

    @PlanningScore
    private HardSoftScore score;

    public Schedule(List<Employee> employees, List<Shift> shifts, List<ShiftAssignment> shiftAssignments) {
        this.employees = employees;
        this.shifts = shifts;
        this.shiftAssignments = shiftAssignments;
    }
}

package com.example.scheduler.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Shift {
    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String shiftType;
    private int requiredEmployees;

    public LocalDate getDate() {
        return startTime.toLocalDate();
    }
}

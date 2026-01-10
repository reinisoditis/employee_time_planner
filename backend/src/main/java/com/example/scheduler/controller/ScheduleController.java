package com.example.scheduler.controller;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.example.scheduler.domain.Employee;
import com.example.scheduler.domain.Schedule;
import com.example.scheduler.domain.Shift;
import com.example.scheduler.domain.ShiftAssignment;
import com.example.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ai.timefold.solver.core.api.solver.SolverJob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SolverManager<Schedule, String> solverManager;
    private final SolutionManager<Schedule, HardSoftScore> solutionManager;

    private final ConcurrentMap<String, Job> jobIdToJob = new ConcurrentHashMap<>();


    @GetMapping
    public Collection<String> list() {
        return jobIdToJob.keySet();
    }

    @PostMapping
    public String solve(@RequestBody Schedule problem){
        String jobId = UUID.randomUUID().toString();
        jobIdToJob.put(jobId, Job.ofSchedule(problem));
            solverManager.solveBuilder()
                    .withProblemId(jobId)
                    .withProblemFinder(jobId_ -> jobIdToJob.get(jobId).schedule)
                    .withBestSolutionConsumer(solution -> jobIdToJob.put(jobId, Job.ofSchedule(solution)))
                    .withExceptionHandler((jobId_, exception) -> {
                        jobIdToJob.put(jobId, Job.ofException(exception));
                        log.error("Failed solving jobId ({}).", jobId, exception);
                    })
                    .run();
        return jobId;
    }

    @GetMapping(value = "/{jobId}")
    public Schedule getSchedule(
            @PathVariable("jobId") String jobId){
        Schedule schedule = getScheduleAndCheckForExceptions(jobId);
        SolverStatus solverStatus = solverManager.getSolverStatus(jobId);
        schedule.setSolverStatus(solverStatus);
        return schedule;
    }

    @GetMapping(value = "/score/{jobId}")
    public ScoreAnalysis<HardSoftScore> analyze(
            @PathVariable("jobId") String jobId)
    {
        Schedule schedule = getScheduleAndCheckForExceptions(jobId);
        return solutionManager.analyze(schedule);
    }

    @GetMapping(value = "/indictments/{jobId}")
    public List<SimpleIndictmentObject> indictments(
            @PathVariable("jobId") String jobId
    ){
        Schedule schedule = getScheduleAndCheckForExceptions(jobId);
        return solutionManager.explain(schedule).getIndictmentMap().entrySet().stream()
                .map(entry -> {
                    Indictment<HardSoftScore> indictment = entry.getValue();
                    return
                            new SimpleIndictmentObject(entry.getKey(),
                                    indictment.getScore(),
                                    indictment.getConstraintMatchCount(),
                                    indictment.getConstraintMatchSet());
                }).collect(Collectors.toList());
    }

    private Schedule getScheduleAndCheckForExceptions(String jobId) {
        Job job = jobIdToJob.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(NOT_FOUND, "No job found for id " + jobId);
        }
        if (job.exception != null) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Job failed", job.exception);
        }
        return job.schedule;
    }

    private record Job(Schedule schedule, Throwable exception, SolverJob<Schedule, UUID> solverJob) {
        static Job ofSchedule(Schedule schedule) {
            return new Job(schedule, null, null);
        }

        static Job ofException(Throwable error) {
            return new Job(null, error, null);
        }

        static Job ofScheduleAndJob(Schedule schedule, SolverJob<Schedule, UUID> solverJob) {
            return new Job(schedule, null, solverJob);
        }
    }

    @Getter @Setter
    public class SimpleIndictmentObject {
        private String indictedObjectID;
        private String indictedObjectClass;
        private HardSoftScore score;
        private int matchCount;
        private List<SimpleConstraintMatch> constraintMatches = new ArrayList<>();

    public SimpleIndictmentObject(Object indictedObject, HardSoftScore score, int matchCount, Set<ConstraintMatch<HardSoftScore>> constraintMatches) {
        this.indictedObjectID = indictedObject instanceof ShiftAssignment ? ((ShiftAssignment) indictedObject).getId() :
                indictedObject instanceof Shift ? ((Shift) indictedObject).toString() :
                        indictedObject instanceof Employee ? ((Employee) indictedObject).getName() :
                                "0";
        this.indictedObjectClass = indictedObject instanceof ShiftAssignment ? "ShiftAssignment" :
                indictedObject instanceof Shift ? "Shift" :
                        indictedObject instanceof Employee ? "Employee" :
                                "Object";
        this.score = score;
        this.matchCount = matchCount;
        this.constraintMatches = constraintMatches.stream().map(constraintMatch -> {
            return new SimpleConstraintMatch(constraintMatch);
        }).collect(Collectors.toList());
    }
}
    @Getter @Setter
    public class SimpleConstraintMatch {
        private String constraintName;
        private HardSoftScore score;

        public SimpleConstraintMatch(ConstraintMatch<HardSoftScore> constraintMatch) {
            this.constraintName = constraintMatch.getConstraintRef().constraintName();
            this.score = constraintMatch.getScore();
        }
    }
}

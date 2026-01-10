import { useEffect, useState } from "react";
import { BACKEND_API_ENDPOINTS } from '../config/api';
import { Link } from "react-router-dom";

type Constraint = { name: string; score: string };
type Analysis = { score: string; constraints: Constraint[] };

type ConstraintMatch = { constraintName: string; score: string };
type Indictment = { indictedObjectID: string; score: string; matchCount: number; constraintMatches: ConstraintMatch[] };

type Employee = {
    id: string;
    name: string;
    unavailableDates: string[];
    shiftTypePreferences: Record<string, number>;
    wantsWeekendDayOff: boolean;
};

type Shift = {
    id: string;
    startTime: string;
    endTime: string;
    shiftType: string;
    requiredEmployees: number;
    date: string;
};

type ShiftAssignment = {
    id: string;
    shift: Shift;
    employee: Employee;
    date: string;
    shiftType: string;
};

type Solution = { solverStatus?: string; employees: Employee[]; shifts: Shift[]; shiftAssignments: ShiftAssignment[]; score?: string };

const badgeStyles = {
    success: { background: "#198754", color: "#fff", padding: "4px 8px", borderRadius: 6 },
    danger: { background: "#dc3545", color: "#fff", padding: "4px 8px", borderRadius: 6 },
    warning: { background: "#ffc107", color: "#000", padding: "4px 8px", borderRadius: 6 },
};

function getHardScore(score: string): number {
    const idx = score.indexOf("hard");
    if (idx === -1) return 0;
    const substr = score.slice(0, idx);
    const n = Number(substr);
    return Number.isNaN(n) ? 0 : n;
}

function getSoftScore(score: string): number {
    const start = score.indexOf("hard/");
    const end = score.indexOf("soft");
    if (start === -1 || end === -1) return 0;
    const substr = score.slice(start + "hard/".length, end);
    const n = Number(substr);
    return Number.isNaN(n) ? 0 : n;
}

function getScorePopoverContent(constraint_list: Constraint[]) {
    let popover_content = "";
    constraint_list.forEach((constraint) => {
        if (getHardScore(constraint.score) === 0) {
            popover_content += `${constraint.name} : ${constraint.score}<br>`;
        } else {
            popover_content += `<b>${constraint.name} : ${constraint.score}</b><br>`;
        }
    });
    return popover_content;
}

function getEntityPopoverContent(entityId: string, indictmentMap: Record<string, Indictment | undefined>) {
    let popover_content = "";
    const indictment = indictmentMap[entityId];
    if (indictment != null) {
        popover_content += `Total score: <b>${indictment.score}</b> (${indictment.matchCount})<br>`;
        indictment.constraintMatches.forEach((match) => {
            if (getHardScore(match.score) === 0) {
                popover_content += `${match.constraintName} : ${match.score}<br>`;
            } else {
                popover_content += `<b>${match.constraintName} : ${match.score}</b><br>`;
            }
        });
    }
    return popover_content;
}

function JobDetails({ jobId }: { jobId?: string }) {
    const [analysis, setAnalysis] = useState<Analysis | null>(null);
    const [solution, setSolution] = useState<Solution | null>(null);
    const [indictments, setIndictments] = useState<Indictment[] | null>(null);
    const [activePopover, setActivePopover] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!jobId) return;

        let isMounted = true;
        let intervalId: number | null = null;

        async function fetchData() {
            if (!isMounted || !jobId) return;
            
            try {
                setError(null);
                const [analysisRes, solutionRes, indictmentsRes] = await Promise.all([
                    fetch(BACKEND_API_ENDPOINTS.jobScore(jobId)),
                    fetch(BACKEND_API_ENDPOINTS.job(jobId)),
                    fetch(BACKEND_API_ENDPOINTS.jobIndictments(jobId)),
                ]);

                async function safeParse(res: Response) {
                    if (!res) return null;
                    if (!res.ok) return null;
                    const ct = res.headers.get("content-type") || "";
                    if (ct.includes("application/json")) {
                        try {
                            return await res.json();
                        } catch (e) {
                            console.error("Failed to parse JSON response", e);
                            return null;
                        }
                    }
                    try {
                        const text = await res.text();
                        console.error("Expected JSON but got:", text);
                    } catch (e) {
                        console.error("Failed to read non-JSON response", e);
                    }
                    return null;
                }

                const [analysisData, solutionData, indictmentsData] = await Promise.all([
                    safeParse(analysisRes),
                    safeParse(solutionRes),
                    safeParse(indictmentsRes),
                ]);

                if (!isMounted) return;

                setAnalysis(analysisData ?? { score: "0hard/0soft", constraints: [] });
                setSolution(solutionData ?? { employees: [], shifts: [], shiftAssignments: [], score: undefined });
                setIndictments(indictmentsData ?? []);

                if (solutionData?.solverStatus === "SOLVING_ACTIVE") {
                    if (!intervalId) {
                        intervalId = window.setInterval(() => fetchData(), 250);
                    }
                } else {
                    if (intervalId) {
                        clearInterval(intervalId);
                        intervalId = null;
                    }
                }
            } catch (e: any) {
                console.error("Fetch error", e);
                if (isMounted) {
                    setError(e.message || "Failed to load data");
                    setAnalysis({ score: "0hard/0soft", constraints: [] });
                    setSolution({ employees: [], shifts: [], shiftAssignments: [], score: undefined });
                    setIndictments([]);
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        }

        setLoading(true);
        fetchData();

        return () => {
            isMounted = false;
            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [jobId]);

    const indictmentMap: Record<string, Indictment | undefined> = {};
    function badgeFromIndict(ind?: Indictment) {
        if (!ind) return badgeStyles.success;
        if (getHardScore(ind.score) !== 0) return badgeStyles.danger;
        if (getSoftScore(ind.score) !== 0) return badgeStyles.warning;
        return badgeStyles.success;
    }
    console.log("Rendering JobDetails", { analysis, solution, indictments });
    if (indictments) {
        indictments.forEach((ind) => (indictmentMap[ind.indictedObjectID] = ind));
    }

    if (loading) return <div>Loading...</div>;
    if (error) return <div style={{ padding: 16, color: "#dc3545" }}>Error: {error}</div>;

    const isSolving = solution?.solverStatus === "SOLVING_ACTIVE";
    const assignmentCount = solution?.shiftAssignments?.length || 0;
    const employeeCount = solution?.employees?.length || 0;

    return (
        <>
            <div className="p-4 items-center gap-3">
                <h1 className="text-3xl font-bold">Schedule details</h1>
                <Link to="/" className="text-blue-600 hover:underline">Home</Link>
            </div>
        <div className="p-4">
            {isSolving && (
                <div className="p-3 mb-4 rounded-md font-semibold w-1/5" style={{ color: "#ffc107" }}>
                    Solver is running...
                </div>
            )}

            {!isSolving && (
                <div className="p-3 mb-4 rounded-md font-semibold w-1/5" style={{ color: "#198754" }}>
                    Solver has finished running.
                </div>    
            )}
            
            <div className="mb-3">
                <span id="score_a" style={{ marginRight: 12, cursor: "pointer" }} onClick={() => setActivePopover(activePopover === "score" ? null : "score")}>
                    <span style={getHardScore(analysis?.score ?? "0hard/0soft") === 0 ? badgeStyles.success : badgeStyles.danger}>Score Breakdown</span>
                </span>
                <span id="score_text" style={{ fontWeight: 600 }}>{analysis?.score}</span>
                {activePopover === "score" && analysis && (
                    <div style={{ border: "1px solid #ddd", padding: 8, marginTop: 8, background: "#fff", maxWidth: 520 }} dangerouslySetInnerHTML={{ __html: getScorePopoverContent(analysis.constraints) }} />
                )}
            </div>

            <div style={{ marginBottom: 16, color: "#666" }}>
                {employeeCount} employees · {assignmentCount} shift assignments
            </div>

            <div id="employees_container">
                <h3>Employees</h3>
                <div className="flex flex-wrap mt-2 gap-4">
                    {solution?.employees.map((emp) => {
                        const key = `emp-${emp.id}`;
                        const empIndict = indictmentMap[emp.name] || indictmentMap[emp.id];
                        const empBadgeStyle = badgeFromIndict(empIndict);
                        return (
                            <div key={key}>
                                <a style={{ marginRight: 8, textDecoration: "none" }} onClick={() => setActivePopover(activePopover === key ? null : key)}>
                                    <span style={empBadgeStyle}>{emp.name}</span>
                                </a>
                                {activePopover === key && (
                                    <div style={{ border: "1px solid #ddd", padding: 8, marginTop: 6, background: "#fff", maxWidth: 520 }}>
                                        <div>ID: {emp.id}</div>
                                        <div>Wants weekend day off: {emp.wantsWeekendDayOff ? "Yes" : "No"}</div>
                                        <div>Shift type preferences: {Object.entries(emp.shiftTypePreferences).map(([stype, score]) => `${stype} (${score})`).join(", ") || "none"}</div>
                                        <div>Unavailable: {emp.unavailableDates.join(", ") || "none"}</div>
                                        <hr />
                                        <div dangerouslySetInnerHTML={{ __html: getEntityPopoverContent(emp.name, indictmentMap) }} />
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            <div id="assignments_container">
                <h3>Shift Assignments</h3>
                <div className="mt-2 grid grid-cols-6 gap-4">
                    {solution?.shiftAssignments.map((assign) => {
                        const aKey = `assign-${assign.id}`;
                        const aIndict = indictmentMap[assign.id] || indictmentMap[assign.id.toString()];
                        const aBadgeStyle = badgeFromIndict(aIndict);
                        const employeeName = assign.employee?.name || "Unassigned";
                        const employeeId = assign.employee?.id || "N/A";
                        return (
                            <div key={assign.id} style={{ marginBottom: 8 }}>
                                <a style={{ marginRight: 8, textDecoration: "none" }} onClick={() => setActivePopover(activePopover === aKey ? null : aKey)}>
                                    <span style={aBadgeStyle}>{assign.shift.date} {assign.shift.shiftType} — {employeeName}</span>
                                </a>
                                {activePopover === aKey && (
                                    <div style={{ border: "1px solid #ddd", padding: 8, marginTop: 6, background: "#fff", maxWidth: 520 }}>
                                        <div>Shift: {assign.shift.id} ({assign.shift.startTime} — {assign.shift.endTime}) Needed employes: {assign.shift.requiredEmployees}</div>
                                        <div>Employee: {employeeName} ({employeeId})</div>
                                        <div>Shift type: {assign.shiftType}</div>
                                        <hr />
                                        <div dangerouslySetInnerHTML={{ __html: getEntityPopoverContent(assign.id, indictmentMap) }} />
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
        </>
    );
};

export default JobDetails;
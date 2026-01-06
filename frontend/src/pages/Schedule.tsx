import { useMemo, useState, useEffect } from "react";

type Employee = {
    id: string;
    name: string;
    // map ISO date (YYYY-MM-DD) => shift description (empty/null => off)
    schedule?: Record<string, string | null>;
};

type ShiftAssignment = {
    id?: string;
    // expected shape from backend: employee may be an object with id
    employee?: {
        id: string;
    };
    // ISO date string (YYYY-MM-DD)
    date: string;
    // shift object from backend
    shift?: {
        id: string;
        startTime: string;
        endTime: string;
        shiftType: string;
        requiredEmployees?: number;
        date?: string;
    } | null;
};

const todayISO = (d = new Date()) => d.toISOString().slice(0, 10);

const addDays = (d: Date, days: number) => {
    const n = new Date(d);
    n.setDate(n.getDate() + days);
    return n;
};

const startOfWeekMonday = (d: Date) => {
    const n = new Date(d);
    const day = n.getDay(); // 0 (Sun) - 6 (Sat)
    const diff = (day + 6) % 7; // days since Monday
    n.setDate(n.getDate() - diff);
    n.setHours(0, 0, 0, 0);
    return n;
};

const getWeekDates = (start: Date, days = 7) =>
    Array.from({ length: days }).map((_, i) => addDays(start, i));

const formatShort = (d: Date) =>
    d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });

// Three color palette for shift types
const SHIFT_COLORS = [
    { bg: "#d4edda", text: "#155724" }, // Green
    { bg: "#cce5ff", text: "#004085" }, // Blue
    { bg: "#fff3cd", text: "#856404" }, // Yellow
];

const sampleEmployees: Employee[] = [
    {
        id: "e1",
        name: "Alice Novak",
        schedule: {
            [todayISO()]: "09:00–17:00",
            [todayISO(addDays(new Date(), 1))]: "09:00–17:00",
            [todayISO(addDays(new Date(), 3))]: "12:00–20:00",
        },
    },
    {
        id: "e2",
        name: "Benis Ozols",
        schedule: {
            [todayISO()]: null,
            [todayISO(addDays(new Date(), 2))]: "08:00–16:00",
            [todayISO(addDays(new Date(), 4))]: "10:00–18:00",
        },
    },
    {
        id: "e3",
        name: "Dace Liepa",
        schedule: {
            [todayISO(addDays(new Date(), 1))]: "07:00–15:00",
            [todayISO(addDays(new Date(), 2))]: "07:00–15:00",
            [todayISO(addDays(new Date(), 3))]: "07:00–15:00",
            [todayISO(addDays(new Date(), 5))]: "14:00–22:00",
        },
    },
];

export default function Schedule({
    initialStartDate,
    solutionId,
}: {
    // start date for the grid (defaults to today)
    initialStartDate?: Date;
    solutionId?: string;
}) {
    const [startDate, setStartDate] = useState<Date>(initialStartDate ? new Date(initialStartDate) : new Date());
    const [employees, setEmployees] = useState<Employee[]>(sampleEmployees);
    const [shiftAssignments, setShiftAssignments] = useState<ShiftAssignment[]>([]);
    const [loading, setLoading] = useState<boolean>(false);

    // normalize to start of day
    startDate.setHours(0, 0, 0, 0);

    useEffect(() => {
        if (!solutionId) return;

        (async () => {
            setLoading(true);
            try {
                // Use backend service name instead of localhost
                const res = await fetch(`http://backend:8080/api/${solutionId}`);
                console.log(res);
                
                if (!res.ok) throw new Error(`status ${res.status}`);
                const data = await res.json();

                console.log(data);
                
                // expect data.employees and data.shiftAssignments
                if (Array.isArray(data.employees)) setEmployees(data.employees);
                if (Array.isArray(data.shiftAssignments)) setShiftAssignments(data.shiftAssignments);
                // determine earliest date from returned values and align to Monday
                try {
                    let firstDateStr: string | null = null;
                    if (Array.isArray(data.shiftAssignments) && data.shiftAssignments.length > 0) {
                        const dates = data.shiftAssignments
                            .map((a: any) => a?.date)
                            .filter(Boolean)
                            .sort();
                        if (dates.length) firstDateStr = dates[0];
                    }
                    if (!firstDateStr && Array.isArray(data.shifts) && data.shifts.length > 0) {
                        const dates = data.shifts
                            .map((s: any) => s?.date)
                            .filter(Boolean)
                            .sort();
                        if (dates.length) firstDateStr = dates[0];
                    }
                    if (firstDateStr) {
                        const d = new Date(firstDateStr + "T00:00:00");
                        if (!isNaN(d.getTime())) {
                            setStartDate(startOfWeekMonday(d));
                        }
                    }
                } catch (e) {
                    // ignore date parsing errors
                }
            } catch (e) {
                console.error("Failed to fetch schedule", e);
                // keep sampleEmployees as fallback
            } finally {
                setLoading(false);
            }
        })();
    }, [solutionId]);

    const weekDates = useMemo(() => getWeekDates(startDate, 7), [startDate]);

    const prevWeek = () => setStartDate((d) => addDays(new Date(d), -7));
    const nextWeek = () => setStartDate((d) => addDays(new Date(d), 7));

    // build lookup: assignmentsByEmployee[empId][dateISO] = assignment
    const assignmentsByEmployee: Record<string, Record<string, ShiftAssignment>> = {};
    shiftAssignments.forEach((a: any) => {
        const empId = a.employee?.id;
        const date = a.date;
        if (!empId || !date) return;
        assignmentsByEmployee[empId] = assignmentsByEmployee[empId] || {};
        assignmentsByEmployee[empId][date] = a;
    });

    // Collect unique shift types and assign colors
    const uniqueShiftTypes = useMemo(() => {
        const types = new Set<string>();
        shiftAssignments.forEach((a) => {
            if (a.shift?.shiftType) types.add(a.shift.shiftType);
        });
        return Array.from(types).sort();
    }, [shiftAssignments]);

    const shiftTypeColors: Record<string, { bg: string; text: string }> = {};
    uniqueShiftTypes.forEach((type, idx) => {
        shiftTypeColors[type] = SHIFT_COLORS[idx % SHIFT_COLORS.length];
    });

    if (loading) return <div className="p-3">Loading schedule...</div>;

    return (
        <div className="p-3 font-sans">
            <div className="flex gap-2 mb-3 items-center">
                <button className="px-2.5 py-1.5 cursor-pointer" onClick={prevWeek}>
                    ← Prev
                </button>
                <button className="px-2.5 py-1.5 cursor-pointer" onClick={nextWeek}>
                    Next →
                </button>

                <label className="ml-auto flex items-center gap-2">
                    Start date:
                    <input
                        className="p-1.5"
                        type="date"
                        value={startDate.toISOString().slice(0, 10)}
                        onChange={(e) => {
                            const d = new Date(e.target.value + "T00:00:00");
                            if (!isNaN(d.getTime())) setStartDate(startOfWeekMonday(d));
                        }}
                    />
                </label>
            </div>

            {uniqueShiftTypes.length > 0 && (
                <div className="flex gap-3 mb-3 items-center">
                    <span className="font-semibold text-sm">Shift Types:</span>
                    {uniqueShiftTypes.map((type) => {
                        const colors = shiftTypeColors[type];
                        return (
                            <div
                                key={type}
                                className="flex items-center gap-1.5 px-2 py-1 rounded-md text-xs font-medium"
                                style={{
                                    background: colors.bg,
                                    color: colors.text,
                                }}
                            >
                                {type}
                            </div>
                        );
                    })}
                </div>
            )}

            <table className="border-collapse w-full">
                <thead>
                    <tr>
                        <th className="border border-gray-300 bg-gray-50 px-2.5 py-2 text-left whitespace-nowrap w-[200px]">
                            Employee
                        </th>
                        {weekDates.map((d) => (
                            <th key={d.toISOString()} className="border border-gray-300 bg-gray-50 px-2.5 py-2 text-left whitespace-nowrap">
                                <div>{formatShort(d)}</div>
                                <div className="text-xs text-gray-600">{d.toISOString().slice(0, 10)}</div>
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {employees.map((emp) => (
                        <tr key={emp.id}>
                            <td className="border border-gray-200 px-2.5 py-2 min-w-[120px] text-left font-semibold bg-white">
                                {emp.name}
                            </td>
                            {weekDates.map((d) => {
                                const key = d.toISOString().slice(0, 10);
                                const assign: ShiftAssignment | undefined = assignmentsByEmployee[emp.id]?.[key];
                                
                                if (assign && assign.shift) {
                                    const s = assign.shift.startTime?.slice(11, 16) ?? "";
                                    const e = assign.shift.endTime?.slice(11, 16) ?? "";
                                    const shiftType = assign.shift.shiftType;
                                    const colors = shiftTypeColors[shiftType] || { bg: "#e6f7ff", text: "#003a8c" };
                                    
                                    return (
                                        <td 
                                            key={key} 
                                            className="border border-gray-200 px-2.5 py-2 min-w-[120px] text-center font-medium"
                                            style={{
                                                background: colors.bg,
                                                color: colors.text,
                                            }}
                                        >
                                            <div>{s}{e ? `–${e}` : ""}</div>
                                        </td>
                                    );
                                }
                                
                                return (
                                    <td key={key} className="border border-gray-200 px-2.5 py-2 min-w-[120px] text-center">
                                        <span className="text-gray-500">Off</span>
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
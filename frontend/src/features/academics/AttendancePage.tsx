import { useCallback, useEffect, useState } from "react";
import { attendanceApi, Session } from "./api";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import { localDateString } from "@/lib/date";
const statuses = ["PRESENT", "ABSENT"];
const today = localDateString();
export function AttendancePage() {
  const [from, setFrom] = useState(today),
    [to, setTo] = useState(today),
    [rows, setRows] = useState<Session[]>([]),
    [selected, setSelected] = useState<Session>(),
    [marks, setMarks] = useState<Record<number, string>>({}),
    [error, setError] = useState("");
  const load = useCallback(
    () =>
      attendanceApi
        .sessions(from, to)
        .then((x) => {
          setRows(x);
          setSelected((current) =>
            current ? x.find((session) => session.id === current.id) : current,
          );
        })
        .catch((e) => setError(handleApiError(e).message)),
    [from, to],
  );
  useEffect(() => {
    void load();
  }, [load]);
  async function submit() {
    if (!selected) return;
    try {
      await attendanceApi.submit(
        selected.id,
        selected.attendance.map((a) => ({
          studentId: a.studentId,
          status: marks[a.studentId] || a.status || "PRESENT",
        })),
      );
      await load();
    } catch (e) {
      setError(handleApiError(e).message);
    }
  }
  return (
    <div className="page-container space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Manual attendance</h1>
        <p className="text-sm text-slate-500">
          Lecture and daily attendance. No RFID or biometric device flow.
        </p>
      </div>
      <Card>
        <div className="flex flex-wrap gap-4">
          <Input label="From" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          <Input label="To" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </Card>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div className="grid gap-5 lg:grid-cols-[340px_1fr]">
        <Card>
          <div className="space-y-2">
            {rows.map((s) => (
              <button
                key={s.id}
                onClick={() => {
                  setSelected(s);
                  setMarks(
                    Object.fromEntries(
                      s.attendance.map((a) => [a.studentId, a.status || "PRESENT"]),
                    ),
                  );
                }}
                className={`w-full rounded-xl border p-3 text-left ${selected?.id === s.id ? "border-indigo-500 bg-indigo-50" : ""}`}
              >
                <b>{s.subject}</b>
                <div className="text-sm">
                  {s.className} · {s.section}
                </div>
                <div className="text-xs text-slate-500">
                  {s.date} · {s.status}
                </div>
              </button>
            ))}
          </div>
        </Card>
        <Card>
          {selected ? (
            <>
              <div className="mb-4">
                <h2 className="font-semibold">
                  {selected.subject} — {selected.className} {selected.section}
                </h2>
                <p className="text-sm text-slate-500">
                  Assigned to {selected.teacher}; locks {new Date(selected.lockAt).toLocaleString()}
                </p>
              </div>
              <div className="space-y-2">
                {selected.attendance.map((a) => (
                  <div
                    key={a.studentId}
                    className="grid items-center gap-3 rounded-lg border p-3 md:grid-cols-[1fr_180px]"
                  >
                    <div>
                      <b>{a.studentName}</b>
                      <div className="text-xs text-slate-500">{a.admissionNumber}</div>
                    </div>
                    <Select
                      disabled={selected.status !== "OPEN"}
                      value={marks[a.studentId] || a.status || "PRESENT"}
                      onChange={(e) => setMarks({ ...marks, [a.studentId]: e.target.value })}
                      options={statuses.map((x) => ({ label: x.replace("_", " "), value: x }))}
                    />
                  </div>
                ))}
              </div>
              {selected.status === "OPEN" && (
                <Button className="mt-4" onClick={submit}>
                  Submit attendance
                </Button>
              )}
            </>
          ) : (
            <p className="text-sm text-slate-500">Select a session to mark attendance.</p>
          )}
        </Card>
      </div>
    </div>
  );
}

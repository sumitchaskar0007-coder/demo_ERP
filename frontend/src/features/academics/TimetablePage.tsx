import { FormEvent, useEffect, useState } from "react";
import { academicApi, timetableApi, Master, Timetable } from "./api";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
export function TimetablePage() {
  const [tables, setTables] = useState<Timetable[]>([]);
  const [master, setMaster] = useState<Record<string, Master[]>>({});
  const [teachers, setTeachers] = useState<Master[]>([]);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    academicYearId: "",
    academicTermId: "",
    classId: "",
    sectionId: "",
    weekStart: "",
  });
  const [entry, setEntry] = useState({
    dayOfWeek: "MONDAY",
    periodId: "",
    subjectId: "",
    teacherId: "",
    roomId: "",
    type: "LECTURE",
  });
  const load = () =>
    timetableApi
      .list()
      .then(setTables)
      .catch((e) => setError(handleApiError(e).message));
  useEffect(() => {
    load();
    Promise.all(
      ["ACADEMIC_YEAR", "TERM", "CLASS", "SECTION", "SUBJECT", "PERIOD", "ROOM"].map(
        async (t) => [t, await academicApi.masters(t)] as const,
      ),
    ).then((x) => setMaster(Object.fromEntries(x)));
    academicApi.people().then(setTeachers);
  }, []);
  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await timetableApi.create(
        Object.fromEntries(
          Object.entries(form).map(([k, v]) => [k, k === "weekStart" ? v : Number(v)]),
        ),
      );
      load();
    } catch (x) {
      setError(handleApiError(x).message);
    }
  }
  const opts = (t: string) =>
    (master[t] || []).map((x) => ({ label: `${x.name} (#${x.id})`, value: String(x.id) }));
  async function add(id: number) {
    try {
      await timetableApi.add(id, {
        ...entry,
        periodId: Number(entry.periodId),
        subjectId: entry.type === "BREAK" ? null : Number(entry.subjectId),
        teacherId: entry.type === "BREAK" ? null : Number(entry.teacherId),
        roomId: entry.type === "BREAK" ? null : Number(entry.roomId),
      });
      load();
    } catch (e) {
      setError(handleApiError(e).message);
    }
  }
  return (
    <div className="page-container space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Timetable assignment</h1>
        <p className="text-sm text-slate-500">
          Draft, validate, publish, and generate attendance sessions.
        </p>
      </div>
      <Card>
        <form onSubmit={create} className="grid gap-4 md:grid-cols-5">
          <Select
            label="Academic year"
            required
            value={form.academicYearId}
            onChange={(e) => setForm({ ...form, academicYearId: e.target.value })}
            options={opts("ACADEMIC_YEAR")}
          />
          <Select
            label="Term"
            required
            value={form.academicTermId}
            onChange={(e) => setForm({ ...form, academicTermId: e.target.value })}
            options={opts("TERM")}
          />
          <Select
            label="Class"
            required
            value={form.classId}
            onChange={(e) => setForm({ ...form, classId: e.target.value })}
            options={opts("CLASS")}
          />
          <Select
            label="Section"
            required
            value={form.sectionId}
            onChange={(e) => setForm({ ...form, sectionId: e.target.value })}
            options={opts("SECTION")}
          />
          <Input
            label="Week starting"
            type="date"
            required
            value={form.weekStart}
            onChange={(e) => setForm({ ...form, weekStart: e.target.value })}
          />
          <Button type="submit">Create draft</Button>
        </form>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </Card>
      <div className="grid gap-4">
        {tables.map((t) => (
          <Card key={t.id}>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-semibold">
                  {t.className} · {t.section}
                </h2>
                <p className="text-sm text-slate-500">
                  {t.term} · week {t.weekStart} · {t.status}
                </p>
              </div>
              {t.status === "DRAFT" && (
                <Button
                  onClick={async () => {
                    try {
                      await timetableApi.publish(t.id);
                      load();
                    } catch (e) {
                      setError(handleApiError(e).message);
                    }
                  }}
                >
                  Publish
                </Button>
              )}
            </div>
            {t.status === "DRAFT" && (
              <div className="mt-4 grid gap-2 rounded-xl bg-slate-50 p-3 md:grid-cols-7">
                <Select
                  value={entry.dayOfWeek}
                  onChange={(e) => setEntry({ ...entry, dayOfWeek: e.target.value })}
                  options={["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"].map(
                    (x) => ({ label: x, value: x }),
                  )}
                />
                <Select
                  value={entry.type}
                  onChange={(e) => setEntry({ ...entry, type: e.target.value })}
                  options={["LECTURE", "PRACTICAL", "LAB", "BREAK"].map((x) => ({
                    label: x,
                    value: x,
                  }))}
                />
                <Select
                  value={entry.periodId}
                  onChange={(e) => setEntry({ ...entry, periodId: e.target.value })}
                  options={opts("PERIOD")}
                />
                <Select
                  disabled={entry.type === "BREAK"}
                  value={entry.subjectId}
                  onChange={(e) => setEntry({ ...entry, subjectId: e.target.value })}
                  options={opts("SUBJECT")}
                />
                <Select
                  disabled={entry.type === "BREAK"}
                  value={entry.teacherId}
                  onChange={(e) => setEntry({ ...entry, teacherId: e.target.value })}
                  options={teachers.map((x) => ({ label: x.name, value: String(x.id) }))}
                />
                <Select
                  disabled={entry.type === "BREAK"}
                  value={entry.roomId}
                  onChange={(e) => setEntry({ ...entry, roomId: e.target.value })}
                  options={opts("ROOM")}
                />
                <Button onClick={() => add(t.id)}>Add period</Button>
              </div>
            )}
            <div className="mt-4 grid gap-2 md:grid-cols-5">
              {t.entries.map((e) => (
                <div key={e.id} className="rounded-xl border p-3 text-sm">
                  <b>
                    {e.dayOfWeek} P{e.periodNumber}
                  </b>
                  <div>
                    {e.startTime}–{e.endTime}
                  </div>
                  <div>{e.subject || e.type}</div>
                  <div className="text-slate-500">
                    {e.teacher}
                    {e.room ? ` · ${e.room}` : ""}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

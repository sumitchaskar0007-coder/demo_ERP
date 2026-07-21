import { useEffect, useState } from "react";
import { Modal } from "@/components/common/Modal";
import { Button } from "@/components/common/Button";
import { Select } from "@/components/common/Select";
import { Input } from "@/components/common/Input";
import { timetableApi, classTeacherApi } from "./api";
import { LECTURE_TYPES } from "./constants";
import type { TimetableEntry, SubjectInfo, TeacherInfo, RoomInfo } from "./types";

interface Props {
  open: boolean;
  onClose: () => void;
  timetableId: number;
  dayOfWeek: string;
  periodNumber: number;
  entry?: TimetableEntry | null;
  onSaved: () => void;
}

export function PeriodPopup({
  open,
  onClose,
  timetableId,
  dayOfWeek,
  periodNumber,
  entry,
  onSaved,
}: Props) {
  const [subjects, setSubjects] = useState<SubjectInfo[]>([]);
  const [teachers, setTeachers] = useState<TeacherInfo[]>([]);
  const [rooms, setRooms] = useState<RoomInfo[]>([]);
  const [saving, setSaving] = useState(false);

  const [type, setType] = useState<string>(entry?.type || "THEORY");
  const [subjectId, setSubjectId] = useState(entry?.subjectId ? String(entry.subjectId) : "");
  const [teacherId, setTeacherId] = useState(entry?.teacherId ? String(entry.teacherId) : "");
  const [roomId, setRoomId] = useState(entry?.roomId ? String(entry.roomId) : "");
  const [remarks, setRemarks] = useState(entry?.remarks || "");

  useEffect(() => {
    if (!open) return;
    classTeacherApi
      .subjects()
      .then(setSubjects)
      .catch(() => {});
    timetableApi
      .rooms()
      .then(setRooms)
      .catch(() => {});
  }, [open]);

  useEffect(() => {
    if (!subjectId || !open) {
      setTeachers([]);
      return;
    }
    classTeacherApi
      .teachers(Number(subjectId))
      .then(setTeachers)
      .catch(() => setTeachers([]));
  }, [subjectId, open]);

  useEffect(() => {
    if (!open) return;
    setType(entry?.type || "THEORY");
    setSubjectId(entry?.subjectId ? String(entry.subjectId) : "");
    setTeacherId(entry?.teacherId ? String(entry.teacherId) : "");
    setRoomId(entry?.roomId ? String(entry.roomId) : "");
    setRemarks(entry?.remarks || "");
  }, [entry, open]);

  const isBreak = type === "BREAK";

  const save = async () => {
    setSaving(true);
    try {
      const payload: Record<string, unknown> = {
        dayOfWeek,
        periodNumber,
        type,
        subjectId: isBreak ? null : subjectId ? Number(subjectId) : null,
        teacherId: isBreak ? null : teacherId ? Number(teacherId) : null,
        roomId: isBreak ? null : roomId ? Number(roomId) : null,
        remarks: isBreak ? remarks || "Break" : remarks || null,
      };
      if (entry?.id) {
        await timetableApi.updateEntry(timetableId, entry.id, payload);
      } else {
        await timetableApi.addEntry(timetableId, payload);
      }
      onSaved();
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={entry ? "Edit Entry" : "Add Entry"} size="md">
      <div className="space-y-4">
        <Select
          label="Type"
          value={type}
          onChange={(e) => setType(e.target.value)}
          options={LECTURE_TYPES}
        />

        {!isBreak && (
          <>
            <Select
              label="Subject"
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
              options={[
                { label: "— Select subject —", value: "" },
                ...subjects.map((s) => ({ label: `${s.name} (${s.code})`, value: String(s.id) })),
              ]}
            />
            <Select
              label="Teacher"
              value={teacherId}
              onChange={(e) => setTeacherId(e.target.value)}
              options={[
                { label: "— Select teacher —", value: "" },
                ...teachers.map((t) => ({ label: t.fullName, value: String(t.id) })),
              ]}
            />
            <Select
              label="Room"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              options={[
                { label: "— Select room —", value: "" },
                ...rooms.map((r) => ({ label: `${r.code} — ${r.name}`, value: String(r.id) })),
              ]}
            />
          </>
        )}

        {isBreak && (
          <Input
            label="Break label"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            placeholder="e.g. Lunch, Short Break"
          />
        )}

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={save} loading={saving}>
            {entry ? "Update" : "Save"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

import { useDraggable } from "@dnd-kit/core";
import { GripVertical, Pencil, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { getSubjectColor } from "./constants";
import type { TimetableEntry } from "./types";

interface Props {
  entry: TimetableEntry;
  isDroppable: boolean;
  onEdit: (entry: TimetableEntry) => void;
  onDelete: (entry: TimetableEntry) => void;
}

export function LectureCard({ entry, isDroppable, onEdit, onDelete }: Props) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `entry-${entry.id}`,
    data: { entry },
  });

  const isBreak = entry.type === "BREAK";
  const isLecture = entry.subjectId && entry.subject;

  const colorClass = isBreak
    ? "bg-slate-100 border-slate-300 text-slate-600"
    : isLecture
    ? getSubjectColor(entry.subjectId!)
    : "bg-white border-slate-200 text-slate-700";

  const style = transform
    ? { transform: `translate(${transform.x}px, ${transform.y}px)`, zIndex: 50 }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "group relative flex min-h-[64px] items-start gap-1 rounded-lg border p-1.5 text-left text-xs transition-shadow",
        isDragging && "shadow-lg opacity-90",
        colorClass,
        isDroppable && "ring-2 ring-blue-300",
      )}
    >
      <button
        className="mt-0.5 shrink-0 cursor-grab text-slate-400 opacity-0 transition-opacity hover:text-slate-600 group-hover:opacity-100 active:cursor-grabbing"
        {...listeners}
        {...attributes}
        aria-label="Drag entry"
      >
        <GripVertical className="h-3.5 w-3.5" />
      </button>

      <div className="min-w-0 flex-1">
        {isBreak ? (
          <p className="font-medium">{entry.remarks || entry.type}</p>
        ) : (
          <>
            <p className="truncate font-semibold leading-tight">{entry.subject}</p>
            {entry.teacher && (
              <p className="truncate text-[10px] opacity-75">{entry.teacher}</p>
            )}
            {entry.room && (
              <p className="truncate text-[10px] opacity-60">{entry.room}</p>
            )}
          </>
        )}
      </div>

      <div className="flex shrink-0 gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          onClick={() => onEdit(entry)}
          className="rounded p-0.5 hover:bg-white/60"
          aria-label="Edit"
        >
          <Pencil className="h-3 w-3" />
        </button>
        <button
          onClick={() => onDelete(entry)}
          className="rounded p-0.5 hover:bg-red-100 hover:text-red-600"
          aria-label="Delete"
        >
          <Trash2 className="h-3 w-3" />
        </button>
      </div>
    </div>
  );
}

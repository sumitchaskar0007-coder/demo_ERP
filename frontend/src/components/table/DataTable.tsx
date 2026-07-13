import type { ReactNode } from "react";

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

export function DataTable<T>({ columns, data, rowKey }: {
  columns: Column<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[850px] border-collapse text-left">
        <thead><tr className="border-b bg-[#f8f9fc]">{columns.map((column) => <th key={column.key} className={`px-5 py-4 text-[11px] font-bold uppercase tracking-wider text-slate-500 ${column.className || ""}`}>{column.header}</th>)}</tr></thead>
        <tbody className="divide-y">
          {data.map((row) => (
            <tr key={rowKey(row)} className="transition hover:bg-[#f8f9fc]">
              {columns.map((column) => <td key={column.key} className={`px-5 py-4 text-sm text-slate-700 ${column.className || ""}`}>{column.render(row)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

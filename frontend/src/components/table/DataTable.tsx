import type { ReactNode } from "react";

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

export function DataTable<T>({
  columns,
  data,
  rowKey,
}: {
  columns: Column<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
}) {
  return (
    <div className="data-table w-full min-w-0 overscroll-x-contain overflow-x-auto">
      <table className="w-full min-w-[760px] border-collapse text-left">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} className={column.className || ""}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column) => (
                <td
                  key={column.key}
                  data-label={column.key === "actions" ? "" : column.header}
                  className={column.className || ""}
                >
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

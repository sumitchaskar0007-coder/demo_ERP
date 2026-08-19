import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

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
  tableClassName,
}: {
  columns: Column<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
  tableClassName?: string;
}) {
  return (
    <div className="data-table w-full min-w-0 overscroll-x-contain overflow-x-auto">
      <table className={cn("w-full min-w-max border-collapse text-left", tableClassName)}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                className={cn(
                  column.className,
                  column.key === "actions" && "w-px whitespace-nowrap",
                )}
              >
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
                  className={cn(
                    column.className,
                    column.key === "actions" && "w-px whitespace-nowrap [overflow-wrap:normal]",
                  )}
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

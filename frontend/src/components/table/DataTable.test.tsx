import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "@/components/common/Button";
import { DataTable, type Column } from "./DataTable";

type Row = { id: number; name: string };

describe("DataTable responsive actions", () => {
  it("preserves intrinsic table width and protects the action column from compression", () => {
    const columns: Column<Row>[] = [
      { key: "name", header: "Name", render: (row) => row.name },
      {
        key: "actions",
        header: "",
        render: () => <Button>Review</Button>,
      },
    ];

    const { container } = render(
      <DataTable columns={columns} data={[{ id: 1, name: "Student" }]} rowKey={(row) => row.id} />,
    );

    expect(container.querySelector("table")).toHaveClass("min-w-max");
    expect(screen.getByRole("button", { name: "Review" }).closest("td")).toHaveClass(
      "w-px",
      "whitespace-nowrap",
      "[overflow-wrap:normal]",
    );
  });

  it("allows a page to opt into a bounded fixed desktop layout", () => {
    const columns: Column<Row>[] = [{ key: "name", header: "Name", render: (row) => row.name }];

    const { container } = render(
      <DataTable
        columns={columns}
        data={[{ id: 1, name: "Student" }]}
        rowKey={(row) => row.id}
        tableClassName="min-w-[1080px] table-fixed"
      />,
    );

    expect(container.querySelector("table")).toHaveClass("w-full", "min-w-[1080px]", "table-fixed");
    expect(container.querySelector("table")).not.toHaveClass("min-w-max");
  });
});

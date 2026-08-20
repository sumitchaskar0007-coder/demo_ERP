import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Input } from "./Input";

describe("Input trailing control", () => {
  it("centers a trailing control within the input instead of the labeled field", () => {
    render(
      <Input
        name="password"
        label="Password"
        trailing={<button type="button">Show password</button>}
      />,
    );

    const input = screen.getByLabelText("Password");
    const button = screen.getByRole("button", { name: "Show password" });

    expect(input).toHaveClass("pr-12");
    expect(button.parentElement).toHaveClass("inset-y-0", "items-center");
  });
});

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./Button";

describe("Button responsive sizing", () => {
  it("keeps action labels intact when their parent becomes narrow", () => {
    render(<Button>Review admission</Button>);

    const button = screen.getByRole("button", { name: "Review admission" });
    expect(button).toHaveClass("shrink-0", "whitespace-nowrap", "[word-break:normal]");
  });
});

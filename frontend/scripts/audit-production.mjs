import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";

const acceptedAdvisories = new Set([
  // The app is a client-only declarative SPA and does not use the affected
  // framework/RSC server-action APIs. Remove this exception once fixed upstream.
  "https://github.com/advisories/GHSA-qwww-vcr4-c8h2",
]);

const result =
  process.platform === "win32"
    ? spawnSync(process.env.ComSpec, ["/d", "/s", "/c", "npm.cmd audit --omit=dev --json"], {
        encoding: "utf8",
      })
    : spawnSync("npm", ["audit", "--omit=dev", "--json"], { encoding: "utf8" });

if (!result.stdout) {
  process.stderr.write(result.stderr || "npm audit returned no JSON output\n");
  process.exit(1);
}

let report;
try {
  report = JSON.parse(result.stdout);
} catch {
  process.stderr.write(result.stdout);
  process.stderr.write(result.stderr || "npm audit returned invalid JSON\n");
  process.exit(1);
}

if (report.error) {
  process.stderr.write(`npm audit failed: ${report.error.summary || "unknown error"}\n`);
  process.exit(1);
}

const packageJson = JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8"));
const routerVersion = packageJson.dependencies?.["react-router-dom"];
if (routerVersion !== "7.18.2") {
  process.stderr.write(`Expected the reviewed React Router pin 7.18.2, found ${routerVersion}\n`);
  process.exit(1);
}

const blocking = [];
const accepted = [];
const vulnerabilities = report.vulnerabilities ?? {};
const isAccepted = (name, seen = new Set()) => {
  if (seen.has(name)) return false;
  seen.add(name);
  const vulnerability = vulnerabilities[name];
  if (!vulnerability) return false;
  return (vulnerability.via ?? []).every((entry) => {
    if (typeof entry === "string") return isAccepted(entry, new Set(seen));
    return acceptedAdvisories.has(entry.url);
  });
};

for (const [name, vulnerability] of Object.entries(vulnerabilities)) {
  if (!new Set(["high", "critical"]).has(vulnerability.severity)) continue;
  const advisories = (vulnerability.via ?? []).filter((entry) => typeof entry === "object");
  if (!isAccepted(name)) blocking.push({ name, vulnerability });
  else accepted.push(...advisories.map((entry) => `${name}: ${entry.url}`));
}

if (blocking.length) {
  process.stderr.write(`${JSON.stringify(blocking, null, 2)}\n`);
  process.exit(1);
}

for (const advisory of accepted)
  process.stdout.write(`Accepted non-applicable advisory: ${advisory}\n`);
process.stdout.write(
  "No applicable high or critical production dependency vulnerabilities found.\n",
);

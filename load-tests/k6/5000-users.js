import http from "k6/http";
import { sleep } from "k6";
import { K6_OPTIONS, SETTINGS, validateSafety } from "./lib/config.js";
import { seedSafetyMetrics } from "./lib/http.js";
import {
  asyncReport,
  attendanceWrite,
  contextFor,
  coreReads,
  login,
  logout,
  presignedTransfer,
  tokenExpiry,
} from "./lib/journeys.js";

const accounts = JSON.parse(open(SETTINGS.usersFile));
const syntheticUpload = open("./fixtures/synthetic-upload.pdf", "b");

export const options = K6_OPTIONS;

export function setup() {
  validateSafety(accounts);

  const response = http.get(`${SETTINGS.baseUrl}${SETTINGS.safetyProbePath}`, {
    redirects: 0,
    responseType:
      SETTINGS.safetyExpectedText || SETTINGS.safetyHeaderName
        ? "text"
        : "none",
    tags: { name: "safety-preflight", journey: "safety-preflight" },
  });
  if (response.status !== 200) {
    throw new Error(
      `Safety probe returned HTTP ${response.status}; refusing to start`,
    );
  }
  if (
    SETTINGS.safetyExpectedText &&
    (!response.body || !response.body.includes(SETTINGS.safetyExpectedText))
  ) {
    throw new Error("Safety probe did not contain SAFETY_PROBE_EXPECTED_TEXT");
  }
  if (SETTINGS.safetyHeaderName) {
    const actual = response.headers[SETTINGS.safetyHeaderName];
    if (!SETTINGS.safetyHeaderValue || actual !== SETTINGS.safetyHeaderValue) {
      throw new Error(
        "Safety environment header did not match the configured value",
      );
    }
  }

  return {
    runId: SETTINGS.runId,
    profile: SETTINGS.profile,
    accountCount: accounts.length,
    targetHost: SETTINGS.hostname,
  };
}

export default function () {
  seedSafetyMetrics();
  const account = accounts[__VU - 1];
  const context = contextFor(account);
  const loginResponse = login(account, context);
  if (loginResponse.status !== 200) {
    sleep(1);
    return;
  }

  coreReads(account, context);
  attendanceWrite(account, context);
  asyncReport(account, context);
  presignedTransfer(account, context, syntheticUpload);
  tokenExpiry(account, context);
  logout(account, context);
  sleep(1 + Math.random() * 3);
}

export function teardown(data) {
  if (!data || data.runId !== SETTINGS.runId) {
    throw new Error("Load-test teardown lost its run identity");
  }
}

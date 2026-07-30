import http from "k6/http";
import { check, group, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const users = JSON.parse(open(__ENV.USERS_FILE || "./users.example.json"));
const baseUrl = (__ENV.BASE_URL || "http://localhost:8081").replace(/\/$/, "");
const readLatency = new Trend("read_api_duration", true);
const writeLatency = new Trend("write_api_duration", true);
const legitimate429 = new Rate("legitimate_429");

export const options = {
  discardResponseBodies: false,
  stages: [
    { duration: "5m", target: 500 },
    { duration: "10m", target: 500 },
    { duration: "5m", target: 1000 },
    { duration: "10m", target: 1000 },
    { duration: "10m", target: 2500 },
    { duration: "15m", target: 2500 },
    { duration: "15m", target: 5000 },
    { duration: "20m", target: 5000 },
    { duration: "10m", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    read_api_duration: ["p(95)<1000"],
    write_api_duration: ["p(95)<2000"],
    legitimate_429: ["rate==0"],
  },
};

function record(response, write = false) {
  (write ? writeLatency : readLatency).add(response.timings.duration);
  legitimate429.add(response.status === 429);
  return response;
}

function csrfHeaders() {
  record(http.get(`${baseUrl}/api/v1/auth/csrf`, { tags: { journey: "csrf" } }));
  const token = http.cookieJar().cookiesForURL(baseUrl)["XSRF-TOKEN"]?.[0];
  return token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {};
}

export function setup() {
  if (!Array.isArray(users) || users.length === 0) throw new Error("USERS_FILE must contain synthetic users");
  if (__ENV.CONFIRM_NON_PRODUCTION !== "YES") {
    throw new Error("Set CONFIRM_NON_PRODUCTION=YES only after verifying the target is an isolated load-test environment");
  }
}

export default function () {
  const account = users[(__VU - 1) % users.length];
  group("login", () => {
    const response = record(http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({
      email: account.email,
      password: account.password,
    }), { headers: { "Content-Type": "application/json" }, tags: { journey: "login" } }), true);
    check(response, { "login succeeds": (r) => r.status === 200 });
  });

  sleep(1 + Math.random() * 3);
  group("dashboard and notices", () => {
    const requests = [
      ["GET", `${baseUrl}/api/v1/auth/me`, null, { tags: { journey: "dashboard" } }],
      ["GET", `${baseUrl}/api/notices/unread-count`, null, { tags: { journey: "notice-count" } }],
    ];
    http.batch(requests).forEach((response) => {
      record(response);
      check(response, { "read succeeds": (r) => r.status === 200 });
    });
  });

  sleep(2 + Math.random() * 6);
  const role = account.role || "STUDENT";
  group("role workspace", () => {
    const path = role === "STUDENT"
      ? "/api/student/attendance"
      : role === "SUBJECT_TEACHER"
        ? "/api/teacher/attendance/today-lectures"
        : "/api/dashboard/" + role.toLowerCase().replaceAll("_", "-");
    const response = record(http.get(`${baseUrl}${path}`, { tags: { journey: "workspace" } }));
    check(response, { "workspace is authorized": (r) => r.status === 200 || r.status === 404 });
  });

  if (__ENV.ENABLE_SAFE_WRITES === "true" && account.safeWritePath) {
    sleep(2 + Math.random() * 4);
    const response = record(http.post(
      `${baseUrl}${account.safeWritePath}`,
      JSON.stringify(account.safeWriteBody || {}),
      { headers: { "Content-Type": "application/json", ...csrfHeaders() }, tags: { journey: "safe-write" } },
    ), true);
    check(response, { "configured safe write succeeds": (r) => r.status >= 200 && r.status < 300 });
  }

  sleep(2 + Math.random() * 5);
  group("logout", () => {
    const response = record(http.post(`${baseUrl}/api/v1/auth/logout`, null, {
      headers: csrfHeaders(), tags: { journey: "logout" },
    }), true);
    check(response, { "logout succeeds": (r) => r.status === 200 });
  });
}

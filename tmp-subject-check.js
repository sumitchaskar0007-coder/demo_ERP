const fetch = globalThis.fetch;
(async () => {
  try {
    const loginRes = await fetch('http://localhost:8081/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'admin@erp.com', password: 'Admin@12345' }),
    });
    const loginJson = await loginRes.json();
    console.log('LOGIN_STATUS', loginRes.status);
    console.log(JSON.stringify(loginJson, null, 2));
    if (!loginJson.data?.token) return;
    const token = loginJson.data.token;
    const subjectsRes = await fetch('http://localhost:8081/api/academic/subjects/search', {
      method: 'GET',
      headers: { Authorization: 'Bearer ' + token },
    });
    const subjectsJson = await subjectsRes.json();
    console.log('SUBJECTS_STATUS', subjectsRes.status);
    console.log(JSON.stringify(subjectsJson, null, 2));
  } catch (error) {
    console.error(error);
  }
})();

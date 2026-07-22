const fetch = globalThis.fetch;
(async () => {
  try {
    const email = process.env.ERP_ADMIN_EMAIL;
    const password = process.env.ERP_ADMIN_PASSWORD;
    if (!email || !password) throw new Error('ERP_ADMIN_EMAIL and ERP_ADMIN_PASSWORD are required');
    const loginRes = await fetch('http://localhost:8081/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
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

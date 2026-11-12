import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.API_URL || 'http://localhost:8080/api/v1';

const errorsRate = new Rate('errors');

export const options = {
  scenarios: {
    warmup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
      ],
      gracefulRampDown: '10s',
    },
    peak: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 },
      ],
      startTime: '30s',
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.1'],
  },
};

export function setup() {
  const timestamp = Date.now();
  const nickname = `loaduser_${timestamp}`;
  const password = 'LoadTestPass123!';

  const regRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ nickname, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (regRes.status !== 200 && regRes.status !== 201) {
    throw new Error(`Registration failed: ${regRes.status} - ${regRes.body}`);
  }

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ nickname, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) {
    throw new Error(`Login failed: ${loginRes.status} - ${loginRes.body}`);
  }

  const token = loginRes.json().token;

  const groupRes = http.post(
    `${BASE_URL}/groups`,
    JSON.stringify({ name: `LoadTestGroup_${timestamp}`, ageRestriction: 18 }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  if (groupRes.status !== 200) {
    throw new Error(`Group creation failed: ${groupRes.status} - ${groupRes.body}`);
  }

  const groupId = groupRes.json().groupId;

  return { token, groupId, nickname, password };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  const rand = Math.random();

  if (rand < 0.7) {
    const res = http.get(`${BASE_URL}/groups/${data.groupId}/recommendations/last`, {
      headers,
      tags: { name: 'get_recommendations' },
    });

    const ok = check(res, {
      'recommendations 200': (r) => r.status === 200,
    });
    errorsRate.add(!ok);
    sleep(Math.random() * 2 + 1);
  } else if (rand < 0.9) {
    const res = http.get(`${BASE_URL}/groups/${data.groupId}`, {
      headers,
      tags: { name: 'get_group' },
    });

    const ok = check(res, {
      'group 200': (r) => r.status === 200,
    });
    errorsRate.add(!ok);
    sleep(Math.random() * 1 + 0.5);
  } else {
    const res = http.get(`${BASE_URL}/groups`, {
      headers,
      tags: { name: 'list_groups' },
    });

    const ok = check(res, {
      'groups 200': (r) => r.status === 200,
    });
    errorsRate.add(!ok);
    sleep(Math.random() * 1);
  }
}

export function teardown(data) {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ nickname: data.nickname, password: data.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) return;

  const token = loginRes.json().token;

  http.del(`${BASE_URL}/groups/${data.groupId}`, null, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
}
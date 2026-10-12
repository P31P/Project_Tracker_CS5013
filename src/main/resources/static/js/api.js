const SIGN_IN = '/login.html';

// Spring Security puts the CSRF token in this cookie; every POST, PATCH or DELETE has to send it back
function csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : '';
}

async function handle(response) {
    if (response.status === 401) {
        location.href = SIGN_IN;
        throw new Error('Signed out');
    }
    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.error || 'Something went wrong (HTTP ' + response.status + ')');
    }
    return response.json();
}

export async function getJson(url) {
    return handle(await fetch(url, { headers: { Accept: 'application/json' } }));
}

export async function sendJson(method, url, body) {
    return handle(await fetch(url, {
        method,
        headers: { Accept: 'application/json', 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken() },
        body: body === undefined ? undefined : JSON.stringify(body),
    }));
}

export async function signOut() {
    await fetch('/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrfToken() } });
    location.href = SIGN_IN + '?logout';
}

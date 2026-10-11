const SIGN_IN = '/login.html';

// Spring Security puts the CSRF token in this cookie; every POST, PATCH or DELETE has to send it back
function csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : '';
}

export async function getJson(url) {
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
    if (response.status === 401) {
        location.href = SIGN_IN;
        throw new Error('Signed out');
    }
    if (!response.ok) {
        throw new Error('HTTP ' + response.status);
    }
    return response.json();
}

export async function signOut() {
    await fetch('/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrfToken() } });
    location.href = SIGN_IN + '?logout';
}

import { getJson, signOut } from './api.js';
import { renderPapers } from './papers.js';
import { el, initials, toast } from './ui.js';

const tabBar = document.getElementById('tabs');
const content = document.getElementById('content');
const tabs = new Map();

function registerTab(id, label, render, description) {
    const button = el('button', 'tab', label);
    button.type = 'button';
    button.id = 'tab-' + id;
    button.setAttribute('role', 'tab');
    button.addEventListener('click', () => {
        location.hash = id;
    });
    tabBar.append(button);
    tabs.set(id, { label, render, description });
}

function showTab() {
    const requested = location.hash.slice(1);
    const id = tabs.has(requested) ? requested : tabs.keys().next().value;
    const tab = tabs.get(id);
    for (const button of tabBar.children) {
        button.setAttribute('aria-selected', String(button.id === 'tab-' + id));
    }

    const actions = el('div', 'page-actions');
    const header = el('header', 'page-header',
        el('div', null, el('h1', 'page-title', tab.label), el('p', 'page-description', tab.description)),
        actions);
    // a fresh panel per visit, so a tab that refreshes itself stops once it is no longer on the page
    const panel = el('section');
    panel.setAttribute('role', 'tabpanel');
    panel.setAttribute('aria-labelledby', 'tab-' + id);
    content.replaceChildren(header, panel);
    document.title = tab.label + ' · Raman Lab Tracker';
    tab.render(panel, { actions });
}

async function showUser() {
    try {
        const me = await getJson('/api/me');
        const name = me.name || me.email;
        document.getElementById('avatar').textContent = initials(name);
        document.getElementById('user-name').textContent = name;
        document.getElementById('user-email').textContent = me.email;
    } catch (error) {
        // only the account menu needs this; the tabs work without it
    }
}

document.getElementById('sign-out').addEventListener('click', () => {
    signOut().catch(() => toast('Could not sign out. Try again.', 'error'));
});

tabBar.setAttribute('role', 'tablist');
registerTab('papers', 'Papers', renderPapers, 'Papers shared in Slack, with their details filled in automatically.');

window.addEventListener('hashchange', showTab);
showTab();
showUser();

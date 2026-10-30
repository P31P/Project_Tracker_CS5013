import { getJson, signOut } from './api.js';
import { renderHome } from './home.js';
import { icon } from './icons.js';
import { renderIdeas } from './ideas.js';
import { renderPapers } from './papers.js';
import { renderProjects } from './projects.js';
import { renderThemePicker } from './theme.js';
import { el, hueOf, initials, toast } from './ui.js';

const tabBar = document.getElementById('tabs');
const content = document.getElementById('content');
const tabs = new Map();

function registerTab(id, label, render, description, iconName) {
    const button = el('button', 'tab', icon(iconName, 16), label);
    button.type = 'button';
    button.id = 'tab-' + id;
    button.setAttribute('role', 'tab');
    button.addEventListener('click', () => {
        location.hash = id;
    });
    tabBar.append(button);
    tabs.set(id, { label, render, description });
}

// "#projects/12" opens the projects tab with path "12"
function showTab() {
    const [requested, ...rest] = location.hash.slice(1).split('/');
    const id = tabs.has(requested) ? requested : tabs.keys().next().value;
    const tab = tabs.get(id);
    for (const button of tabBar.children) {
        button.setAttribute('aria-selected', String(button.id === 'tab-' + id));
    }

    const heading = el('h1', 'page-title', tab.label);
    const description = el('p', 'page-description', tab.description);
    const actions = el('div', 'page-actions');
    // a fresh panel per visit, so a tab that refreshes itself stops once it is no longer on the page
    const panel = el('section');
    panel.setAttribute('role', 'tabpanel');
    panel.setAttribute('aria-labelledby', 'tab-' + id);
    content.replaceChildren(el('header', 'page-header', el('div', null, heading, description), actions), panel);
    document.title = tab.label + ' · Raman Lab Tracker';
    tab.render(panel, { actions, heading, description, path: rest.join('/') });
}

async function showUser() {
    try {
        const me = await getJson('/api/me');
        const name = me.name || me.email;
        const avatar = document.getElementById('avatar');
        avatar.textContent = initials(name);
        avatar.style.setProperty('--hue', hueOf(name));
        document.getElementById('user-name').textContent = name;
        document.getElementById('user-email').textContent = me.email;
    } catch (error) {
        // only the account menu needs this; the tabs work without it
    }
}

renderThemePicker(document.getElementById('theme-picker'));

const signOutButton = document.getElementById('sign-out');
signOutButton.prepend(icon('logout', 16));
signOutButton.addEventListener('click', () => {
    signOut().catch(() => toast('Could not sign out. Try again.', 'error'));
});

tabBar.setAttribute('role', 'tablist');
registerTab('home', 'Home', renderHome, 'What is happening across the lab.', 'home');
registerTab('projects', 'Projects', renderProjects, 'What the lab is working on, and how each project is moving.', 'folder');
registerTab('ideas', 'Ideas', renderIdeas, 'Thoughts worth keeping. Upvote the good ones, and turn one into a project when it is ready.', 'bulb');
registerTab('papers', 'Papers', renderPapers, 'Papers shared in Slack, with their details filled in automatically.', 'paper');

window.addEventListener('hashchange', showTab);
showTab();
showUser();

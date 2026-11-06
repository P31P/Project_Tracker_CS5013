// People tab: everyone in the lab with their background and interests (#people), and one person's page with
// their profile, projects and ideas (#people/<id>).

import { getJson } from './api.js';
import { icon } from './icons.js';
import { loadProfileStyles, profileCard, roleBadge } from './profile.js';
import { statusChip } from './projects.js';
import {
    avatar, avatarStack, button, el, emptyState, errorState, input, relativeTime, skeleton, tagList, tagPill,
} from './ui.js';

export async function renderPeople(container, page) {
    loadProfileStyles();
    if (page.path) {
        return renderPerson(container, page, page.path);
    }
    container.replaceChildren(skeleton(3));
    let people;
    try {
        people = await getJson('/api/people/directory');
    } catch (problem) {
        container.replaceChildren(errorState('Could not load the lab members. ' + problem.message));
        return;
    }

    let interest = null;
    const nameBox = input('name', { placeholder: 'Find someone by name' });
    nameBox.type = 'search';
    nameBox.setAttribute('aria-label', 'Find someone by name');
    const list = el('div');
    const pickInterest = picked => {
        interest = picked;
        draw();
    };
    const draw = () => {
        const text = nameBox.value.trim().toLowerCase();
        const shown = people.filter(person => (!text || person.name.toLowerCase().includes(text))
            && (!interest || person.interests.includes(interest)));
        list.replaceChildren(...[
            interest && el('div', 'active-filter', 'Showing people interested in ', tagPill(interest),
                button(['close', 'Clear'], 'link-button', () => pickInterest(null))),
            shown.length > 0
                ? el('div', 'people-grid', ...shown.map(person => personCard(person, pickInterest)))
                : el('p', 'muted', 'Nobody matches.'),
        ].filter(Boolean));
    };
    // the list redraws as you type, and doesn't animate, so typing stays calm
    nameBox.addEventListener('input', draw);
    container.replaceChildren(el('div', 'people-toolbar', nameBox), list);
    draw();
}

function personCard(person, pickInterest) {
    const counts = [plural(person.projectCount, 'project'), plural(person.ideaCount, 'idea')].join(' · ');
    const name = el('a', 'person-name', person.name);
    name.href = '#people/' + person.id;
    return el('article', 'card person-card',
        el('div', 'person-head', avatar(person.name, 'avatar-person'),
            el('div', 'person-who', el('p', 'person-name-row', name, roleBadge(person.role)),
                el('p', 'person-background', person.background || 'No background added yet'))),
        tagList(person.interests, pickInterest),
        el('p', 'person-counts', counts));
}

async function renderPerson(container, page, id) {
    page.heading.textContent = 'Person';
    page.description.replaceChildren(backLink());
    container.replaceChildren(skeleton(2));

    let person;
    try {
        person = await getJson('/api/people/' + encodeURIComponent(id));
    } catch (problem) {
        container.replaceChildren(errorState(problem.message));
        return;
    }
    page.heading.textContent = person.profile.name;
    document.title = person.profile.name + ' · Raman Lab Tracker';
    container.replaceChildren(
        profileCard(person.profile),
        el('div', 'home-grid',
            el('section', 'home-section', el('h2', 'section-title', 'Projects'), projectList(person.projects)),
            el('section', 'home-section', el('h2', 'section-title', 'Ideas'), ideaList(person.ideas))));
}

function projectList(projects) {
    if (projects.length === 0) {
        return emptyState('No projects yet', 'Projects they start or join show up here.', null, 'folder');
    }
    return el('div', 'card mini-list', ...projects.map(project => {
        const row = el('a', 'mini-project',
            el('span', 'mini-main', el('span', 'mini-title', project.title),
                el('span', 'mini-meta', 'updated ' + relativeTime(project.updatedAt))),
            avatarStack(project.members, 3),
            statusChip(project.status));
        row.href = '#projects/' + project.id;
        return row;
    }));
}

function ideaList(ideas) {
    if (ideas.length === 0) {
        return emptyState('No ideas yet', 'Ideas they share show up here.', null, 'bulb');
    }
    return el('div', 'card mini-list', ...ideas.map(idea => {
        const row = el('a', 'mini-project',
            el('span', 'mini-votes', icon('up', 14), String(idea.voteCount)),
            el('span', 'mini-main', el('span', 'mini-title', idea.title),
                el('span', 'mini-meta', idea.projectId !== null
                    ? 'became a project' : 'shared ' + relativeTime(idea.createdAt))));
        row.href = idea.projectId !== null ? '#projects/' + idea.projectId : '#ideas';
        return row;
    }));
}

function backLink() {
    const back = el('a', 'back-link', icon('back', 16), 'All people');
    back.href = '#people';
    return back;
}

function plural(count, word) {
    return count + ' ' + word + (count === 1 ? '' : 's');
}

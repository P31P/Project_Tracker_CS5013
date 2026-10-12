// Projects tab: the list (#projects) and one project with its timeline (#projects/<id>).

import { getJson, sendJson } from './api.js';
import {
    button, el, emptyState, errorState, field, filterChips, formDialog, input, relativeTime, select, skeleton,
    textarea, toast,
} from './ui.js';

const STATUSES = [['PLANNING', 'Planning'], ['ACTIVE', 'Active'], ['ON_HOLD', 'On hold'], ['DONE', 'Done']];
const LABELS = Object.fromEntries(STATUSES);

export function renderProjects(container, page) {
    if (page.path) {
        return renderProject(container, page, page.path);
    }
    return renderList(container, page);
}

export function statusChip(status) {
    return el('span', 'status status-' + status.toLowerCase(), LABELS[status]);
}

/* List */

async function renderList(container, page) {
    page.actions.append(button('New project', 'button', () => newProjectDialog()));
    container.replaceChildren(skeleton());

    let projects;
    try {
        projects = await getJson('/api/projects');
    } catch (problem) {
        container.replaceChildren(errorState('Could not load projects. ' + problem.message));
        return;
    }
    if (projects.length === 0) {
        container.replaceChildren(emptyState('No projects yet',
            'Start one with just a title. You can add details and updates as it grows.',
            button('New project', 'button', () => newProjectDialog())));
        return;
    }

    let filter = 'ALL';
    const draw = () => {
        const shown = filter === 'ALL' ? projects : projects.filter(p => p.status === filter);
        const options = [['ALL', 'All', projects.length],
            ...STATUSES.map(([key, label]) => [key, label, projects.filter(p => p.status === key).length])];
        container.replaceChildren(
            filterChips(options, filter, key => { filter = key; draw(); }),
            shown.length > 0
                ? el('div', 'card-list', ...shown.map(projectCard))
                : el('p', 'muted', 'No projects with this status.'));
    };
    draw();
}

function projectCard(project) {
    const card = el('a', 'card card-link',
        el('div', 'card-head', el('h3', 'card-title', project.title), statusChip(project.status)),
        project.description && el('p', 'card-text clamp', project.description),
        el('p', 'card-meta', [
            project.ownerName,
            'updated ' + relativeTime(project.updatedAt),
            project.updateCount === 1 ? '1 update' : project.updateCount + ' updates',
        ].filter(Boolean).join(' · ')));
    card.href = '#projects/' + project.id;
    return card;
}

function newProjectDialog() {
    const more = el('details', 'more',
        el('summary', null, 'More details (optional)'),
        field('Description', textarea('description', { placeholder: 'What is it about? Any links or context.' })),
        field('Status', select('status', STATUSES, 'ACTIVE')));
    formDialog('New project', [
        field('Title', input('title', { placeholder: 'What are you working on?', required: true })),
        more,
    ], 'Create project', async values => {
        const project = await sendJson('POST', '/api/projects', values);
        toast('Project created');
        location.hash = 'projects/' + project.id;
    });
}

/* One project */

async function renderProject(container, page, id) {
    page.heading.textContent = 'Project';
    page.description.replaceChildren(backLink());
    container.replaceChildren(skeleton(2));

    let project;
    try {
        project = await getJson('/api/projects/' + id);
    } catch (problem) {
        container.replaceChildren(errorState(problem.message));
        return;
    }
    const redraw = updated => {
        project = updated;
        draw();
    };
    const draw = () => {
        page.heading.textContent = project.title;
        page.description.replaceChildren(backLink(), statusChip(project.status),
            el('span', 'muted', [project.ownerName && 'Owned by ' + project.ownerName,
                'started ' + relativeTime(project.createdAt)].filter(Boolean).join(' · ')));
        page.actions.replaceChildren(
            statusPicker(project, redraw),
            button('Edit', 'button button-secondary', () => editDialog(project, redraw)));
        const about = project.description ? [el('div', 'card', el('p', 'prose', project.description))] : [];
        container.replaceChildren(...about, composer(project, redraw), timeline(project.timeline));
    };
    draw();
}

function backLink() {
    const link = el('a', 'back-link', '← All projects');
    link.href = '#projects';
    return link;
}

function statusPicker(project, redraw) {
    const picker = select('status', STATUSES, project.status);
    picker.classList.add('input-compact');
    picker.setAttribute('aria-label', 'Status');
    picker.addEventListener('change', async () => {
        try {
            redraw(await sendJson('PATCH', '/api/projects/' + project.id, { status: picker.value }));
            toast('Status changed to ' + LABELS[picker.value]);
        } catch (problem) {
            picker.value = project.status;
            toast(problem.message, 'error');
        }
    });
    return picker;
}

function editDialog(project, redraw) {
    formDialog('Edit project', [
        field('Title', input('title', { value: project.title, required: true })),
        field('Description', textarea('description', { value: project.description || '', rows: 5 })),
    ], 'Save', async values => {
        redraw(await sendJson('PATCH', '/api/projects/' + project.id, values));
        toast('Project saved');
    });
}

function composer(project, redraw) {
    const body = textarea('body', { placeholder: 'What has changed? Results, blockers, next steps…', rows: 3 });
    const post = el('button', 'button', 'Post update');
    post.type = 'submit';
    const form = el('form', 'card composer', body, el('div', 'composer-actions', post));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        post.disabled = true;
        try {
            await sendJson('POST', '/api/projects/' + project.id + '/updates', { body: body.value });
            redraw(await getJson('/api/projects/' + project.id));
            toast('Update posted');
        } catch (problem) {
            toast(problem.message, 'error');
            post.disabled = false;
        }
    });
    return form;
}

function timeline(entries) {
    if (entries.length === 0) {
        return el('p', 'muted timeline-empty', 'No updates yet. Post the first one above.');
    }
    return el('ol', 'timeline', ...entries.map(entry => el('li', 'timeline-item timeline-' + entry.kind.toLowerCase(),
        el('p', 'timeline-meta',
            el('strong', null, entry.authorName || 'Someone'), ' · ' + relativeTime(entry.createdAt)),
        el('p', entry.kind === 'STATUS' ? 'timeline-change' : 'prose', entry.body))));
}

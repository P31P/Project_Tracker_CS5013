// Projects tab: the list or board (#projects) and one project with its timeline (#projects/<id>).

import { getJson, sendJson } from './api.js';
import { suggestButton } from './autofill.js';
import { icon } from './icons.js';
import { markdown, markdownHint, plainText } from './markdown.js';
import {
    avatar, avatarStack, button, el, emptyState, errorState, field, filterChips, formDialog, input, relativeTime,
    segmented, select, skeleton, splitTags, tagList, tagPill, textarea, toast,
} from './ui.js';

const STATUSES = [['PLANNING', 'Planning'], ['ACTIVE', 'Active'], ['ON_HOLD', 'On hold'], ['DONE', 'Done']];
const LABELS = Object.fromEntries(STATUSES);
const VIEW_KEY = 'projects-view';

export function renderProjects(container, page) {
    if (page.path) {
        return renderProject(container, page, page.path);
    }
    return renderList(container, page);
}

export function statusChip(status) {
    return el('span', 'status status-' + status.toLowerCase(), LABELS[status]);
}

/* List and board */

async function renderList(container, page) {
    const newButton = () => button(['plus', 'New project'], 'button', () => newProjectDialog());
    page.actions.append(newButton());
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
            'Start one with just a title. You can add people, tags, links and updates as it grows.', newButton(),
            'folder'));
        return;
    }

    let view = savedView();
    let status = 'ALL';
    let tag = null;
    const pickTag = picked => {
        tag = picked;
        draw();
    };
    const move = async (id, newStatus) => {
        const project = projects.find(p => p.id === id);
        if (!project || project.status === newStatus) {
            return;
        }
        try {
            const updated = await sendJson('PATCH', '/api/projects/' + id, { status: newStatus });
            Object.assign(project, { status: updated.status, updatedAt: updated.updatedAt,
                updateCount: updated.timeline.length });
            toast(project.title + ' moved to ' + LABELS[newStatus]);
        } catch (problem) {
            toast(problem.message, 'error');
        }
        draw();
    };
    const draw = () => {
        page.actions.replaceChildren(
            segmented([['list', 'List', 'list'], ['board', 'Board', 'board']], view, picked => {
                view = picked;
                saveView(picked);
                draw();
            }),
            newButton());
        const tagged = tag ? projects.filter(p => p.tags.includes(tag)) : projects;
        const tagFilter = tag && el('div', 'active-filter', 'Showing projects tagged ', tagPill(tag),
            button(['close', 'Clear'], 'link-button', () => pickTag(null)));
        if (view === 'board') {
            container.replaceChildren(...[tagFilter, board(tagged, pickTag, move)].filter(Boolean));
            return;
        }
        const shown = status === 'ALL' ? tagged : tagged.filter(p => p.status === status);
        const options = [['ALL', 'All', tagged.length],
            ...STATUSES.map(([key, label]) => [key, label, tagged.filter(p => p.status === key).length])];
        container.replaceChildren(...[
            filterChips(options, status, key => { status = key; draw(); }),
            tagFilter,
            shown.length > 0
                ? el('div', 'card-list', ...shown.map((project, index) => projectCard(project, pickTag, index)))
                : el('p', 'muted', 'No projects match.'),
        ].filter(Boolean));
    };
    draw();
}

// the whole card opens the project; tags stay clickable on top of it
function projectCard(project, pickTag, index = 0) {
    const link = el('a', 'stretched-link', project.title);
    link.href = '#projects/' + project.id;
    const card = el('article', 'card card-link project-card',
        el('div', 'card-head', el('h3', 'card-title', link), statusChip(project.status)),
        project.description && el('p', 'card-text clamp', plainText(project.description)),
        tagList(project.tags, pickTag),
        el('div', 'card-foot',
            avatarStack(project.members),
            el('span', 'card-meta-inline', icon('message', 14), String(project.updateCount),
                el('span', null, '· updated ' + relativeTime(project.updatedAt)))));
    card.style.setProperty('--i', Math.min(index, 10));
    return card;
}

function board(projects, pickTag, move) {
    return el('div', 'board', ...STATUSES.map(([key, label]) => {
        const items = projects.filter(p => p.status === key);
        const cards = el('div', 'board-cards', ...items.map(project => boardCard(project, pickTag)));
        if (items.length === 0) {
            cards.append(el('p', 'board-empty', 'Drag a project here'));
        }
        const column = el('section', 'board-column status-' + key.toLowerCase(),
            el('header', 'board-head', el('span', 'board-dot'), el('h2', 'board-title', label),
                el('span', 'board-count', String(items.length))),
            cards);
        column.addEventListener('dragover', event => {
            event.preventDefault();
            column.classList.add('drop-target');
        });
        column.addEventListener('dragleave', event => {
            if (!column.contains(event.relatedTarget)) {
                column.classList.remove('drop-target');
            }
        });
        column.addEventListener('drop', event => {
            event.preventDefault();
            column.classList.remove('drop-target');
            move(Number(event.dataTransfer.getData('text/plain')), key);
        });
        return column;
    }));
}

function boardCard(project, pickTag) {
    const card = projectCard(project, pickTag);
    card.classList.add('board-card');
    card.draggable = true;
    // the card is what gets dragged, not the link inside it
    card.querySelector('a').draggable = false;
    card.addEventListener('dragstart', event => {
        event.dataTransfer.setData('text/plain', String(project.id));
        event.dataTransfer.effectAllowed = 'move';
        card.classList.add('dragging');
    });
    card.addEventListener('dragend', () => card.classList.remove('dragging'));
    return card;
}

function savedView() {
    try {
        return localStorage.getItem(VIEW_KEY) === 'board' ? 'board' : 'list';
    } catch {
        return 'list';
    }
}

function saveView(view) {
    try {
        localStorage.setItem(VIEW_KEY, view);
    } catch {
        // the choice just isn't remembered
    }
}

/* Creating and editing */

let people;

// everyone who can sign in; fetched once per page load
async function loadPeople() {
    if (!people) {
        people = getJson('/api/people').catch(error => {
            people = null;
            throw error;
        });
    }
    return people;
}

function memberPicker(everyone, selectedIds) {
    return el('div', 'member-picker', ...everyone.map(person => {
        const box = el('input');
        box.type = 'checkbox';
        box.value = person.id;
        box.checked = selectedIds.includes(person.id);
        return el('label', 'member-option', box, avatar(person.name), el('span', null, person.name));
    }));
}

function pickedIds(picker) {
    return [...picker.querySelectorAll('input:checked')].map(box => Number(box.value));
}

export async function newProjectDialog() {
    const everyone = await loadPeople().catch(() => []);
    const picker = memberPicker(everyone, []);
    const title = input('title', { placeholder: 'What are you working on?', required: true });
    const description = textarea('description', { placeholder: 'What is it about? Any context.' });
    const tags = input('tags', { placeholder: 'e.g. genomics, ml' });
    const more = el('details', 'more',
        el('summary', null, 'More details (optional)'),
        field('Description', description),
        markdownHint(),
        field('Status', select('status', STATUSES, 'ACTIVE')),
        field('Tags', tags),
        everyone.length > 0 && field('Teammates (you are added automatically)', picker));
    const suggest = suggestButton(() => title.value + '\n' + description.value, suggestion => {
        title.value = suggestion.title;
        description.value = suggestion.description || '';
        tags.value = suggestion.tags.join(', ');
        more.open = true;
    });
    formDialog('New project', [
        field('Title', title),
        el('div', 'autofill-row', suggest),
        more,
    ], 'Create project', async values => {
        const project = await sendJson('POST', '/api/projects',
            { ...values, tags: splitTags(values.tags), memberIds: pickedIds(picker) });
        toast('Project created');
        location.hash = 'projects/' + project.id;
    });
}

async function editDialog(project, redraw) {
    const everyone = await loadPeople().catch(() => null);
    const picker = everyone && memberPicker(everyone, project.members.map(member => member.id));
    formDialog('Edit project', [
        field('Title', input('title', { value: project.title, required: true })),
        field('Description', textarea('description', { value: project.description || '', rows: 5 })),
        markdownHint(),
        field('Tags', input('tags', { value: project.tags.join(', '), placeholder: 'e.g. genomics, ml' })),
        picker && field('Members', picker),
    ].filter(Boolean), 'Save', async values => {
        const changes = { ...values, tags: splitTags(values.tags) };
        if (picker) {
            changes.memberIds = pickedIds(picker);
        }
        redraw(await sendJson('PATCH', '/api/projects/' + project.id, changes));
        toast('Project saved');
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
        document.title = project.title + ' · Raman Lab Tracker';
        page.description.replaceChildren(backLink(), statusChip(project.status),
            el('span', 'muted', [project.ownerName && 'Owned by ' + project.ownerName,
                'started ' + relativeTime(project.createdAt)].filter(Boolean).join(' · ')));
        page.actions.replaceChildren(button(['edit', 'Edit'], 'button button-secondary', () => editDialog(project, redraw)));
        const about = project.description && el('div', 'card', markdown(project.description));
        container.replaceChildren(el('div', 'detail-layout',
            el('div', 'detail-main', about, composer(project, redraw),
                el('h2', 'section-title', 'Timeline'), timeline(project.timeline)),
            el('aside', 'card detail-side', ...sidePanel(project, redraw))));
    };
    draw();
}

function backLink() {
    const link = el('a', 'back-link', icon('back', 16), 'All projects');
    link.href = '#projects';
    return link;
}

function sideSection(title, ...children) {
    return el('section', 'side-section', el('h2', 'side-title', title), ...children);
}

function sidePanel(project, redraw) {
    return [
        sideSection('Status', statusPicker(project, redraw)),
        sideSection('Members', project.members.length > 0
            ? el('ul', 'people', ...project.members.map(person =>
                el('li', 'person', avatar(person.name), el('span', null, person.name))))
            : el('p', 'muted side-empty', 'No members yet.')),
        sideSection('Tags', tagList(project.tags) || el('p', 'muted side-empty', 'No tags yet. Add some with Edit.')),
        sideSection('Links', linkList(project, redraw), linkForm(project, redraw)),
    ];
}

function statusPicker(project, redraw) {
    const picker = select('status', STATUSES, project.status);
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

function linkList(project, redraw) {
    if (project.links.length === 0) {
        return el('p', 'muted side-empty', 'Repos, docs, slides or datasets.');
    }
    return el('ul', 'links', ...project.links.map(link => {
        const anchor = el('a', 'link-item', icon('external', 14), el('span', 'link-label', link.label));
        // the server only stores http(s) links; checked again so nothing else ever becomes a href
        if (/^https?:\/\//i.test(link.url)) {
            anchor.href = link.url;
        }
        anchor.target = '_blank';
        anchor.rel = 'noopener noreferrer';
        anchor.title = link.url;
        const remove = button(icon('close', 14), 'icon-button', async () => {
            try {
                redraw(await sendJson('DELETE', '/api/projects/' + project.id + '/links/' + link.id));
                toast('Link removed');
            } catch (problem) {
                toast(problem.message, 'error');
            }
        });
        remove.setAttribute('aria-label', 'Remove ' + link.label);
        return el('li', 'link-row', anchor, remove);
    }));
}

function linkForm(project, redraw) {
    const url = input('url', { placeholder: 'Paste a link', required: true });
    url.setAttribute('aria-label', 'Link URL');
    const label = input('label', { placeholder: 'Name (optional)' });
    label.setAttribute('aria-label', 'Link name');
    const add = el('button', 'button button-secondary button-small', icon('plus', 16), 'Add link');
    add.type = 'submit';
    const form = el('form', 'link-form', url, label, add);
    form.addEventListener('submit', async event => {
        event.preventDefault();
        add.disabled = true;
        // "github.com/lab/repo" is taken to mean https://github.com/lab/repo
        const address = /^[a-z]+:/i.test(url.value.trim()) ? url.value.trim() : 'https://' + url.value.trim();
        try {
            redraw(await sendJson('POST', '/api/projects/' + project.id + '/links', { label: label.value, url: address }));
            toast('Link added');
        } catch (problem) {
            toast(problem.message, 'error');
            add.disabled = false;
        }
    });
    return form;
}

function composer(project, redraw) {
    const body = textarea('body', { placeholder: 'What has changed? Results, blockers, next steps…', rows: 3 });
    const post = el('button', 'button', icon('message', 16), 'Post update');
    post.type = 'submit';
    const form = el('form', 'card composer', body, el('div', 'composer-actions', markdownHint(), post));
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
            avatar(entry.authorName || 'Someone'),
            el('strong', null, entry.authorName || 'Someone'), ' · ' + relativeTime(entry.createdAt)),
        entry.kind === 'STATUS' ? el('p', 'timeline-change', entry.body) : markdown(entry.body))));
}

// Projects tab: the list or board (#projects) and one project with its timeline (#projects/<id>).

import { getJson, sendJson } from './api.js';
import { suggestButton } from './autofill.js';
import {
    collabBadge, collabSelect, detailFields, detailSections, detailsFrom, fillDetails, hasDetails,
} from './details.js';
import { discussionSection } from './discussion.js';
import { icon } from './icons.js';
import { markdown, markdownHint, plainText } from './markdown.js';
import { linkForm, linkList, paperList, paperPicker } from './references.js';
import { tagPicker } from './tags.js';
import {
    avatar, avatarStack, button, el, emptyState, errorState, field, filterChips, formDialog, input, relativeTime,
    segmented, select, skeleton, splitTags, tagList, tagPill, textarea, toast,
} from './ui.js';

const STATUSES = [['PLANNING', 'Planning'], ['ACTIVE', 'Ongoing'], ['ON_HOLD', 'On hold'], ['DONE', 'Completed']];
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
    let openOnly = false;
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
        const open = projects.filter(p => !openOnly || p.openForCollab);
        const tagged = tag ? open.filter(p => p.tags.includes(tag)) : open;
        const openToggle = button(['users', 'Open for collaboration'], 'filter-chip', () => {
            openOnly = !openOnly;
            draw();
        });
        openToggle.setAttribute('aria-pressed', String(openOnly));
        const tagFilter = tag && el('div', 'active-filter', 'Showing projects tagged ', tagPill(tag),
            button(['close', 'Clear'], 'link-button', () => pickTag(null)));
        if (view === 'board') {
            container.replaceChildren(...[el('div', 'filters', openToggle), tagFilter,
                board(tagged, pickTag, move)].filter(Boolean));
            return;
        }
        const shown = status === 'ALL' ? tagged : tagged.filter(p => p.status === status);
        const options = [['ALL', 'All', tagged.length],
            ...STATUSES.map(([key, label]) => [key, label, tagged.filter(p => p.status === key).length])];
        container.replaceChildren(...[
            el('div', 'toolbar', filterChips(options, status, key => { status = key; draw(); }), openToggle),
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
        el('div', 'card-head', el('h3', 'card-title', link),
            el('span', 'card-badges', collabBadge(project.openForCollab), statusChip(project.status))),
        project.description && el('p', 'card-text clamp', plainText(project.description)),
        tagList(project.tags, pickTag),
        el('div', 'card-foot',
            avatarStack(project.members),
            el('span', 'card-meta-inline', icon('up', 14), String(project.voteCount),
                icon('message', 14), String(project.updateCount),
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
    const tags = tagPicker('tags');
    const more = el('details', 'more',
        el('summary', null, 'More details (optional)'),
        field('Description', description),
        markdownHint(),
        el('div', 'field-row',
            field('Status', select('status', STATUSES, 'ACTIVE')),
            field('Started on', input('startedOn', { value: today(), type: 'date' })),
            field('Collaboration', collabSelect(false))),
        field('Tags', tags),
        everyone.length > 0 && field('Teammates (you are added automatically)', picker));
    const detailed = el('details', 'more',
        el('summary', null, 'Detailed info: question, motivation, approach, resources, challenges (optional)'),
        ...detailFields());
    const suggest = suggestButton(() => title.value + '\n' + description.value, suggestion => {
        title.value = suggestion.title;
        description.value = suggestion.description || '';
        tags.suggest(suggestion.tags);
        more.open = true;
        if (fillDetails(detailed, suggestion)) {
            detailed.open = true;
        }
    });
    formDialog('New project', [
        field('Title', title),
        el('div', 'autofill-row', suggest),
        more,
        detailed,
    ], 'Create project', async values => {
        const project = await sendJson('POST', '/api/projects', {
            title: values.title, description: values.description, status: values.status,
            startedOn: values.startedOn || null, openForCollab: values.openForCollab === 'true',
            tags: splitTags(values.tags), memberIds: pickedIds(picker), details: detailsFrom(values),
        });
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
        el('div', 'field-row',
            field('Started on', input('startedOn', { value: project.startedOn, type: 'date', required: true })),
            field('Collaboration', collabSelect(project.openForCollab))),
        field('Tags', tagPicker('tags', project.tags)),
        picker && field('Members', picker),
        detailedInfo(project.details),
    ].filter(Boolean), 'Save', async values => {
        const changes = {
            title: values.title, description: values.description, startedOn: values.startedOn,
            openForCollab: values.openForCollab === 'true', tags: splitTags(values.tags),
            details: detailsFrom(values),
        };
        if (picker) {
            changes.memberIds = pickedIds(picker);
        }
        redraw(await sendJson('PATCH', '/api/projects/' + project.id, changes));
        toast('Project saved');
    });
}

// open straight away when there is something in it, so the person sees what they wrote
function detailedInfo(details) {
    const section = el('details', 'more',
        el('summary', null, 'Detailed info: question, motivation, approach, resources, challenges'),
        ...detailFields(details));
    section.open = hasDetails(details);
    return section;
}

function today() {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function formatDay(isoDate) {
    return new Date(isoDate + 'T00:00').toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
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
    // made once, so redrawing after a vote or a new link doesn't reload the comments or lose a half-typed one
    const discussion = discussionSection('/api/projects/' + project.id);
    const draw = () => {
        page.heading.textContent = project.title;
        document.title = project.title + ' · Raman Lab Tracker';
        page.description.replaceChildren(backLink(), statusChip(project.status),
            el('span', 'muted', ...(project.ownerName
                ? ['Owned by ', personLink(project.ownerId, project.ownerName), ' · '] : []),
                'started ' + relativeTime(project.createdAt)));
        page.actions.replaceChildren(voteButton(project, redraw),
            button(['edit', 'Edit'], 'button button-secondary', () => editDialog(project, redraw)));
        const about = project.description && el('div', 'card', markdown(project.description));
        const write = hasDetails(project.details) && el('div', 'card detail-parts', ...detailSections(project.details));
        container.replaceChildren(el('div', 'detail-layout',
            el('div', 'detail-main', about, write, composer(project, redraw),
                el('h2', 'section-title', 'Timeline'), timeline(project.timeline),
                el('h2', 'section-title', 'Discussion'), discussion),
            el('aside', 'card detail-side', ...sidePanel(project, redraw))));
    };
    draw();
}

/** A person's name that opens their page in the People tab. */
export function personLink(id, name) {
    if (id === null || id === undefined) {
        return el('span', null, name);
    }
    const link = el('a', 'person-link', name);
    link.href = '#people/' + id;
    return link;
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
    const base = '/api/projects/' + project.id;
    return [
        sideSection('Status', statusPicker(project, redraw)),
        sideSection('Started', el('p', 'side-text', formatDay(project.startedOn))),
        sideSection('Collaboration', el('p', 'side-text',
            project.openForCollab ? 'Open: others can ask to join' : 'Closed or solo')),
        sideSection('Members', project.members.length > 0
            ? el('ul', 'people', ...project.members.map(person =>
                el('li', 'person', avatar(person.name), personLink(person.id, person.name))))
            : el('p', 'muted side-empty', 'No members yet.')),
        sideSection('Tags', tagList(project.tags) || el('p', 'muted side-empty', 'No tags yet. Add some with Edit.')),
        sideSection('Links', linkList(project, base, redraw), linkForm(base, redraw)),
        sideSection('Papers', paperList(project, base, redraw), paperPicker(project, base, redraw)),
    ];
}

function voteButton(project, redraw) {
    const vote = button(['up', (project.votedByMe ? 'Upvoted' : 'Upvote') + ' · ' + project.voteCount],
        'button button-secondary', async () => {
            vote.disabled = true;
            try {
                redraw(await sendJson(project.votedByMe ? 'DELETE' : 'PUT', '/api/projects/' + project.id + '/vote'));
            } catch (problem) {
                toast(problem.message, 'error');
                vote.disabled = false;
            }
        });
    vote.setAttribute('aria-pressed', String(project.votedByMe));
    return vote;
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

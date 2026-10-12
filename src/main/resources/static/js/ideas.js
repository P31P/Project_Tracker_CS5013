// Ideas tab: capture an idea in seconds, and turn it into a project when it's ready.

import { getJson, sendJson } from './api.js';
import { button, el, emptyState, errorState, filterChips, input, relativeTime, skeleton, textarea, toast } from './ui.js';

export async function renderIdeas(container) {
    const list = el('div');
    container.replaceChildren(composer(() => load(list)), list);
    await load(list);
}

function composer(onSaved) {
    const title = input('title', { placeholder: 'Got an idea? Write it down…' });
    title.classList.add('input-large');
    const description = textarea('description', { placeholder: 'Any context, links or first thoughts (optional)' });
    const more = el('details', 'more', el('summary', null, 'Add details'), description);
    const save = el('button', 'button', 'Save idea');
    save.type = 'submit';
    const form = el('form', 'card composer', title, more, el('div', 'composer-actions', save));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        save.disabled = true;
        try {
            await sendJson('POST', '/api/ideas', { title: title.value, description: description.value });
            form.reset();
            more.open = false;
            toast('Idea saved');
            await onSaved();
        } catch (problem) {
            toast(problem.message, 'error');
        } finally {
            save.disabled = false;
            title.focus();
        }
    });
    return form;
}

async function load(list) {
    list.replaceChildren(skeleton());
    let ideas;
    try {
        ideas = await getJson('/api/ideas');
    } catch (problem) {
        list.replaceChildren(errorState('Could not load ideas. ' + problem.message));
        return;
    }
    if (ideas.length === 0) {
        list.replaceChildren(emptyState('No ideas yet',
            'Write the first one above. Half-formed is fine; you can turn it into a project later.'));
        return;
    }

    let filter = 'ALL';
    const open = ideas.filter(idea => idea.projectId === null);
    const draw = () => {
        const shown = filter === 'OPEN' ? open : filter === 'PROMOTED' ? ideas.filter(idea => idea.projectId !== null) : ideas;
        list.replaceChildren(
            filterChips([['ALL', 'All', ideas.length], ['OPEN', 'Open', open.length],
                ['PROMOTED', 'Became projects', ideas.length - open.length]], filter, key => { filter = key; draw(); }),
            shown.length > 0
                ? el('div', 'card-list', ...shown.map(ideaCard))
                : el('p', 'muted', 'Nothing here yet.'));
    };
    draw();
}

function ideaCard(idea) {
    return el('article', 'card idea',
        el('div', 'idea-body',
            el('h3', 'card-title', idea.title),
            idea.description && el('p', 'card-text', idea.description),
            el('p', 'card-meta', [idea.authorName, relativeTime(idea.createdAt)].filter(Boolean).join(' · '))),
        idea.projectId !== null ? projectLink(idea) : promoteButton(idea));
}

function projectLink(idea) {
    const link = el('a', 'status status-done', 'Became a project →');
    link.href = '#projects/' + idea.projectId;
    link.title = idea.projectTitle;
    return link;
}

function promoteButton(idea) {
    const promote = button('Turn into a project', 'button button-secondary', async () => {
        promote.disabled = true;
        try {
            const project = await sendJson('POST', '/api/ideas/' + idea.id + '/promote');
            toast('Project created from your idea');
            location.hash = 'projects/' + project.id;
        } catch (problem) {
            toast(problem.message, 'error');
            promote.disabled = false;
        }
    });
    return promote;
}

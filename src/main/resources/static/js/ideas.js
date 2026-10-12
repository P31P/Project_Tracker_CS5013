// Ideas tab: capture an idea in seconds, upvote the ones worth doing, and turn one into a project when it's ready.

import { getJson, sendJson } from './api.js';
import { icon } from './icons.js';
import {
    avatar, button, el, emptyState, errorState, filterChips, input, relativeTime, segmented, skeleton, splitTags,
    tagList, tagPill, textarea, toast,
} from './ui.js';

export async function renderIdeas(container) {
    const list = el('div');
    container.replaceChildren(composer(() => load(list)), list);
    await load(list);
}

function composer(onSaved) {
    const title = input('title', { placeholder: 'Got an idea? Write it down…' });
    title.classList.add('input-large');
    title.setAttribute('aria-label', 'Idea');
    const description = textarea('description', { placeholder: 'Any context, links or first thoughts (optional)' });
    description.setAttribute('aria-label', 'Details');
    const tags = input('tags', { placeholder: 'Tags, separated by commas (optional)' });
    tags.setAttribute('aria-label', 'Tags');
    const more = el('details', 'more', el('summary', null, 'Add details'), el('div', 'more-fields', description, tags));
    const save = el('button', 'button', icon('bulb', 16), 'Save idea');
    save.type = 'submit';
    const form = el('form', 'card composer composer-idea', title, more, el('div', 'composer-actions', save));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        save.disabled = true;
        try {
            await sendJson('POST', '/api/ideas',
                { title: title.value, description: description.value, tags: splitTags(tags.value) });
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
            'Write the first one above. Half-formed is fine; you can turn it into a project later.', null, 'bulb'));
        return;
    }

    let filter = 'ALL';
    let sort = 'NEW';
    let tag = null;
    const pickTag = picked => {
        tag = picked;
        draw();
    };
    const replace = updated => {
        ideas = ideas.map(idea => idea.id === updated.id ? updated : idea);
        draw();
    };
    const draw = () => {
        const tagged = tag ? ideas.filter(idea => idea.tags.includes(tag)) : ideas;
        const open = tagged.filter(idea => idea.projectId === null);
        let shown = filter === 'OPEN' ? open
            : filter === 'PROMOTED' ? tagged.filter(idea => idea.projectId !== null) : tagged;
        if (sort === 'TOP') {
            // sort is stable, so ideas with the same votes stay newest first
            shown = [...shown].sort((a, b) => b.voteCount - a.voteCount);
        }
        list.replaceChildren(...[
            el('div', 'toolbar',
                filterChips([['ALL', 'All', tagged.length], ['OPEN', 'Open', open.length],
                    ['PROMOTED', 'Became projects', tagged.length - open.length]], filter, key => { filter = key; draw(); }),
                segmented([['NEW', 'Newest'], ['TOP', 'Most votes', 'up']], sort, key => { sort = key; draw(); })),
            tag && el('div', 'active-filter', 'Showing ideas tagged ', tagPill(tag),
                button(['close', 'Clear'], 'link-button', () => pickTag(null))),
            shown.length > 0
                ? el('div', 'card-list', ...shown.map((idea, index) => ideaCard(idea, replace, pickTag, index)))
                : el('p', 'muted', 'Nothing here yet.'),
        ].filter(Boolean));
    };
    draw();
}

function ideaCard(idea, replace, pickTag, index) {
    const card = el('article', 'card idea',
        voteButton(idea, replace),
        el('div', 'idea-body',
            el('h3', 'card-title', idea.title),
            idea.description && el('p', 'card-text', idea.description),
            tagList(idea.tags, pickTag),
            el('p', 'card-meta card-byline', idea.authorName && avatar(idea.authorName),
                [idea.authorName, relativeTime(idea.createdAt)].filter(Boolean).join(' · '))),
        el('div', 'idea-action', idea.projectId !== null ? projectLink(idea) : promoteButton(idea)));
    card.style.setProperty('--i', Math.min(index, 10));
    return card;
}

function voteButton(idea, replace) {
    const vote = button(el('span', 'vote-inner', icon('up', 18), el('span', 'vote-count', String(idea.voteCount))),
        'vote', async () => {
            vote.disabled = true;
            try {
                replace(await sendJson(idea.votedByMe ? 'DELETE' : 'PUT', '/api/ideas/' + idea.id + '/vote'));
            } catch (problem) {
                toast(problem.message, 'error');
                vote.disabled = false;
            }
        });
    vote.setAttribute('aria-pressed', String(idea.votedByMe));
    vote.setAttribute('aria-label', (idea.votedByMe ? 'Remove your vote' : 'Upvote') + ', ' + idea.voteCount + ' votes');
    return vote;
}

function projectLink(idea) {
    const link = el('a', 'status status-done', 'Became a project →');
    link.href = '#projects/' + idea.projectId;
    link.title = idea.projectTitle;
    return link;
}

function promoteButton(idea) {
    const promote = button(['folder', 'Turn into a project'], 'button button-secondary', async () => {
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

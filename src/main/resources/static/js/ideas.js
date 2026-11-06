// Ideas tab: capture an idea in seconds, discuss and upvote the ones worth doing, and turn one into a project
// when it's ready.

import { getJson, sendJson } from './api.js';
import { suggestButton } from './autofill.js';
import { collabBadge, detailFields, detailSections, detailsFrom, fillDetails, hasDetails } from './details.js';
import { icon } from './icons.js';
import { markdown, markdownHint } from './markdown.js';
import { linkForm, linkList, paperList, paperPicker } from './references.js';
import { tagPicker } from './tags.js';
import {
    avatar, button, el, emptyState, errorState, filterChips, input, relativeTime, segmented, skeleton, tagList, tagPill,
    textarea, toast,
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
    // replaced with an empty picker after each save
    let tags = tagPicker('tags');
    const collab = el('input');
    collab.type = 'checkbox';
    collab.name = 'openForCollab';
    const more = el('details', 'more', el('summary', null, 'Add details'),
        el('div', 'more-fields', description, markdownHint(), tags,
            el('label', 'check-field', collab, 'Looking for people to work on this with'),
            ...detailFields()));
    const save = el('button', 'button', icon('bulb', 16), 'Save idea');
    save.type = 'submit';
    const suggest = suggestButton(() => title.value + '\n' + description.value, suggestion => {
        title.value = suggestion.title;
        description.value = suggestion.description || '';
        tags.suggest(suggestion.tags);
        fillDetails(more, suggestion);
        more.open = true;
    });
    const form = el('form', 'card composer composer-idea', title, more, el('div', 'composer-actions', suggest, save));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        save.disabled = true;
        try {
            await sendJson('POST', '/api/ideas', {
                title: title.value, description: description.value, tags: tags.tags(),
                openForCollab: collab.checked, details: detailsFrom(Object.fromEntries(new FormData(form))),
            });
            form.reset();
            const fresh = tagPicker('tags');
            tags.replaceWith(fresh);
            tags = fresh;
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
    // ids of ideas whose discussion is open, so it stays open when the list is redrawn
    const openDiscussions = new Set();
    // half-typed comments by idea id; every redraw rebuilds the comment boxes, so the text is kept here
    const drafts = new Map();
    const toggleDiscussion = id => {
        if (openDiscussions.has(id)) {
            openDiscussions.delete(id);
        } else {
            openDiscussions.add(id);
        }
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
                ? el('div', 'card-list', ...shown.map((idea, index) => ideaCard(idea, replace, pickTag, index, openDiscussions.has(idea.id), toggleDiscussion, drafts)))
                : el('p', 'muted', 'Nothing here yet.'),
        ].filter(Boolean));
    };
    draw();
}

function ideaCard(idea, replace, pickTag, index, discussionOpen, toggleDiscussion, drafts) {
    const card = el('article', 'card idea',
        voteButton(idea, replace),
        el('div', 'idea-body',
            el('h3', 'card-title', idea.title, collabBadge(idea.openForCollab)),
            idea.description && markdown(idea.description),
            hasDetails(idea.details) && el('details', 'idea-details',
                el('summary', null, 'More about this idea'), ...detailSections(idea.details)),
            references(idea),
            tagList(idea.tags, pickTag),
            el('p', 'card-meta card-byline', idea.authorName && avatar(idea.authorName),
                [idea.authorName, relativeTime(idea.createdAt)].filter(Boolean).join(' · '),
                discussionToggle(idea, discussionOpen, toggleDiscussion))),
        el('div', 'idea-action', idea.projectId !== null ? projectLink(idea) : promoteButton(idea)),
        discussionOpen && discussion(idea, replace, drafts));
    card.style.setProperty('--i', Math.min(index, 10));
    return card;
}

function discussionToggle(idea, open, toggleDiscussion) {
    const count = idea.commentCount;
    const label = count === 0 ? 'Discuss and add references' : count === 1 ? '1 comment' : count + ' comments';
    const toggle = button(['message', label], 'link-button discussion-toggle', () => toggleDiscussion(idea.id));
    toggle.setAttribute('aria-expanded', String(open));
    return toggle;
}

// the thread loads when the discussion is opened; posting redraws the card, which loads it again
function discussion(idea, replace, drafts) {
    const thread = el('ol', 'comments', el('li', 'muted comments-note', 'Loading the discussion…'));
    getJson('/api/ideas/' + idea.id + '/comments')
        .then(comments => thread.replaceChildren(...(comments.length > 0
            ? comments.map(commentItem)
            : [el('li', 'muted comments-note', 'No comments yet. Start the discussion.')])))
        .catch(problem => thread.replaceChildren(el('li', 'notice notice-error', 'Could not load the discussion. '
            + problem.message)));

    const body = textarea('body', { placeholder: 'Add to the discussion…', rows: 2 });
    body.setAttribute('aria-label', 'Comment');
    body.value = drafts.get(idea.id) || '';
    body.addEventListener('input', () => drafts.set(idea.id, body.value));
    const post = el('button', 'button button-small', icon('message', 16), 'Comment');
    post.type = 'submit';
    const form = el('form', 'comment-form', body, el('div', 'composer-actions', markdownHint(), post));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        post.disabled = true;
        try {
            await sendJson('POST', '/api/ideas/' + idea.id + '/comments', { body: body.value });
            toast('Comment posted');
            drafts.delete(idea.id);
            replace({ ...idea, commentCount: idea.commentCount + 1 });
        } catch (problem) {
            toast(problem.message, 'error');
            post.disabled = false;
        }
    });
    const base = '/api/ideas/' + idea.id;
    const editor = el('div', 'reference-editor',
        el('h4', 'side-title', 'Links'), linkList(idea, base, replace), linkForm(base, replace),
        el('h4', 'side-title', 'Papers'), paperList(idea, base, replace), paperPicker(idea, base, replace));
    return el('section', 'discussion', thread, form, editor);
}

// a short line of the idea's references on the card; they are edited under the discussion
function references(idea) {
    const items = [
        ...idea.links.filter(link => /^https?:\/\//i.test(link.url)).map(link => {
            const anchor = el('a', null, link.label);
            anchor.href = link.url;
            anchor.target = '_blank';
            anchor.rel = 'noopener noreferrer';
            return anchor;
        }),
        ...idea.papers.map(paper => {
            const anchor = el('a', null, paper.title);
            anchor.href = '#papers/' + paper.id;
            return anchor;
        }),
    ];
    if (items.length === 0) {
        return null;
    }
    return el('p', 'idea-references', icon('link', 14), ...items.flatMap((item, i) => i === 0 ? [item] : [' · ', item]));
}

function commentItem(comment) {
    const name = comment.authorName || 'Someone';
    return el('li', 'comment', avatar(name),
        el('div', 'comment-body',
            el('p', 'comment-meta', el('strong', null, name), ' · ' + relativeTime(comment.createdAt)),
            markdown(comment.body)));
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

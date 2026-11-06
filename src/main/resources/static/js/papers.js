// Papers tab: the list of papers shared in Slack (#papers) and one paper with a form to fix its details (#papers/<id>).
// Anyone can upvote or tag a paper; "read" marks are each person's own.
// Text from the API goes in as text nodes, never as HTML: titles come from outside websites.

import { getJson, sendJson } from './api.js';
import { icon } from './icons.js';
import {
    button, el, errorState, field, filterChips, formDialog, input, segmented, skeleton, splitTags, tagList, tagPill,
    textarea, toast,
} from './ui.js';

const REFRESH_MS = 10000;
const MAX_AUTHORS = 3;

const timers = new WeakMap();

const SOURCES = {
    REGISTRY: 'Crossref or arXiv',
    META_TAGS: "the publisher's web page",
    MODEL: 'an AI reading of the PDF, so please check it',
    MANUAL: 'entered by hand',
};

export async function renderPapers(container, page) {
    loadStylesheet();
    if (page && page.path) {
        return renderPaper(container, page, page.path);
    }
    container.replaceChildren(el('p', 'papers-note', 'Loading papers…'));
    // kept across the automatic refreshes, so a refresh doesn't undo the reader's choices
    await refresh(container, { filter: 'ALL', sort: 'NEW', tag: null });
}

async function refresh(container, view) {
    clearTimeout(timers.get(container));

    let papers;
    try {
        const response = await fetch('/api/papers', { headers: { Accept: 'application/json' } });
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        papers = await response.json();
    } catch (error) {
        container.replaceChildren(
            el('p', 'papers-note papers-error', 'Could not load papers. Try reloading the page.'));
        return;
    }

    if (papers.length === 0) {
        container.replaceChildren(el('p', 'papers-note',
            'No papers yet. Mention the bot in Slack with a paper link and it will show up here.'));
        return;
    }

    papers.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const replace = updated => {
        papers = papers.map(paper => paper.id === updated.id ? updated : paper);
        draw();
    };
    const pickTag = tag => {
        view.tag = tag;
        draw();
    };
    const draw = () => {
        const tagged = view.tag ? papers.filter(paper => paper.tags.includes(view.tag)) : papers;
        const unread = tagged.filter(paper => !paper.readByMe);
        let shown = view.filter === 'UNREAD' ? unread
            : view.filter === 'READ' ? tagged.filter(paper => paper.readByMe) : tagged;
        if (view.sort === 'TOP') {
            // sort is stable, so papers with the same votes stay newest first
            shown = [...shown].sort((a, b) => b.voteCount - a.voteCount);
        }
        container.replaceChildren(...[
            el('div', 'toolbar',
                filterChips([['ALL', 'All', tagged.length], ['UNREAD', 'Unread', unread.length],
                    ['READ', 'Read', tagged.length - unread.length]], view.filter, key => { view.filter = key; draw(); }),
                segmented([['NEW', 'Newest'], ['TOP', 'Most votes', 'up']], view.sort, key => { view.sort = key; draw(); })),
            view.tag && el('div', 'active-filter', 'Showing papers tagged ', tagPill(view.tag),
                button(['close', 'Clear'], 'link-button', () => pickTag(null))),
            shown.length > 0
                ? el('ul', 'papers', ...shown.map(paper => paperItem(paper, replace, pickTag)))
                : el('p', 'muted', 'Nothing here.'),
        ].filter(Boolean));
    };
    draw();

    // new papers are looked up within about 30 s, so check again while any are waiting
    if (papers.some(paper => paper.status === 'PENDING')) {
        timers.set(container, setTimeout(() => {
            if (container.isConnected) {
                refresh(container, view);
            }
        }, REFRESH_MS));
    }
}

function paperItem(paper, replace, pickTag) {
    const title = paper.title || paper.normalizedUrl;
    const heading = el('div', 'paper-head', link(paper.normalizedUrl, title, 'paper-title'));

    const badgeNode = statusBadge(paper);
    if (badgeNode) {
        heading.append(badgeNode);
    }

    const body = el('div', 'paper-body', heading);
    if (paper.authors && paper.authors.length > 0) {
        body.append(el('p', 'paper-authors', formatAuthors(paper.authors)));
    }
    const tags = tagList(paper.tags, pickTag);
    if (tags) {
        body.append(tags);
    }

    const details = el('p', 'paper-meta');
    const parts = [];
    if (paper.publishedOn) {
        parts.push(paper.publishedOn.slice(0, 4));
    }
    if (paper.sharedAt) {
        const who = paper.sharedBy ? 'Shared by ' + paper.sharedBy : 'Shared';
        parts.push(who + ' on ' + formatDate(paper.sharedAt));
    }
    if (parts.length > 0) {
        details.append(parts.join(' · '));
    }
    if (isWebLink(paper.slackPermalink)) {
        details.append(parts.length > 0 ? ' · ' : '', link(paper.slackPermalink, 'View in Slack'));
    }
    const open = el('a', null, 'Details');
    open.href = '#papers/' + paper.id;
    details.append(details.hasChildNodes() ? ' · ' : '', open, ' · ', readToggle(paper, replace, 'link-button'));
    body.append(details);
    return el('li', paper.readByMe ? 'paper paper-read' : 'paper', voteButton(paper, replace), body);
}

function voteButton(paper, replace) {
    const vote = button(el('span', 'vote-inner', icon('up', 18), el('span', 'vote-count', String(paper.voteCount))),
        'vote', () => send(vote, paper.votedByMe ? 'DELETE' : 'PUT', paper.id + '/vote', replace));
    vote.setAttribute('aria-pressed', String(paper.votedByMe));
    vote.setAttribute('aria-label', (paper.votedByMe ? 'Remove your vote' : 'Upvote') + ', ' + paper.voteCount + ' votes');
    return vote;
}

function readToggle(paper, replace, className) {
    const toggle = button(paper.readByMe ? ['check', 'Read'] : 'Mark as read', className,
        () => send(toggle, paper.readByMe ? 'DELETE' : 'PUT', paper.id + '/read', replace));
    toggle.setAttribute('aria-pressed', String(paper.readByMe));
    toggle.title = paper.readByMe ? 'Mark as unread' : 'Only you see what you have read';
    return toggle;
}

async function send(control, method, path, replace) {
    control.disabled = true;
    try {
        replace(await sendJson(method, '/api/papers/' + path));
    } catch (problem) {
        toast(problem.message, 'error');
        control.disabled = false;
    }
}

function statusBadge(paper) {
    if (paper.status === 'PENDING') {
        return badge('Looking up details…', 'pending');
    }
    if (paper.status === 'UNRESOLVED') {
        return badge('Details not found', 'unresolved');
    }
    return paper.source === 'MODEL' ? badge('Guessed by AI, please check', 'model') : null;
}

async function renderPaper(container, page, id) {
    page.heading.textContent = 'Paper';
    page.description.replaceChildren(backLink());
    container.replaceChildren(skeleton(2));

    let paper;
    try {
        paper = await getJson('/api/papers/' + encodeURIComponent(id));
    } catch (problem) {
        container.replaceChildren(errorState(problem.message));
        return;
    }
    // loaded once; fixing the details redraws the page but doesn't change the references
    const references = referencesSection(paper.id);
    const replace = updated => {
        paper = updated;
        draw();
    };
    const draw = () => {
        page.heading.textContent = paper.title || paper.normalizedUrl;
        document.title = (paper.title || 'Paper') + ' · Raman Lab Tracker';
        page.description.replaceChildren(...[backLink(), statusBadge(paper)].filter(Boolean));
        page.actions.replaceChildren(
            voteAction(paper, replace),
            readToggle(paper, replace, 'button button-secondary'),
            button(['tag', 'Edit tags'], 'button button-secondary', () => tagsDialog(paper, replace)),
            button(['edit', 'Fix details'], 'button button-secondary', () => fixDialog(paper, replace)));
        container.replaceChildren(el('div', 'card', el('dl', 'paper-facts', ...facts(paper))), references);
    };
    draw();
}

function referencesSection(paperId) {
    const body = el('p', 'muted', 'Loading references…');
    const section = el('section', 'paper-references', el('h2', 'section-title', 'References'), body);
    getJson('/api/papers/' + paperId + '/references')
        .then(references => body.replaceWith(references.length > 0
            ? el('ol', 'reference-list', ...references.map(referenceItem))
            : el('p', 'muted', 'References not available. They come from Crossref, so only papers it knows have them.')))
        .catch(problem => body.replaceWith(errorState('Could not load the references. ' + problem.message)));
    return section;
}

function referenceItem(reference) {
    const item = el('li', null, reference.citation);
    if (reference.doi) {
        item.append(' ', link('https://doi.org/' + reference.doi, 'DOI'));
    }
    return item;
}

function backLink() {
    const back = el('a', 'back-link', icon('back', 16), 'All papers');
    back.href = '#papers';
    return back;
}

function facts(paper) {
    const rows = [];
    const add = (label, value) => {
        if (value) {
            rows.push(el('dt', null, label), el('dd', null, value));
        }
    };
    add('Authors', paper.authors && paper.authors.length > 0 ? paper.authors.join(', ') : null);
    add('Published', paper.publishedOn);
    add('DOI', paper.doi);
    add('Tags', tagList(paper.tags));
    add('Paper', link(paper.normalizedUrl, paper.normalizedUrl));
    if (paper.sharedAt) {
        add('Shared', (paper.sharedBy ? 'by ' + paper.sharedBy + ' on ' : 'on ') + formatDate(paper.sharedAt));
    }
    add('Slack', isWebLink(paper.slackPermalink) ? link(paper.slackPermalink, 'View in Slack') : null);
    add('Details from', paper.source ? SOURCES[paper.source] || paper.source : null);
    return rows;
}

function voteAction(paper, replace) {
    const vote = button(['up', (paper.votedByMe ? 'Upvoted' : 'Upvote') + ' · ' + paper.voteCount],
        'button button-secondary', () => send(vote, paper.votedByMe ? 'DELETE' : 'PUT', paper.id + '/vote', replace));
    vote.setAttribute('aria-pressed', String(paper.votedByMe));
    return vote;
}

function tagsDialog(paper, onSaved) {
    formDialog('Edit tags', [
        field('Tags, separated by commas',
            input('tags', { value: paper.tags.join(', '), placeholder: 'genomics, machine learning' })),
    ], 'Save', async values => {
        const updated = await sendJson('PUT', '/api/papers/' + paper.id + '/tags', { tags: splitTags(values.tags) });
        toast('Tags saved');
        onSaved(updated);
    });
}

function fixDialog(paper, onSaved) {
    formDialog('Fix details', [
        field('Title', input('title', { value: paper.title || '', required: true })),
        field('Authors, one per line', textarea('authors', { value: (paper.authors || []).join('\n'), rows: 4 })),
        field('Published', input('publishedOn', { value: paper.publishedOn || '', type: 'date' })),
        field('DOI', input('doi', { value: paper.doi || '', placeholder: '10.1000/xyz123' })),
    ], 'Save', async values => {
        const updated = await sendJson('PATCH', '/api/papers/' + paper.id, {
            title: values.title,
            authors: values.authors.split('\n'),
            publishedOn: values.publishedOn || null,
            doi: values.doi,
        });
        toast('Details saved');
        onSaved(updated);
    });
}

function formatAuthors(authors) {
    if (authors.length <= MAX_AUTHORS + 1) {
        return authors.join(', ');
    }
    return authors.slice(0, MAX_AUTHORS).join(', ') + ' and ' + (authors.length - MAX_AUTHORS) + ' more';
}

function formatDate(isoDateTime) {
    return new Date(isoDateTime).toLocaleDateString(undefined,
        { day: 'numeric', month: 'short', year: 'numeric' });
}

function isWebLink(url) {
    return typeof url === 'string' && /^https?:\/\//i.test(url);
}

function link(url, text, className) {
    const a = el('a', className, text);
    if (isWebLink(url)) {
        a.href = url;
        a.target = '_blank';
        a.rel = 'noopener noreferrer';
    }
    return a;
}

function badge(text, kind) {
    return el('span', 'badge badge-' + kind, text);
}

function loadStylesheet() {
    if (document.getElementById('papers-css')) {
        return;
    }
    const stylesheet = document.createElement('link');
    stylesheet.id = 'papers-css';
    stylesheet.rel = 'stylesheet';
    stylesheet.href = new URL('../css/papers.css', import.meta.url).href;
    document.head.append(stylesheet);
}

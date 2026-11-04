// Papers tab: the list of papers shared in Slack (#papers) and one paper with a form to fix its details (#papers/<id>).
// Text from the API goes in as text nodes, never as HTML: titles come from outside websites.

import { getJson, sendJson } from './api.js';
import { icon } from './icons.js';
import { button, el, errorState, field, formDialog, input, skeleton, textarea, toast } from './ui.js';

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
    await refresh(container);
}

async function refresh(container) {
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
    container.replaceChildren(el('ul', 'papers', ...papers.map(paperItem)));

    // new papers are looked up within about 30 s, so check again while any are waiting
    if (papers.some(paper => paper.status === 'PENDING')) {
        timers.set(container, setTimeout(() => {
            if (container.isConnected) {
                refresh(container);
            }
        }, REFRESH_MS));
    }
}

function paperItem(paper) {
    const title = paper.title || paper.normalizedUrl;
    const heading = el('div', 'paper-head', link(paper.normalizedUrl, title, 'paper-title'));

    const badgeNode = statusBadge(paper);
    if (badgeNode) {
        heading.append(badgeNode);
    }

    const item = el('li', 'paper', heading);
    if (paper.authors && paper.authors.length > 0) {
        item.append(el('p', 'paper-authors', formatAuthors(paper.authors)));
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
    details.append(details.hasChildNodes() ? ' · ' : '', open);
    item.append(details);
    return item;
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
    const draw = () => {
        page.heading.textContent = paper.title || paper.normalizedUrl;
        document.title = (paper.title || 'Paper') + ' · Raman Lab Tracker';
        page.description.replaceChildren(...[backLink(), statusBadge(paper)].filter(Boolean));
        page.actions.replaceChildren(button(['edit', 'Fix details'], 'button button-secondary',
            () => fixDialog(paper, updated => {
                paper = updated;
                draw();
            })));
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
    add('Paper', link(paper.normalizedUrl, paper.normalizedUrl));
    if (paper.sharedAt) {
        add('Shared', (paper.sharedBy ? 'by ' + paper.sharedBy + ' on ' : 'on ') + formatDate(paper.sharedAt));
    }
    add('Slack', isWebLink(paper.slackPermalink) ? link(paper.slackPermalink, 'View in Slack') : null);
    add('Details from', paper.source ? SOURCES[paper.source] || paper.source : null);
    return rows;
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

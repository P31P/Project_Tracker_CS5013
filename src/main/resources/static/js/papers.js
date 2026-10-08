// Papers tab: a read-only list of the papers shared in Slack.
// Text from the API goes in as text nodes, never as HTML: titles come from outside websites.

const REFRESH_MS = 10000;
const MAX_AUTHORS = 3;

const timers = new WeakMap();

export async function renderPapers(container) {
    loadStylesheet();
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

    if (paper.status === 'PENDING') {
        heading.append(badge('Looking up details…', 'pending'));
    } else if (paper.status === 'UNRESOLVED') {
        heading.append(badge('Details not found', 'unresolved'));
    } else if (paper.source === 'MODEL') {
        heading.append(badge('Guessed by AI, please check', 'model'));
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
    if (details.hasChildNodes()) {
        item.append(details);
    }
    return item;
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

function el(tag, className, ...children) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    node.append(...children);
    return node;
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

// Search: the box in the header, and the results page (#search/<text>) with results grouped by type.

import { getJson } from './api.js';
import { icon } from './icons.js';
import { el, emptyState, errorState, filterChips, skeleton } from './ui.js';

const KINDS = [['PROJECT', 'Projects', 'folder'], ['IDEA', 'Ideas', 'bulb'], ['PAPER', 'Papers', 'paper']];
const MAX_RESULTS = 50;

/** Sends what is typed in the header box to the results page. */
export function initSearchBar(form) {
    form.prepend(el('span', 'search-icon', icon('search', 16)));
    form.addEventListener('submit', event => {
        event.preventDefault();
        const text = form.elements.q.value.trim();
        location.hash = text ? 'search/' + encodeURIComponent(text) : 'search';
    });
}

export async function renderSearch(container, page) {
    const text = queryFrom(page.path);
    const box = document.getElementById('search-input');
    if (box) {
        box.value = text;
    }
    page.description.textContent = text ? 'Results for “' + text + '”' : '';

    if (!text) {
        container.replaceChildren(emptyState('Search the lab',
            'Find projects, ideas and papers by title, description, tags or authors. Put “quotes” around an exact phrase.',
            null, 'search'));
        return;
    }

    container.replaceChildren(skeleton());
    let results;
    try {
        results = await getJson('/api/search?q=' + encodeURIComponent(text));
    } catch (problem) {
        container.replaceChildren(errorState('Could not search. ' + problem.message));
        return;
    }
    if (results.length === 0) {
        container.replaceChildren(emptyState('No matches',
            'Nothing found for “' + text + '”. Try fewer or different words.', null, 'search'));
        return;
    }

    let kind = 'ALL';
    const draw = () => {
        const counts = KINDS.map(([key]) => results.filter(result => result.type === key).length);
        const chips = filterChips([['ALL', 'All', results.length],
            ...KINDS.map(([key, label], i) => [key, label, counts[i]])], kind, picked => {
            kind = picked;
            draw();
        });
        const groups = KINDS.filter(([key]) => kind === 'ALL' || kind === key).map(([key, label, iconName]) => {
            const matches = results.filter(result => result.type === key);
            return matches.length > 0 && el('section', 'search-group',
                el('h2', 'section-title', label),
                el('ul', 'search-results', ...matches.map(result => resultItem(result, iconName))));
        });
        const note = results.length >= MAX_RESULTS && el('p', 'muted', 'Showing the best ' + MAX_RESULTS + ' matches.');
        container.replaceChildren(chips, ...[...groups, note].filter(Boolean));
    };
    draw();
}

// the hash keeps the text percent-encoded; a hand-edited address may not decode
function queryFrom(path) {
    try {
        return decodeURIComponent(path).trim();
    } catch (error) {
        return path.trim();
    }
}

// ideas have no page of their own, so they open the Ideas tab
function addressOf(result) {
    return result.type === 'IDEA' ? '#ideas' : '#' + result.type.toLowerCase() + 's/' + result.id;
}

function resultItem(result, iconName) {
    const row = el('a', 'card search-result', el('span', 'search-result-icon', icon(iconName, 18)),
        el('div', 'search-result-body',
            el('p', 'search-result-title', result.title),
            result.detail && el('p', 'search-result-detail clamp', result.detail)));
    row.href = addressOf(result);
    return row;
}

// Search tab: a search box and filters (tags, person, dates) at the top, results grouped by type below.
// The search lives in the address (#search/q=flux&tag=genomics&person=3&from=2026-01-01), so it can be bookmarked.

import { getJson } from './api.js';
import { icon } from './icons.js';
import { plainText } from './markdown.js';
import {
    button, el, emptyState, errorState, field, filterChips, input, relativeTime, select, skeleton, splitTags, tagList,
} from './ui.js';

const KINDS = [['PROJECT', 'Projects', 'folder'], ['IDEA', 'Ideas', 'bulb'], ['PAPER', 'Papers', 'paper']];
const MAX_RESULTS = 50;

/** Pressing "/" anywhere outside a text box opens the Search tab, or jumps to its box. */
export function initSearchShortcut() {
    document.addEventListener('keydown', event => {
        const typing = event.target.closest && event.target.closest('input, textarea, select, [contenteditable]');
        if (event.key !== '/' || typing || event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }
        event.preventDefault();
        const box = document.getElementById('search-input');
        if (box) {
            box.focus();
        } else {
            location.hash = 'search';
        }
    });
}

export async function renderSearch(container, page) {
    const search = searchFrom(page.path);
    const filtered = search.tags.length > 0 || search.person || search.from || search.to;
    const form = searchForm(search, filtered);
    const results = el('div');
    container.replaceChildren(form, results);
    if (search.text) {
        page.description.textContent = 'Results for “' + search.text + '”';
    }

    const people = getJson('/api/people').catch(() => []);
    fillPeople(form.elements.person, people, search.person);

    if (!search.text && !filtered) {
        results.replaceChildren(emptyState('Search the lab',
            'Find projects, ideas and papers by title, description, tags or authors. Put “quotes” around an exact '
            + 'phrase. Or leave the box empty and pick tags, a person or dates to list everything that matches.',
            null, 'search'));
        form.elements.q.focus();
        return;
    }

    results.replaceChildren(skeleton());
    let found;
    try {
        found = await getJson('/api/search?' + apiParams(search));
    } catch (problem) {
        results.replaceChildren(errorState('Could not search. ' + problem.message));
        return;
    }
    if (found.length === 0) {
        results.replaceChildren(emptyState('No matches',
            'Nothing found. Try fewer words, or loosen the filters.', null, 'search'));
        return;
    }

    // the type chips only filter what is already loaded, so they update the address without a new search
    let kind = search.type;
    const draw = () => {
        const counts = KINDS.map(([key]) => found.filter(result => result.type === key).length);
        const chips = filterChips([['ALL', 'All', found.length],
            ...KINDS.map(([key, label], i) => [key, label, counts[i]])], kind, picked => {
            kind = picked;
            history.replaceState(null, '', '#search/' + hashParams({ ...search, type: kind }));
            draw();
        });
        const groups = KINDS.filter(([key]) => kind === 'ALL' || kind === key).map(([key, label, iconName]) => {
            const matches = found.filter(result => result.type === key);
            return matches.length > 0 && el('section', 'search-group',
                el('h2', 'section-title', label),
                el('ul', 'search-results', ...matches.map(result => resultItem(result, iconName, search))));
        });
        const note = found.length >= MAX_RESULTS && el('p', 'muted', 'Showing the best ' + MAX_RESULTS + ' matches.');
        results.replaceChildren(chips, ...[...groups, note].filter(Boolean));
    };
    draw();
}

function searchForm(search, filtered) {
    const box = input('q', { value: search.text, placeholder: 'Search projects, ideas and papers' });
    box.id = 'search-input';
    box.type = 'search';
    box.setAttribute('aria-label', 'Search projects, ideas and papers');

    const person = select('person', [['', 'Anyone']], '');
    const filters = el('div', 'search-filters',
        field('Tags', input('tags', { value: search.tags.join(', '), placeholder: 'genomics, machine learning' })),
        field('Person', person),
        field('Added from', input('from', { value: search.from, type: 'date' })),
        field('Added to', input('to', { value: search.to, type: 'date' })),
        filtered && button(['close', 'Clear filters'], 'link-button search-clear', () => {
            location.hash = 'search/' + hashParams({ ...search, tags: [], person: '', from: '', to: '' });
        }));

    const form = el('form', 'search-form',
        el('div', 'search-bar', el('label', 'search-box', el('span', 'search-icon', icon('search', 16)), box),
            el('button', 'button', 'Search')),
        filters);
    form.setAttribute('role', 'search');
    form.addEventListener('submit', event => {
        event.preventDefault();
        const values = form.elements;
        location.hash = 'search/' + hashParams({
            text: values.q.value.trim(), type: search.type, tags: splitTags(values.tags.value),
            person: values.person.value, from: values.from.value, to: values.to.value,
        });
    });
    // a changed filter searches right away; the text box waits for Enter or the button
    filters.addEventListener('change', () => form.requestSubmit());
    return form;
}

async function fillPeople(control, people, current) {
    for (const person of await people) {
        const option = el('option', null, person.name);
        option.value = String(person.id);
        option.selected = String(person.id) === current;
        control.append(option);
    }
}

// "q=flux&tag=genomics" is a search with filters; anything else is an older #search/<text> link
function searchFrom(path) {
    const params = path.includes('=') ? new URLSearchParams(path) : new URLSearchParams({ q: decoded(path) });
    return {
        text: (params.get('q') || '').trim(),
        type: KINDS.some(([key]) => key === params.get('type')) ? params.get('type') : 'ALL',
        tags: params.getAll('tag'),
        person: params.get('person') || '',
        from: params.get('from') || '',
        to: params.get('to') || '',
    };
}

// a hand-edited address may not decode
function decoded(path) {
    try {
        return decodeURIComponent(path);
    } catch (error) {
        return path;
    }
}

function hashParams(search) {
    const params = new URLSearchParams();
    if (search.text) {
        params.set('q', search.text);
    }
    if (search.type && search.type !== 'ALL') {
        params.set('type', search.type);
    }
    search.tags.forEach(tag => params.append('tag', tag));
    for (const key of ['person', 'from', 'to']) {
        if (search[key]) {
            params.set(key, search[key]);
        }
    }
    return params.toString();
}

// the type isn't sent: the chips filter the loaded results, so their counts stay visible
function apiParams(search) {
    return hashParams({ ...search, type: 'ALL' });
}

// ideas have no page of their own, so they open the Ideas tab
function addressOf(result) {
    return result.type === 'IDEA' ? '#ideas' : '#' + result.type.toLowerCase() + 's/' + result.id;
}

function resultItem(result, iconName, search) {
    // a tag sits outside the link, so clicking it narrows the search instead of opening the result
    const addTag = tag => {
        if (!search.tags.includes(tag)) {
            location.hash = 'search/' + hashParams({ ...search, tags: [...search.tags, tag] });
        }
    };
    const row = el('a', 'search-result-link',
        el('p', 'search-result-title', result.title),
        result.detail && el('p', 'search-result-detail clamp', plainText(result.detail)));
    row.href = addressOf(result);
    return el('li', 'card search-result', el('span', 'search-result-icon', icon(iconName, 18)),
        el('div', 'search-result-body', row,
            el('p', 'search-result-meta', 'Added ' + relativeTime(result.createdAt)),
            tagList(result.tags, addTag)));
}

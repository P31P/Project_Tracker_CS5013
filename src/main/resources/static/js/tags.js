// The tag picker: the lab's built-in tags as chips in their groups, other tags already in use, and a box for a
// new tag. Tags are stored lowercased; a built-in tag is shown by its label, such as "Machine Learning".

import { getJson } from './api.js';
import { button, el } from './ui.js';

const MAX_IN_USE = 15;

let catalog;

/** Loads the built-in tags and the custom tags in use, once per page load. */
export function loadTagCatalog() {
    if (!catalog) {
        catalog = getJson('/api/tags').catch(() => {
            catalog = null;
            return { groups: [], custom: [] };
        });
    }
    return catalog;
}

// the same tidying the server does, so a typed "#Genomics" and the built-in "genomics" are one tag
function tidy(tag) {
    return tag.trim().replace(/^#/, '').toLowerCase();
}

/**
 * A tag picker for a form. It keeps a hidden input called `name` up to date (tags joined with ", "), so the
 * form's values carry the tags like a text box would. picker.tags() returns them as a list. picker.suggest(tags)
 * adds tags from Suggest and takes back the ones an earlier suggestion added that the person hasn't touched.
 */
export function tagPicker(name, selected = []) {
    const chosen = new Set(selected.map(tidy).filter(Boolean));
    let suggested = new Set();
    let data = { groups: [], custom: [] };

    const hidden = el('input');
    hidden.type = 'hidden';
    hidden.name = name;
    const groups = el('div', 'tag-picker-groups', el('p', 'muted', 'Loading tags…'));
    const typed = el('input', 'input tag-picker-input');
    typed.placeholder = 'Add your own tag and press Enter';
    typed.autocomplete = 'off';
    typed.setAttribute('aria-label', 'Add your own tag');

    const chip = (tag, label) => {
        const node = button(label, 'tag-chip', () => {
            if (chosen.has(tag)) {
                chosen.delete(tag);
            } else {
                chosen.add(tag);
            }
            // once the person touches a suggested tag it is theirs, so a new suggestion leaves it alone
            suggested.delete(tag);
            draw();
        });
        node.setAttribute('aria-pressed', String(chosen.has(tag)));
        return node;
    };
    const draw = () => {
        const builtIn = new Set(data.groups.flatMap(group => group.tags.map(tag => tag.name)));
        const own = [...chosen].filter(tag => !builtIn.has(tag));
        const inUse = data.custom.filter(tag => !chosen.has(tag)).slice(0, MAX_IN_USE);
        const others = [...own, ...inUse];
        groups.replaceChildren(...data.groups.map(group => el('section', 'tag-group',
            el('h4', 'tag-group-title', group.label),
            el('div', 'tag-chips', ...group.tags.map(tag => chip(tag.name, tag.label))))),
        ...(others.length > 0 ? [el('section', 'tag-group', el('h4', 'tag-group-title', 'Other tags'),
            el('div', 'tag-chips', ...others.map(tag => chip(tag, tag))))] : []));
        hidden.value = [...chosen].join(', ');
    };
    const addTyped = () => {
        typed.value.split(',').map(tidy).filter(Boolean).forEach(tag => {
            chosen.add(tag);
            suggested.delete(tag);
        });
        typed.value = '';
        draw();
    };
    typed.addEventListener('keydown', event => {
        // Enter adds the tag instead of sending the form
        if (event.key === 'Enter' || event.key === ',') {
            event.preventDefault();
            addTyped();
        }
    });
    typed.addEventListener('blur', addTyped);

    const picker = el('div', 'tag-picker', groups, typed, hidden);
    picker.tags = () => [...chosen];
    picker.suggest = tags => {
        suggested.forEach(tag => chosen.delete(tag));
        suggested = new Set(tags.map(tidy).filter(tag => tag && !chosen.has(tag)));
        suggested.forEach(tag => chosen.add(tag));
        draw();
    };
    draw();
    loadTagCatalog().then(loaded => {
        data = loaded;
        draw();
    });
    return picker;
}

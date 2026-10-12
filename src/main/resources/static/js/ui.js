// Small DOM helpers shared by the shell and the tabs. Text always goes in as text, never as HTML.

import { icon } from './icons.js';

export function el(tag, className, ...children) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    node.append(...children.filter(child => child !== null && child !== undefined && child !== false));
    return node;
}

/** A button; label can be text, an icon name with text (['plus', 'New project']) or any node. */
export function button(label, className, onClick) {
    const node = Array.isArray(label) ? el('button', className, icon(label[0]), label[1]) : el('button', className, label);
    node.type = 'button';
    node.addEventListener('click', onClick);
    return node;
}

export function initials(name) {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(word => word[0].toUpperCase()).join('');
}

/** The same text always gets the same hue, so a person or a tag keeps its colour everywhere. */
export function hueOf(text) {
    let hash = 0;
    for (const char of text) {
        hash = (hash * 31 + char.codePointAt(0)) % 360;
    }
    return hash;
}

export function avatar(name, className = 'avatar-small') {
    const node = el('span', 'avatar ' + className, initials(name));
    node.style.setProperty('--hue', hueOf(name));
    node.title = name;
    return node;
}

/** Overlapping avatars, with "+2" when there are more than fit. */
export function avatarStack(people, max = 4) {
    const shown = people.slice(0, max).map(person => avatar(person.name));
    const extra = people.length > max && el('span', 'avatar avatar-small avatar-more', '+' + (people.length - max));
    const stack = el('span', 'avatar-stack', ...shown, extra);
    stack.setAttribute('aria-label', people.map(person => person.name).join(', '));
    return stack;
}

export function tagPill(tag, onClick) {
    const pill = onClick ? button('#' + tag, 'tag', onClick) : el('span', 'tag', '#' + tag);
    pill.style.setProperty('--hue', hueOf(tag));
    return pill;
}

export function tagList(tags, onClick) {
    return tags.length > 0 && el('div', 'tag-list', ...tags.map(tag => tagPill(tag, onClick && (() => onClick(tag)))));
}

/** "ml, Vision" from a text box becomes ["ml", "Vision"]; the server tidies case and duplicates. */
export function splitTags(text) {
    return (text || '').split(',').map(tag => tag.trim()).filter(Boolean);
}

export function relativeTime(isoDateTime) {
    const seconds = Math.round((new Date(isoDateTime) - Date.now()) / 1000);
    const units = [['year', 31536000], ['month', 2592000], ['week', 604800], ['day', 86400], ['hour', 3600], ['minute', 60]];
    const format = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
    for (const [unit, size] of units) {
        if (Math.abs(seconds) >= size) {
            return format.format(Math.round(seconds / size), unit);
        }
    }
    return 'just now';
}

export function toast(message, kind = 'success') {
    const note = el('div', 'toast toast-' + kind, message);
    document.getElementById('toasts').append(note);
    setTimeout(() => note.remove(), 4000);
}

export function skeleton(count = 3) {
    return el('div', 'skeleton-list', ...Array.from({ length: count }, () => el('div', 'skeleton-card')));
}

export function emptyState(title, text, action, iconName = 'plus') {
    return el('div', 'empty', el('div', 'empty-mark', icon(iconName, 22)), el('h3', 'empty-title', title),
        el('p', 'empty-text', text), action);
}

export function errorState(message) {
    return el('p', 'notice notice-error', message);
}

/** Filter chips such as "All 12 · Active 5"; calls onPick with the chosen key. */
export function filterChips(options, current, onPick) {
    return el('div', 'filters', ...options.map(([key, label, count]) => {
        const chip = button(label, 'filter-chip', () => onPick(key));
        chip.append(el('span', 'filter-count', String(count)));
        chip.setAttribute('aria-pressed', String(key === current));
        return chip;
    }));
}

export function field(label, control) {
    return el('label', 'field', el('span', 'field-label', label), control);
}

export function input(name, { placeholder = '', value = '', required = false, type = 'text' } = {}) {
    const node = el('input', 'input');
    node.type = type;
    node.name = name;
    node.placeholder = placeholder;
    node.value = value;
    node.required = required;
    node.autocomplete = 'off';
    return node;
}

export function textarea(name, { placeholder = '', value = '', rows = 3 } = {}) {
    const node = el('textarea', 'input');
    node.name = name;
    node.placeholder = placeholder;
    node.value = value;
    node.rows = rows;
    return node;
}

export function select(name, options, value) {
    const node = el('select', 'input', ...options.map(([key, label]) => {
        const option = el('option', null, label);
        option.value = key;
        option.selected = key === value;
        return option;
    }));
    node.name = name;
    return node;
}

/** Two or more buttons where one is pressed, e.g. List / Board. options are [key, label, iconName?]. */
export function segmented(options, current, onPick) {
    const group = el('div', 'segmented', ...options.map(([key, label, iconName]) => {
        const option = button(iconName ? [iconName, label] : label, 'segment', () => onPick(key));
        option.setAttribute('aria-pressed', String(key === current));
        return option;
    }));
    group.setAttribute('role', 'group');
    return group;
}

/** A modal form. onSubmit gets the form's values; if it throws, the message is shown and the dialog stays open. */
export function formDialog(title, fields, submitLabel, onSubmit) {
    const error = el('p', 'form-error');
    error.hidden = true;
    const submit = el('button', 'button', submitLabel);
    submit.type = 'submit';
    const form = el('form', 'dialog-form', el('h2', 'dialog-title', title), ...fields, error,
        el('div', 'dialog-actions', button('Cancel', 'button button-secondary', () => dialog.close()), submit));
    const dialog = el('dialog', 'dialog', form);
    dialog.addEventListener('close', () => dialog.remove());
    form.addEventListener('submit', async event => {
        event.preventDefault();
        submit.disabled = true;
        error.hidden = true;
        try {
            await onSubmit(Object.fromEntries(new FormData(form)));
            dialog.close();
        } catch (problem) {
            error.textContent = problem.message;
            error.hidden = false;
        } finally {
            submit.disabled = false;
        }
    });
    document.body.append(dialog);
    dialog.showModal();
}

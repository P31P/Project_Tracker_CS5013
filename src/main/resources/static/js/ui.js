// Small DOM helpers shared by the shell and the tabs. Text always goes in as text, never as HTML.

export function el(tag, className, ...children) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    node.append(...children.filter(child => child !== null && child !== undefined && child !== false));
    return node;
}

export function button(label, className, onClick) {
    const node = el('button', className, label);
    node.type = 'button';
    node.addEventListener('click', onClick);
    return node;
}

export function initials(name) {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(word => word[0].toUpperCase()).join('');
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

export function emptyState(title, text, action) {
    return el('div', 'empty', el('div', 'empty-mark', '+'), el('h3', 'empty-title', title), el('p', 'empty-text', text), action);
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

export function input(name, { placeholder = '', value = '', required = false } = {}) {
    const node = el('input', 'input');
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

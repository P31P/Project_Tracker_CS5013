// Small DOM helpers shared by the shell and the tabs. Text always goes in as text, never as HTML.

export function el(tag, className, ...children) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    node.append(...children.filter(child => child !== null && child !== undefined));
    return node;
}

export function initials(name) {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(word => word[0].toUpperCase()).join('');
}

export function toast(message, kind = 'success') {
    const note = el('div', 'toast toast-' + kind, message);
    document.getElementById('toasts').append(note);
    setTimeout(() => note.remove(), 4000);
}

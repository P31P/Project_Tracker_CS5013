// Light, dark or the computer's setting. The choice is kept in this browser; the <head> of each page applies it
// before drawing, and app.css switches colours on data-theme.

import { icon } from './icons.js';
import { button, el } from './ui.js';

const KEY = 'theme';
const CHOICES = [['system', 'System', 'monitor'], ['light', 'Light', 'sun'], ['dark', 'Dark', 'moon']];

function savedTheme() {
    try {
        return localStorage.getItem(KEY) || 'system';
    } catch {
        return 'system';
    }
}

function applyTheme(theme) {
    if (theme === 'system') {
        delete document.documentElement.dataset.theme;
    } else {
        document.documentElement.dataset.theme = theme;
    }
    try {
        if (theme === 'system') {
            localStorage.removeItem(KEY);
        } else {
            localStorage.setItem(KEY, theme);
        }
    } catch {
        // the theme still changes, it just isn't remembered
    }
}

/** Fills the container with a small System / Light / Dark switch: icons only, each named for screen readers. */
export function renderThemePicker(container) {
    const current = savedTheme();
    const group = el('div', 'segmented theme-switch', ...CHOICES.map(([theme, label, iconName]) => {
        const option = button(icon(iconName, 15), 'segment', () => {
            applyTheme(theme);
            // update the buttons in place rather than redrawing them, so keyboard focus stays on this one
            for (const other of group.children) {
                other.setAttribute('aria-pressed', String(other === option));
            }
        });
        option.setAttribute('aria-pressed', String(theme === current));
        option.setAttribute('aria-label', label + ' theme');
        option.title = label;
        return option;
    }));
    group.setAttribute('role', 'group');
    group.setAttribute('aria-label', 'Theme');
    container.replaceChildren(group);
}

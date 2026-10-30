// Light, dark or the computer's setting. The choice is kept in this browser; the <head> of each page applies it
// before drawing, and app.css switches colours on data-theme.

import { segmented } from './ui.js';

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

/** Fills the container with a System / Light / Dark switch. */
export function renderThemePicker(container) {
    container.replaceChildren(segmented(CHOICES, savedTheme(), theme => {
        applyTheme(theme);
        renderThemePicker(container);
    }));
}

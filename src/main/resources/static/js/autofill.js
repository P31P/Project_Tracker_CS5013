// Auto-fill: a "Suggest" button that turns a rough note into a title, description and tags.
// It only fills the form; nothing is saved until the person checks it and saves.

import { getJson, sendJson } from './api.js';
import { button, splitTags, toast } from './ui.js';

let enabled;

// asked once per page load; if the server can't say, the button stays hidden
function autoFillEnabled() {
    if (!enabled) {
        enabled = getJson('/api/autofill').then(status => status.enabled).catch(() => false);
    }
    return enabled;
}

/**
 * A Suggest button, hidden unless auto-fill is on. readNote() returns what the person has written;
 * fill(suggestion) puts { title, description, tags } into the form.
 */
export function suggestButton(readNote, fill) {
    const suggest = button(['sparkles', 'Suggest'], 'button button-secondary button-small', async () => {
        const note = readNote().trim();
        if (!note) {
            toast('Write a few words first, then ask for a suggestion', 'error');
            return;
        }
        const label = suggest.lastChild;
        suggest.disabled = true;
        label.textContent = 'Thinking…';
        try {
            fill(await sendJson('POST', '/api/autofill', { note }));
            toast('Suggestion filled in. Check it before saving.');
        } catch (problem) {
            toast(problem.message, 'error');
        } finally {
            suggest.disabled = false;
            label.textContent = 'Suggest';
        }
    });
    suggest.title = 'Fill in a title, description and tags from what you wrote';
    suggest.hidden = true;
    autoFillEnabled().then(on => {
        suggest.hidden = !on;
    });
    return suggest;
}

/**
 * The tags field's value after a suggestion: the tags the person typed, then the suggested ones not already there.
 * Tags left from an earlier suggestion are replaced, so pressing Suggest twice doesn't pile them up.
 */
export function mergeSuggestedTags(field, suggested) {
    // the field's dataset remembers the last value Suggest put there and what the person had typed before it
    if (field.value !== (field.dataset.suggested || '')) {
        field.dataset.typed = field.value;
    }
    const typed = splitTags(field.dataset.typed);
    const seen = new Set(typed.map(tag => tag.replace(/^#/, '').toLowerCase()));
    const value = [...typed, ...suggested.filter(tag => !seen.has(tag))].join(', ');
    field.dataset.suggested = value;
    return value;
}

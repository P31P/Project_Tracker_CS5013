// Auto-fill: a "Suggest" button that turns a rough note into a title, description, tags and the detailed info.
// It only fills the form; nothing is saved until the person checks it and saves.

import { getJson, sendJson } from './api.js';
import { button, toast } from './ui.js';

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
 * fill(suggestion) puts { title, description, tags, coreQuestion, motivation, approach, resources, challenges }
 * into the form.
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
    suggest.title = 'Fill in a title, description, tags and the detailed info from what you wrote';
    suggest.hidden = true;
    autoFillEnabled().then(on => {
        suggest.hidden = !on;
    });
    return suggest;
}

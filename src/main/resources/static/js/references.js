// References on a project or an idea: web links, and papers from our own archive. `base` is the item's API path,
// such as "/api/projects/12"; every change answers with the whole item, which goes to redraw().

import { getJson, sendJson } from './api.js';
import { icon } from './icons.js';
import { button, el, input, toast } from './ui.js';

const PAPER_RESULTS = 5;

async function change(method, path, body, redraw, message) {
    try {
        redraw(await sendJson(method, path, body));
        toast(message);
    } catch (problem) {
        toast(problem.message, 'error');
    }
}

export function linkList(item, base, redraw) {
    if (item.links.length === 0) {
        return el('p', 'muted side-empty', 'Repos, docs, slides or datasets.');
    }
    return el('ul', 'links', ...item.links.map(link => {
        const anchor = el('a', 'link-item', icon('external', 14), el('span', 'link-label', link.label));
        // the server only stores http(s) links; checked again so nothing else ever becomes a href
        if (/^https?:\/\//i.test(link.url)) {
            anchor.href = link.url;
        }
        anchor.target = '_blank';
        anchor.rel = 'noopener noreferrer';
        anchor.title = link.url;
        const remove = button(icon('close', 14), 'icon-button',
            () => change('DELETE', base + '/links/' + link.id, undefined, redraw, 'Link removed'));
        remove.setAttribute('aria-label', 'Remove ' + link.label);
        return el('li', 'link-row', anchor, remove);
    }));
}

export function linkForm(base, redraw) {
    const url = input('url', { placeholder: 'Paste a link', required: true });
    url.setAttribute('aria-label', 'Link URL');
    const label = input('label', { placeholder: 'Name (optional)' });
    label.setAttribute('aria-label', 'Link name');
    const add = el('button', 'button button-secondary button-small', icon('plus', 16), 'Add link');
    add.type = 'submit';
    const form = el('form', 'link-form', url, label, add);
    form.addEventListener('submit', async event => {
        event.preventDefault();
        add.disabled = true;
        // "github.com/lab/repo" is taken to mean https://github.com/lab/repo
        const address = /^[a-z]+:/i.test(url.value.trim()) ? url.value.trim() : 'https://' + url.value.trim();
        await change('POST', base + '/links', { label: label.value, url: address }, redraw, 'Link added');
        add.disabled = false;
    });
    return form;
}

export function paperList(item, base, redraw) {
    if (item.papers.length === 0) {
        return el('p', 'muted side-empty', 'Papers from the lab’s archive.');
    }
    return el('ul', 'links', ...item.papers.map(paper => {
        const anchor = el('a', 'link-item', icon('paper', 14), el('span', 'link-label', paper.title));
        anchor.href = '#papers/' + paper.id;
        const remove = button(icon('close', 14), 'icon-button',
            () => change('DELETE', base + '/papers/' + paper.id, undefined, redraw, 'Paper removed'));
        remove.setAttribute('aria-label', 'Remove ' + paper.title);
        return el('li', 'link-row', anchor, remove);
    }));
}

/** A search box over the paper archive; picking a result adds it to the references. */
export function paperPicker(item, base, redraw) {
    const box = input('', { placeholder: 'Find a paper in the archive' });
    box.type = 'search';
    box.setAttribute('aria-label', 'Find a paper in the archive');
    const results = el('ul', 'paper-results');
    let timer;
    let asked = 0;
    box.addEventListener('input', () => {
        clearTimeout(timer);
        const text = box.value.trim();
        if (!text) {
            results.replaceChildren();
            return;
        }
        // wait for a pause in typing, and ignore answers to older searches
        timer = setTimeout(async () => {
            const ask = ++asked;
            let found;
            try {
                found = await getJson('/api/search?type=PAPER&q=' + encodeURIComponent(text));
            } catch (problem) {
                found = [];
            }
            if (ask !== asked) {
                return;
            }
            const listed = new Set(item.papers.map(paper => paper.id));
            const options = found.filter(result => result.type === 'PAPER' && !listed.has(result.id))
                .slice(0, PAPER_RESULTS);
            results.replaceChildren(...(options.length > 0
                ? options.map(result => el('li', null, button(['plus', result.title], 'paper-result',
                    () => change('POST', base + '/papers', { paperId: result.id }, redraw, 'Paper added'))))
                : [el('li', 'muted side-empty', 'No papers match.')]));
        }, 300);
    });
    return el('div', 'paper-picker', box, results);
}

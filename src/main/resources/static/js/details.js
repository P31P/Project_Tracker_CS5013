// The longer write-up of a project or an idea: five optional parts in Markdown, plus whether others can join.
// Shared by the Projects and Ideas tabs so both describe a piece of work the same way.

import { markdown } from './markdown.js';
import { el, field, select, textarea } from './ui.js';

export const DETAIL_PARTS = [
    ['coreQuestion', 'Core question', 'What question does it try to answer?'],
    ['motivation', 'Motivation', 'Why does it matter?'],
    ['approach', 'Proposed approach', 'How could it be done?'],
    ['resources', 'Data and resources needed', 'Datasets, tools, compute, people…'],
    ['challenges', 'Challenges', 'What could make it hard?'],
];

/** The five text boxes, filled from details if given. Their names start with "detail-" to keep them apart. */
export function detailFields(details = {}) {
    return DETAIL_PARTS.map(([key, label, hint]) =>
        field(label, textarea('detail-' + key, { value: details[key] || '', placeholder: hint, rows: 2 })));
}

/** Reads the five parts back out of a form's values. */
export function detailsFrom(values) {
    return Object.fromEntries(DETAIL_PARTS.map(([key]) => [key, values['detail-' + key] || '']));
}

export function hasDetails(details) {
    return Boolean(details) && DETAIL_PARTS.some(([key]) => details[key]);
}

/** The parts that are filled in, each under its heading. */
export function detailSections(details) {
    return DETAIL_PARTS.filter(([key]) => details[key]).map(([key, label]) =>
        el('section', 'detail-part', el('h3', 'detail-part-title', label), markdown(details[key])));
}

/** A select for whether others can join; its value is "true" or "false". */
export function collabSelect(open) {
    return select('openForCollab', [['false', 'Closed or solo'], ['true', 'Open for collaboration']], String(open));
}

export function collabBadge(open) {
    return open ? el('span', 'badge-collab', 'Open for collaboration') : null;
}

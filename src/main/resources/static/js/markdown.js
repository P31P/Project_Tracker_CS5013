// A small Markdown renderer that builds DOM nodes, so text from users can never become HTML.
// Supports paragraphs (a new line is a line break), # headings, > quotes, - and 1. lists, ``` code blocks,
// **bold**, *italic*, `code`, [text](https://…) links and bare https:// links. Only http(s) links are made.

import { el } from './ui.js';

const FENCE = /^\s*```/;
const HEADING = /^(#{1,3})\s+(.*)$/;
const QUOTE = /^>\s?(.*)$/;
const BULLET = /^\s*[-*+]\s+(.*)$/;
const NUMBERED = /^\s*\d+[.)]\s+(.*)$/;

// one alternative per inline style; the first that matches at a position wins
const INLINE = /`([^`]+)`|\*\*([^*]+)\*\*|\*([^*\s][^*]*)\*|\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)|(https?:\/\/[^\s<>]*[^\s<>.,;:!?)\]'"])/g;

/** Renders Markdown text into a <div class="markdown">. */
export function markdown(text) {
    const lines = (text || '').replace(/\r\n?/g, '\n').split('\n');
    const blocks = [];
    let i = 0;
    while (i < lines.length) {
        const line = lines[i];
        if (line.trim() === '') {
            i++;
        } else if (FENCE.test(line)) {
            const code = [];
            i++;
            while (i < lines.length && !FENCE.test(lines[i])) {
                code.push(lines[i++]);
            }
            i++; // the closing ```
            blocks.push(el('pre', null, el('code', null, code.join('\n'))));
        } else if (HEADING.test(line)) {
            const [, hashes, title] = line.match(HEADING);
            // card titles are h3, so the largest Markdown heading is h4
            blocks.push(el('h' + (hashes.length + 3), 'md-heading', ...inline(title)));
            i++;
        } else if (QUOTE.test(line)) {
            i = collect(lines, i, QUOTE, quoted => blocks.push(el('blockquote', null, ...withBreaks(quoted))));
        } else if (BULLET.test(line)) {
            i = collect(lines, i, BULLET, items => blocks.push(list('ul', items)));
        } else if (NUMBERED.test(line)) {
            i = collect(lines, i, NUMBERED, items => blocks.push(list('ol', items)));
        } else {
            const paragraph = [];
            while (i < lines.length && lines[i].trim() !== '' && !startsBlock(lines[i])) {
                paragraph.push(lines[i++]);
            }
            blocks.push(el('p', null, ...withBreaks(paragraph)));
        }
    }
    return el('div', 'markdown', ...blocks);
}

/** Markdown reduced to plain text, for one- or two-line previews. */
export function plainText(text) {
    return (text || '')
        .replace(/^\s*```.*$/gm, '')
        .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '$1')
        .replace(/^\s*(#{1,3}\s+|>\s?|[-*+]\s+|\d+[.)]\s+)/gm, '')
        .replace(/\*\*|\*|`/g, '')
        .replace(/\s+/g, ' ')
        .trim();
}

/** A short note for under a text box, saying Markdown works there. */
export function markdownHint() {
    return el('span', 'markdown-hint', 'Markdown works: **bold**, *italic*, `code`, - lists, [links](https://…)');
}

function startsBlock(line) {
    return FENCE.test(line) || HEADING.test(line) || QUOTE.test(line) || BULLET.test(line) || NUMBERED.test(line);
}

// gathers the run of lines matching `pattern` from `start`, passes their contents on, returns the next line
function collect(lines, start, pattern, use) {
    const contents = [];
    let i = start;
    while (i < lines.length && pattern.test(lines[i])) {
        contents.push(lines[i].match(pattern)[1]);
        i++;
    }
    use(contents);
    return i;
}

function list(tag, items) {
    return el(tag, null, ...items.map(item => el('li', null, ...inline(item))));
}

function withBreaks(lines) {
    return lines.flatMap((line, index) => index === 0 ? inline(line) : [el('br'), ...inline(line)]);
}

function inline(text) {
    const nodes = [];
    let last = 0;
    for (const match of text.matchAll(INLINE)) {
        if (match.index > last) {
            nodes.push(text.slice(last, match.index));
        }
        const [, code, bold, italic, label, url, bare] = match;
        if (code !== undefined) {
            nodes.push(el('code', null, code));
        } else if (bold !== undefined) {
            nodes.push(el('strong', null, ...inline(bold)));
        } else if (italic !== undefined) {
            nodes.push(el('em', null, ...inline(italic)));
        } else {
            nodes.push(link(label || bare, url || bare));
        }
        last = match.index + match[0].length;
    }
    if (last < text.length) {
        nodes.push(text.slice(last));
    }
    return nodes;
}

function link(label, url) {
    const anchor = el('a', null, label);
    anchor.href = url;
    anchor.target = '_blank';
    anchor.rel = 'noopener noreferrer';
    return anchor;
}

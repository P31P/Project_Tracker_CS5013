// A discussion under a project: comments oldest first, and a box to add one. `base` is the project's API path.

import { getJson, sendJson } from './api.js';
import { icon } from './icons.js';
import { markdown, markdownHint } from './markdown.js';
import { avatar, el, relativeTime, textarea, toast } from './ui.js';

export function discussionSection(base) {
    const thread = el('ol', 'comments', el('li', 'muted comments-note', 'Loading the discussion…'));
    const load = () => getJson(base + '/comments')
        .then(comments => thread.replaceChildren(...(comments.length > 0
            ? comments.map(commentItem)
            : [el('li', 'muted comments-note', 'No comments yet. Ask a question or share a thought.')])))
        .catch(problem => thread.replaceChildren(el('li', 'notice notice-error',
            'Could not load the discussion. ' + problem.message)));
    load();

    const body = textarea('body', { placeholder: 'Add to the discussion…', rows: 2 });
    body.setAttribute('aria-label', 'Comment');
    const post = el('button', 'button button-small', icon('message', 16), 'Comment');
    post.type = 'submit';
    const form = el('form', 'comment-form', body, el('div', 'composer-actions', markdownHint(), post));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        post.disabled = true;
        try {
            await sendJson('POST', base + '/comments', { body: body.value });
            body.value = '';
            toast('Comment posted');
            await load();
        } catch (problem) {
            toast(problem.message, 'error');
        } finally {
            post.disabled = false;
        }
    });
    return el('section', 'card project-discussion', thread, form);
}

function commentItem(comment) {
    const name = comment.authorName || 'Someone';
    return el('li', 'comment', avatar(name),
        el('div', 'comment-body',
            el('p', 'comment-meta', el('strong', null, name), ' · ' + relativeTime(comment.createdAt)),
            markdown(comment.body)));
}

// Manage access (#access): who can sign in and what they may do. Only Lab Managers and admins reach it; the
// server checks again on every change.

import { getJson, sendJson } from './api.js';
import { avatar, button, el, errorState, field, formDialog, input, select, skeleton, toast } from './ui.js';

const ROLES = [['MEMBER', 'Member'], ['LAB_MANAGER', 'Lab Manager'], ['ADMIN', 'Admin']];

export async function renderAccess(container, page) {
    container.replaceChildren(skeleton(3));
    let people;
    let me;
    try {
        [people, me] = await Promise.all([getJson('/api/access'), getJson('/api/me')]);
    } catch (problem) {
        container.replaceChildren(errorState(problem.message));
        return;
    }
    const replace = updated => {
        const known = people.some(person => person.id === updated.id);
        people = known ? people.map(person => person.id === updated.id ? updated : person) : [...people, updated];
        draw();
    };
    page.actions.replaceChildren(button(['plus', 'Add person'], 'button', () => addDialog(replace)));
    const draw = () => {
        const active = people.filter(person => person.allowlisted);
        const removed = people.filter(person => !person.allowlisted);
        container.replaceChildren(...[
            el('p', 'muted access-note', 'People sign in with the Google account added here. Removing someone signs them '
                + 'out straight away; their projects, ideas and comments stay.'),
            el('div', 'card access-list', ...active.map(person => row(person, me, replace))),
            removed.length > 0 && el('h2', 'section-title', 'Access removed'),
            removed.length > 0 && el('div', 'card access-list', ...removed.map(person => row(person, me, replace))),
        ].filter(Boolean));
    };
    draw();
}

function row(person, me, replace) {
    const name = person.name || person.email;
    const self = person.email === me.email;
    const role = select('role', ROLES, person.role);
    role.setAttribute('aria-label', 'Role of ' + name);
    role.disabled = self || !person.allowlisted;
    role.addEventListener('change', async () => {
        try {
            replace(await sendJson('PATCH', '/api/access/' + person.id, { role: role.value }));
            toast(name + ' is now ' + ROLES.find(([key]) => key === role.value)[1]);
        } catch (problem) {
            role.value = person.role;
            toast(problem.message, 'error');
        }
    });
    const toggle = button(person.allowlisted ? 'Remove access' : 'Restore access',
        person.allowlisted ? 'button button-secondary button-small' : 'button button-small', async () => {
            if (person.allowlisted && !confirm('Remove ' + name + '\'s access? They will be signed out.')) {
                return;
            }
            try {
                replace(await sendJson('PATCH', '/api/access/' + person.id, { allowlisted: !person.allowlisted }));
                toast(person.allowlisted ? name + ' can no longer sign in' : name + ' can sign in again');
            } catch (problem) {
                toast(problem.message, 'error');
            }
        });
    toggle.disabled = self;
    return el('div', 'access-row',
        avatar(name),
        el('div', 'access-who', el('p', 'access-name', name + (self ? ' (you)' : '')),
            el('p', 'access-email', person.email)),
        role, toggle);
}

function addDialog(replace) {
    formDialog('Add person', [
        field('Google account email', input('email', { placeholder: 'name@smail.iitm.ac.in', required: true, type: 'email' })),
        field('Name (optional)', input('name', { placeholder: 'Filled in from Google when they sign in' })),
        field('Role', select('role', ROLES, 'MEMBER')),
    ], 'Add', async values => {
        const added = await sendJson('POST', '/api/access', { email: values.email, name: values.name, role: values.role });
        toast((added.name || added.email) + ' can now sign in');
        replace(added);
    });
}

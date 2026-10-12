// Home tab: lab-wide numbers, the projects you are part of, and what happened lately.

import { getJson } from './api.js';
import { icon } from './icons.js';
import { newProjectDialog, statusChip } from './projects.js';
import { avatarStack, button, el, emptyState, errorState, relativeTime, skeleton } from './ui.js';

const FEED_ICONS = { NOTE: 'message', STATUS: 'refresh', PROJECT: 'folder', IDEA: 'bulb', PAPER: 'paper' };

export async function renderHome(container, page) {
    page.actions.append(
        button(['bulb', 'New idea'], 'button button-secondary', () => { location.hash = 'ideas'; }),
        button(['plus', 'New project'], 'button', () => newProjectDialog()));
    container.replaceChildren(skeleton(3));

    let overview;
    let ideas;
    try {
        [overview, ideas] = await Promise.all([getJson('/api/overview'), getJson('/api/ideas')]);
    } catch (problem) {
        container.replaceChildren(errorState('Could not load the overview. ' + problem.message));
        return;
    }
    const me = await getJson('/api/me').catch(() => null);
    const firstName = me && me.name ? me.name.split(' ')[0] : null;
    page.heading.textContent = greeting() + (firstName ? ', ' + firstName : '');

    const counts = overview.counts;
    container.replaceChildren(
        el('div', 'stats',
            statCard('activity', 'Active projects', counts.active, '#projects', 145),
            statCard('flag', 'In planning', counts.planning, '#projects', 265),
            statCard('check', 'Done', counts.done, '#projects', 215),
            statCard('bulb', 'Open ideas', counts.openIdeas, '#ideas', 40),
            statCard('paper', 'Papers', counts.papers, '#papers', 190)),
        el('div', 'home-grid',
            el('section', 'home-section',
                sectionHead('Your projects', '#projects', 'All projects'),
                myProjects(overview.mine),
                sectionHead('Popular ideas', '#ideas', 'All ideas'),
                popularIdeas(ideas)),
            el('section', 'home-section',
                sectionHead('Recent activity'),
                feed(overview.activity))));
}

function greeting() {
    const hour = new Date().getHours();
    return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
}

function statCard(iconName, label, value, href, hue) {
    const card = el('a', 'stat', el('span', 'stat-icon', icon(iconName, 20)),
        el('span', 'stat-value', String(value)), el('span', 'stat-label', label));
    card.href = href;
    card.style.setProperty('--hue', hue);
    return card;
}

function sectionHead(title, href, linkText) {
    const link = href && el('a', 'section-link', linkText + ' →');
    if (link) {
        link.href = href;
    }
    return el('div', 'section-head', el('h2', 'section-title', title), link);
}

function myProjects(projects) {
    if (projects.length === 0) {
        return emptyState('Nothing here yet', 'Projects you start or are added to show up here.', null, 'folder');
    }
    return el('div', 'card mini-list', ...projects.slice(0, 6).map(project => {
        const row = el('a', 'mini-project',
            el('span', 'mini-main', el('span', 'mini-title', project.title),
                el('span', 'mini-meta', 'updated ' + relativeTime(project.updatedAt))),
            avatarStack(project.members, 3),
            statusChip(project.status));
        row.href = '#projects/' + project.id;
        return row;
    }));
}

// open ideas with at least one vote, most votes first
function popularIdeas(ideas) {
    const popular = ideas.filter(idea => idea.projectId === null && idea.voteCount > 0)
        .sort((a, b) => b.voteCount - a.voteCount)
        .slice(0, 4);
    if (popular.length === 0) {
        return el('p', 'muted', 'No votes yet. Upvote the ideas you would like to see happen.');
    }
    return el('div', 'card mini-list', ...popular.map(idea => {
        const row = el('a', 'mini-project',
            el('span', 'mini-votes', icon('up', 14), String(idea.voteCount)),
            el('span', 'mini-main', el('span', 'mini-title', idea.title),
                el('span', 'mini-meta', [idea.authorName, relativeTime(idea.createdAt)].filter(Boolean).join(' · '))));
        row.href = '#ideas';
        return row;
    }));
}

function feed(items) {
    if (items.length === 0) {
        return emptyState('Quiet so far', 'New projects, updates, ideas and papers will appear here.', null, 'activity');
    }
    return el('ol', 'card feed', ...items.map(item => {
        const target = el('a', 'feed-target', item.target);
        // paper links leave the app; everything else is a tab link
        if (item.href.startsWith('#')) {
            target.href = item.href;
        } else if (/^https?:\/\//i.test(item.href)) {
            target.href = item.href;
            target.target = '_blank';
            target.rel = 'noopener noreferrer';
        }
        return el('li', 'feed-item feed-' + item.kind.toLowerCase(),
            el('span', 'feed-icon', icon(FEED_ICONS[item.kind], 16)),
            el('div', 'feed-body',
                el('p', 'feed-text', item.who && el('strong', null, item.who), item.who ? ' ' + item.action + ' ' : item.action + ': ',
                    target),
                item.excerpt && el('p', 'feed-excerpt clamp', item.excerpt),
                el('p', 'feed-time', relativeTime(item.at))));
    }));
}

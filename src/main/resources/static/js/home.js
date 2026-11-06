// Home tab: your profile, lab-wide numbers, your projects and ideas, and what happened lately, with the new
// ideas and papers that match your interests under "For you".

import { getJson } from './api.js';
import { icon } from './icons.js';
import { plainText } from './markdown.js';
import { profileCard } from './profile.js';
import { newProjectDialog, statusChip } from './projects.js';
import { avatarStack, button, el, emptyState, errorState, filterChips, relativeTime, skeleton, tagPill } from './ui.js';

const FEED_ICONS = { NOTE: 'message', STATUS: 'refresh', PROJECT: 'folder', IDEA: 'bulb', PAPER: 'paper' };

export async function renderHome(container, page) {
    page.actions.append(
        button(['bulb', 'New idea'], 'button button-secondary', () => { location.hash = 'ideas'; }),
        button(['plus', 'New project'], 'button', () => newProjectDialog()));
    await load(container, page);
}

async function load(container, page) {
    container.replaceChildren(skeleton(3));

    let overview;
    let ideas;
    let profile;
    try {
        [overview, ideas, profile] = await Promise.all(
            [getJson('/api/overview'), getJson('/api/ideas'), getJson('/api/profile')]);
    } catch (problem) {
        container.replaceChildren(errorState('Could not load the overview. ' + problem.message));
        return;
    }
    // the name falls back to the email when Google gave none, and an email makes an odd greeting
    const firstName = profile.name && !profile.name.includes('@') ? profile.name.split(' ')[0] : null;
    page.heading.textContent = greeting() + (firstName ? ', ' + firstName : '');

    // saving the profile changes "For you" too, so the whole page is drawn again
    const card = profileCard(profile, () => load(container, page));
    const counts = overview.counts;
    // the lab-wide numbers are the admin's view; members get their own dashboard
    const labWide = profile.role === 'ADMIN' && el('section', 'lab-overview',
        sectionHead('Across the lab'),
        el('div', 'stats',
            statCard('Ongoing projects', counts.active, '#projects'),
            statCard('In planning', counts.planning, '#projects'),
            statCard('Completed', counts.done, '#projects'),
            statCard('Open ideas', counts.openIdeas, '#ideas'),
            statCard('Papers', counts.papers, '#papers')));
    container.replaceChildren(...[
        card,
        labWide,
        el('div', 'home-grid',
            el('section', 'home-section',
                sectionHead('Your projects', '#projects', 'All projects'),
                myProjects(overview.mine),
                sectionHead('Your ideas', '#ideas', 'All ideas'),
                myIdeas(overview.myIdeas),
                sectionHead('Popular ideas'),
                popularIdeas(ideas)),
            activitySection(overview, profile)),
    ].filter(Boolean));
}

// "For you" first when something matches, so the page opens on what is relevant to this person
function activitySection(overview, profile) {
    const section = el('section', 'home-section');
    let shown = overview.forYou.length > 0 ? 'FOR_YOU' : 'ALL';
    const draw = () => {
        const chips = filterChips([['FOR_YOU', 'For you', overview.forYou.length],
            ['ALL', 'Everything', overview.activity.length]], shown, key => { shown = key; draw(); });
        section.replaceChildren(sectionHead('Recent activity'), chips,
            shown === 'ALL' ? feed(overview.activity) : forYou(overview.forYou, profile));
    };
    draw();
    return section;
}

function forYou(items, profile) {
    if (items.length > 0) {
        return feed(items);
    }
    return profile.interests.length === 0
        ? emptyState('Nothing picked for you yet',
            'Add your fields of interest to your profile, and new ideas and papers on those topics will show up here.',
            null, 'tag')
        : emptyState('Nothing new on your topics',
            'No new ideas or papers in the last 30 days match your interests.', null, 'tag');
}

function myIdeas(ideas) {
    if (ideas.length === 0) {
        return el('p', 'muted', 'Ideas you share show up here.');
    }
    return el('div', 'card mini-list', ...ideas.slice(0, 5).map(idea => {
        const row = el('a', 'mini-project',
            el('span', 'mini-votes', icon('up', 14), String(idea.voteCount)),
            el('span', 'mini-main', el('span', 'mini-title', idea.title),
                el('span', 'mini-meta', idea.projectId !== null
                    ? 'became a project' : 'shared ' + relativeTime(idea.createdAt))));
        row.href = idea.projectId !== null ? '#projects/' + idea.projectId : '#ideas';
        return row;
    }));
}

function greeting() {
    const hour = new Date().getHours();
    return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
}

function statCard(label, value, href) {
    const card = el('a', 'stat', el('span', 'stat-label', label), el('span', 'stat-value', String(value)));
    card.href = href;
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
                item.excerpt && el('p', 'feed-excerpt clamp', plainText(item.excerpt)),
                item.interest && el('p', 'feed-match', 'Matches your interest', tagPill(item.interest)),
                el('p', 'feed-time', relativeTime(item.at))));
    }));
}

// A lab member's profile card (introduction, background, interests), shown on Home and in the People tab,
// and the dialog where people edit their own.

import { sendJson } from './api.js';
import { markdown, markdownHint } from './markdown.js';
import { avatar, button, el, field, formDialog, input, splitTags, tagList, textarea, toast } from './ui.js';

/**
 * Builds a profile card. Pass onSaved for the signed-in person's own profile: the card then gets an edit button,
 * and onSaved receives the profile after it is saved.
 */
export function profileCard(profile, onSaved) {
    loadStylesheet();
    const empty = !profile.bio && !profile.background && profile.interests.length === 0;
    const edit = onSaved && button(['edit', empty ? 'Fill in your profile' : 'Edit profile'],
        empty ? 'button' : 'button button-secondary', () => editProfileDialog(profile, onSaved));
    const intro = profile.bio
        ? el('div', 'profile-bio', markdown(profile.bio))
        : el('p', 'muted', onSaved
            ? 'Tell the lab what you work on and what you would like to hear about. New ideas and papers that match '
                + 'your interests will show up under “For you”.'
            : 'No profile yet.');
    return el('section', 'card profile-card',
        el('div', 'profile-head',
            avatar(profile.name, 'avatar-profile'),
            el('div', 'profile-who',
                el('h2', 'profile-name', profile.name),
                profile.background && el('p', 'profile-background', profile.background),
                el('p', 'profile-email', profile.email)),
            edit),
        intro,
        profile.interests.length > 0 && el('div', 'profile-interests',
            el('h3', 'profile-label', 'Interested in'), tagList(profile.interests)));
}

function editProfileDialog(profile, onSaved) {
    formDialog('Your profile', [
        field('Introduction', textarea('bio', {
            value: profile.bio || '', rows: 5, placeholder: 'What you work on, and what you would like help with',
        })),
        markdownHint(),
        field('Study background', input('background', {
            value: profile.background || '', placeholder: 'PhD, systems biology',
        })),
        field('Fields of interest, separated by commas', input('interests', {
            value: profile.interests.join(', '), placeholder: 'genomics, machine learning, metabolic modeling',
        })),
    ], 'Save', async values => {
        const saved = await sendJson('PATCH', '/api/profile', {
            bio: values.bio, background: values.background, interests: splitTags(values.interests),
        });
        toast('Profile saved');
        onSaved(saved);
    });
}

function loadStylesheet() {
    if (document.getElementById('profile-css')) {
        return;
    }
    const stylesheet = document.createElement('link');
    stylesheet.id = 'profile-css';
    stylesheet.rel = 'stylesheet';
    stylesheet.href = new URL('../css/profile.css', import.meta.url).href;
    document.head.append(stylesheet);
}

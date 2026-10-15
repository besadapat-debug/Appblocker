// Turns what the user typed in a block list ("youtube.com", "https://www.instagram.com/x")
// into every hostname that has to go in the hosts file for that site to really be blocked.
//
// The hosts file has no wildcards, so "youtube.com" alone leaves www., m., music. etc.
// open - and big sites load their content from other domains (YouTube's app shell can
// come from a cached service worker and then pull videos/feeds from googleapis.com).
// So: every site gets its common subdomains, and well-known sites get their extra
// content domains too.

// Common subdomains added for every blocked site.
const COMMON_SUBS = ['www', 'm', 'mobile'];

// Extra hosts for popular sites. Keyed by the bare domain the user types.
const ALIASES = {
  'youtube.com': [
    'music.youtube.com', 'studio.youtube.com', 'tv.youtube.com', 'gaming.youtube.com', 'kids.youtube.com',
    'youtubekids.com', 'www.youtubekids.com',
    'youtu.be', 'www.youtu.be',
    'youtube-nocookie.com', 'www.youtube-nocookie.com',
    'youtubei.googleapis.com', 'youtube.googleapis.com', 'yt.be',
    'i.ytimg.com', 's.ytimg.com', 'i9.ytimg.com', 'ytimg.com',
    'yt3.ggpht.com', 'yt4.ggpht.com', 'yt3.googleusercontent.com',
    'redirector.googlevideo.com'
  ],
  'youtu.be': ['youtube.com'],
  'instagram.com': ['i.instagram.com', 'graph.instagram.com', 'help.instagram.com', 'l.instagram.com',
    'scontent.cdninstagram.com', 'static.cdninstagram.com', 'cdninstagram.com', 'instagr.am', 'www.instagr.am'],
  'facebook.com': ['web.facebook.com', 'touch.facebook.com', 'mbasic.facebook.com', 'fb.com', 'www.fb.com',
    'fb.me', 'static.xx.fbcdn.net', 'graph.facebook.com'],
  'tiktok.com': ['vm.tiktok.com', 'vt.tiktok.com', 'us.tiktok.com', 'tiktokv.com', 'www.tiktokv.com',
    'tiktokcdn.com', 'tiktokcdn-us.com', 'p16-sign-va.tiktokcdn.com', 'v16-webapp.tiktok.com', 'webcast.tiktok.com'],
  'twitter.com': ['x.com', 'www.x.com', 'mobile.x.com', 'api.twitter.com', 'api.x.com', 't.co',
    'abs.twimg.com', 'pbs.twimg.com', 'video.twimg.com'],
  'x.com': ['twitter.com'],
  'reddit.com': ['old.reddit.com', 'new.reddit.com', 'np.reddit.com', 'gateway.reddit.com', 'oauth.reddit.com',
    'redd.it', 'i.redd.it', 'v.redd.it', 'preview.redd.it', 'redditmedia.com', 'www.redditmedia.com',
    'redditstatic.com', 'www.redditstatic.com'],
  'snapchat.com': ['web.snapchat.com', 'accounts.snapchat.com', 'story.snapchat.com'],
  'netflix.com': ['assets.nflxext.com', 'nflxvideo.net', 'nflximg.net'],
  'twitch.tv': ['clips.twitch.tv', 'gql.twitch.tv', 'usher.ttvnw.net', 'static.twitchcdn.net'],
  'pinterest.com': ['pinterest.com.au', 'www.pinterest.com.au', 'au.pinterest.com', 'pin.it', 'i.pinimg.com', 's.pinimg.com'],
  'linkedin.com': ['lnkd.in', 'static.licdn.com', 'media.licdn.com'],
  'discord.com': ['discord.gg', 'discordapp.com', 'www.discordapp.com', 'cdn.discordapp.com', 'gateway.discord.gg'],
  'threads.net': ['www.threads.net', 'threads.com', 'www.threads.com']
};

// "https://www.YouTube.com/feed" -> "youtube.com"
function clean(input) {
  let h = String(input || '').trim().toLowerCase();
  h = h.replace(/^[a-z]+:\/\//, '').replace(/[/?#].*$/, '').replace(/:\d+$/, '').replace(/\.$/, '');
  if (!/^[a-z0-9.-]+\.[a-z0-9-]{2,}$/.test(h)) return '';
  return h;
}

function bare(h) {
  return h.replace(/^(www|m|mobile)\./, '');
}

function withSubs(host) {
  const b = bare(host);
  const out = [b];
  // Only add subdomains to registrable-looking domains (example.com / example.com.au),
  // not to deep hosts like i.ytimg.com.
  const parts = b.split('.');
  const shallow = parts.length === 2 || (parts.length === 3 && parts[1].length <= 3 && parts[2].length === 2);
  if (shallow) for (const s of COMMON_SUBS) out.push(`${s}.${b}`);
  if (host !== b) out.push(host);
  return out;
}

// Every hostname to put in the hosts file for one user-entered site.
function expand(input, seen = new Set()) {
  const host = clean(input);
  if (!host) return [];
  const b = bare(host);
  if (seen.has(b)) return [];
  seen.add(b);
  const out = withSubs(host);
  for (const extra of (ALIASES[b] || [])) {
    if (ALIASES[extra] && !seen.has(extra)) out.push(...expand(extra, seen));
    else out.push(...(extra.split('.').length === 2 ? withSubs(extra) : [extra]));
  }
  return out;
}

function expandAll(list) {
  const set = new Set();
  for (const d of list || []) for (const h of expand(d)) set.add(h);
  return [...set];
}

// --- Website exceptions (allow a specific page/channel/profile on a blocked site) ---
// The host part of an exception pattern ("youtube.com/@Some" -> "youtube.com").
function excHost(ex) {
  let h = String(ex || '').trim().toLowerCase().replace(/^[a-z]+:\/\//, '');
  h = h.replace(/[/?#].*$/, '').replace(/:\d+$/, '').replace(/\.$/, '');
  return bare(h);
}
// Does a block-list website have an exception targeting its domain? If so we
// must NOT put it in the hosts file (hosts can't allow sub-paths) - the browser
// extension enforces it instead and lets the excepted URLs through.
function websiteHasException(website, exceptions) {
  const d = bare(clean(website));
  if (!d) return false;
  return (exceptions || []).some(ex => {
    const h = excHost(ex);
    return h && (h === d || d.endsWith('.' + h) || h.endsWith('.' + d));
  });
}

module.exports = { expand, expandAll, clean, ALIASES, excHost, websiteHasException };

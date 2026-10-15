// The heart: every few seconds work out what should be blocked right now and
// apply it - update the hosts file for block-list websites, keep the adult
// filter's browser policies in place, and kill blocked apps.
const { listActive } = require('../shared/schedule');
const safedns = require('../shared/safedns');
const sites = require('../shared/sites');
const hosts = require('./hosts');
const apps = require('./apps');

class Engine {
  constructor(store) {
    this.store = store;
    this.timer = null;
    this.lastError = null;
  }

  start() {
    this.stop();
    this.tick();
    this.timer = setInterval(() => this.tick(), 4000);
  }

  stop() {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  activeLists() {
    return (this.store.get().lists || []).filter(l => listActive(l));
  }

  async tick() {
    const active = this.activeLists();

    // Block-list websites -> hosts file (only rewritten when the set changes).
    // The adult list is NOT put in the hosts file any more (it made Windows DNS
    // crawl and Firefox/installers hang) - see safedns.js.
    const domains = [];
    // Skip sites that have a Website Exception - those are enforced by the
    // browser extension (which can allow the excepted pages), not the hosts file.
    for (const l of active) for (const w of (l.websites || [])) {
      if (!sites.websiteHasException(w, l.exceptions)) domains.push(w);
    }
    const res = hosts.applyDomains(domains);
    this.lastError = res.ok ? null : res.error;

    // Adult filter -> family-safe DNS inside the browsers.
    safedns.sync(!!this.store.get().adultFilter);

    // Apps -> kill on sight.
    const appNames = [];
    for (const l of active) for (const a of (l.apps || [])) appNames.push(a);
    await apps.enforce(appNames);
  }

  // Called when blocking is fully turned off (nothing locked and the app quits).
  async clearAll() {
    hosts.clear();
    await safedns.sync(false);
  }
}

module.exports = { Engine };

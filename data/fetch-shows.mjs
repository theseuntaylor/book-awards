import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { curate } from './theatre/curate.mjs';
import { sources } from './theatre/sources/index.mjs';

// Usage: node data/fetch-shows.mjs [--out <path>] [--candidates <path>] [--today YYYY-MM-DD]
// Scrapes each source's "what's on" page, applies data/shows-curated.json, and writes data/shows.json plus
// data/show-candidates.md (listings still waiting for an include or exclude decision).
const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const option = (name, fallback) => (args.includes(name) ? args[args.indexOf(name) + 1] : fallback);
const publishedPath = join(here, 'shows.json');
const outPath = resolve(option('--out', publishedPath));
const candidatesPath = resolve(option('--candidates', join(here, 'show-candidates.md')));
// London dates: the day the shows are listed for, whatever the machine's zone.
const today = option('--today', new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/London' }).format(new Date()));

const curated = JSON.parse(readFileSync(join(here, 'shows-curated.json'), 'utf8'));
// Always the published copy, so a show that vanished from its venue page can be carried over.
const previous = existsSync(publishedPath) ? JSON.parse(readFileSync(publishedPath, 'utf8')).shows : [];

const errors = [];
const listings = [];
for (const source of sources) {
  try {
    const found = await source.fetchListings(today);
    if (found.length === 0) {
      errors.push(`${source.source}: no listings found on ${source.page}; its page layout has probably changed`);
    } else {
      console.log(`${source.source}: ${found.length} listings`);
      listings.push(...found);
    }
  } catch (error) {
    errors.push(`${source.source}: ${error.message}`);
  }
}

const seenIds = new Set();
for (const listing of listings) {
  if (seenIds.has(listing.id)) errors.push(`${listing.id} is listed twice by its source`);
  seenIds.add(listing.id);
}

const result = curate({ listings, curated, previous, today });
errors.push(...result.errors);
for (const warning of result.warnings) console.warn(`warning: ${warning}`);

const candidateLines = result.candidates.map((c) => {
  const dates = c.startDate || c.endDate ? `${c.startDate ?? '…'} → ${c.endDate ?? '…'}` : `dates unread: "${c.dateText ?? ''}"`;
  return `- \`${c.id}\` **${c.title}** — ${[c.credits, c.venue].filter(Boolean).join(' · ')} — ${dates} — ${c.url}`;
});
writeFileSync(candidatesPath, `# Show candidates (${today})\n\nListings not yet included or excluded in data/shows-curated.json.\n\n${candidateLines.join('\n')}\n`);
console.log(`${result.candidates.length} candidates written to ${candidatesPath}`);

if (errors.length > 0) {
  console.error(`Not writing ${outPath}:\n${errors.map((e) => `  - ${e}`).join('\n')}`);
  process.exit(1);
}

writeFileSync(outPath, `${JSON.stringify({ generatedAt: new Date().toISOString(), shows: result.shows }, null, 2)}\n`);
console.log(`Wrote ${result.shows.length} shows to ${outPath}`);

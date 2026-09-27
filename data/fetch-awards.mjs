import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { applyCorrections, checkNominations, checkShrinkage, mergeWikipedia, statusRank } from './combine.mjs';
import { fetchWikipediaNominations } from './wikipedia-sources.mjs';
import { userAgent } from './wikitext.mjs';

// Usage: node data/fetch-awards.mjs [--out <path>] [--allow-shrink]
const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const outPath = args.includes('--out') ? resolve(args[args.indexOf('--out') + 1]) : join(here, 'awards.json');
const allowShrink = args.includes('--allow-shrink');

// Keys match the Award enum in the app.
const awards = {
  BOOKER: 'Q160082',
  PULITZER: 'Q833633',
  NATIONAL_BOOK_AWARD: 'Q3873144',
};

// Nominations sit either on the book, or on the author with a "for work" (P1686) qualifier.
// "has characteristic" (P1552) holds the nomination level, e.g. "Booker Prize shortlist".
function query(awardId) {
  // Each branch starts from the award so the planner never scans every dated statement.
  const branch = (subject, property, kind, forWork) => `{
    ?${subject} p:${property} ?st . ?st ps:${property} wd:${awardId} ; pq:P585 ?date .
    ${forWork ? '?st pq:P1686 ?work .' : 'FILTER NOT EXISTS { ?work wdt:P31 wd:Q5 }'}
    OPTIONAL { ?st pq:P1552 ?level . }
    BIND("${kind}" AS ?kind)
  }`;
  return `
SELECT ?work ?workLabel ?year ?kind ?levelLabel ?authorLabel ?personLabel WHERE {
  ${branch('work', 'P1411', 'nominated', false)}
  UNION ${branch('person', 'P1411', 'nominated', true)}
  UNION ${branch('work', 'P166', 'won', false)}
  UNION ${branch('person', 'P166', 'won', true)}
  BIND(YEAR(?date) AS ?year)
  OPTIONAL { ?work wdt:P50 ?author . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
}`;
}

async function runQuery(sparql) {
  const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparql)}`;
  const response = await fetch(url, {
    headers: { Accept: 'application/sparql-results+json', 'User-Agent': userAgent },
  });
  if (!response.ok) throw new Error(`Wikidata query failed: ${response.status} ${await response.text()}`);
  return (await response.json()).results.bindings;
}

function statusFor(kind, levelLabel) {
  if (kind === 'won') return 'WINNER';
  const level = (levelLabel ?? '').toLowerCase();
  if (level.includes('shortlist')) return 'SHORTLIST';
  if (level.includes('longlist')) return 'LONGLIST';
  if (level.includes('finalist')) return 'FINALIST';
  return 'NOMINEE';
}

const isUnlabelled = (label, id) => !label || label === id;

const nominations = [];
for (const [award, awardId] of Object.entries(awards)) {
  const rows = await runQuery(query(awardId));
  const byWorkAndYear = new Map();

  for (const row of rows) {
    const wikidataId = row.work.value.split('/').pop();
    const title = row.workLabel?.value;
    if (isUnlabelled(title, wikidataId)) continue;

    const year = Number(row.year.value);
    const key = `${wikidataId}|${year}`;
    const entry = byWorkAndYear.get(key) ?? { award, year, title, authors: new Set(), people: new Set(), status: 'NOMINEE', wikidataId };
    const status = statusFor(row.kind.value, row.levelLabel?.value);
    if (statusRank[status] < statusRank[entry.status]) entry.status = status;
    if (row.authorLabel?.value) entry.authors.add(row.authorLabel.value);
    if (row.personLabel?.value) entry.people.add(row.personLabel.value);
    byWorkAndYear.set(key, entry);
  }

  for (const entry of byWorkAndYear.values()) {
    // Prefer the book's own author (P50); fall back to whoever the nomination was recorded on.
    const authors = entry.authors.size > 0 ? entry.authors : entry.people;
    nominations.push({
      award: entry.award,
      year: entry.year,
      title: entry.title,
      author: [...authors].sort().join(', '),
      status: entry.status,
      wikidataId: entry.wikidataId,
    });
  }
  console.log(`${award}: ${byWorkAndYear.size} nominations from ${rows.length} rows`);
}

// Wikipedia lists new longlists and shortlists within days; Wikidata can lag by months.
const wikipediaReport = mergeWikipedia(nominations, await fetchWikipediaNominations());
for (const [award, { rows, added, upgraded }] of Object.entries(wikipediaReport)) {
  console.log(`${award}: Wikipedia had ${rows} rows, added ${added}, upgraded ${upgraded}`);
}

// Hand-curated fixes for gaps and errors in either source. A warning means a fix no longer
// matches anything, or has expired, so it can be deleted.
applyCorrections(nominations, JSON.parse(readFileSync(join(here, 'corrections.json'), 'utf8')));

nominations.sort((a, b) =>
  a.award.localeCompare(b.award) ||
  b.year - a.year ||
  statusRank[a.status] - statusRank[b.status] ||
  a.title.localeCompare(b.title),
);

checkNominations(nominations, Object.keys(awards));
const previousPath = existsSync(outPath) ? outPath : join(here, 'awards.json');
if (existsSync(previousPath)) checkShrinkage(JSON.parse(readFileSync(previousPath, 'utf8')).nominations, nominations, allowShrink);

// The app only replaces its data with a strictly newer file, so every run is dated.
const generatedAt = new Date().toISOString();
writeFileSync(outPath, `${JSON.stringify({ generatedAt, nominations }, null, 2)}\n`);
console.log(`Wrote ${nominations.length} nominations to ${outPath}`);

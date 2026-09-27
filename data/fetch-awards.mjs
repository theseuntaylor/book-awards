import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const userAgent = 'book-awards/0.1 (https://github.com/theseuntaylor/book-awards)';

// Keys match the Award enum in the app.
const awards = {
  BOOKER: 'Q160082',
  PULITZER: 'Q833633',
  NATIONAL_BOOK_AWARD: 'Q3873144',
};

const statusRank = { WINNER: 0, SHORTLIST: 1, FINALIST: 2, LONGLIST: 3, NOMINEE: 4 };

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

// Hand-curated fixes for gaps and errors in Wikidata. A warning means a fix no longer
// matches anything, usually because Wikidata was corrected, so it can be deleted.
const corrections = JSON.parse(readFileSync(join(here, 'corrections.json'), 'utf8'));
const matches = (target) => (n) =>
  n.award === target.award && n.year === target.year && n.wikidataId === target.wikidataId;

for (const target of corrections.remove) {
  const index = nominations.findIndex(matches(target));
  if (index === -1) console.warn(`Stale removal: ${target.award} ${target.year} ${target.wikidataId}`);
  else nominations.splice(index, 1);
}

for (const target of corrections.update) {
  const nomination = nominations.find(matches(target));
  if (!nomination) console.warn(`Stale update: ${target.award} ${target.year} ${target.wikidataId}`);
  else Object.assign(nomination, target.set);
}

const normalizeTitle = (title) => title.toLowerCase().replaceAll('’', "'").trim();

for (const { award, year, nominations: additions } of corrections.add) {
  for (const { title, author, status, wikidataId = null } of additions) {
    if (!(award in awards) || !(status in statusRank)) {
      throw new Error(`Invalid addition: ${award} ${year} ${title}`);
    }
    // Match on ID when known, otherwise on title, so an addition Wikidata later gains isn't duplicated.
    const existing = nominations.find((n) =>
      n.award === award && n.year === year &&
      (wikidataId ? n.wikidataId === wikidataId : normalizeTitle(n.title) === normalizeTitle(title)));
    if (existing) {
      console.warn(`Addition now in Wikidata: ${award} ${year} ${title}`);
      existing.status = status;
    } else {
      nominations.push({ award, year, title, author, status, wikidataId });
    }
  }
}

nominations.sort((a, b) =>
  a.award.localeCompare(b.award) ||
  b.year - a.year ||
  statusRank[a.status] - statusRank[b.status] ||
  a.title.localeCompare(b.title),
);

writeFileSync(join(here, 'awards.json'), `${JSON.stringify({ nominations }, null, 2)}\n`);
console.log(`Wrote ${nominations.length} nominations to data/awards.json`);

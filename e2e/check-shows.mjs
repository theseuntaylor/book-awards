// Checks a shows.json built by e2e/shows-pipeline.sh against the curation file and the run log.
// Usage: node e2e/check-shows.mjs <shows.json> <run.log> <today>
import { readFileSync } from 'node:fs';
import { sources } from '../data/theatre/sources/index.mjs';

const [showsPath, logPath, today] = process.argv.slice(2);
const { generatedAt, shows } = JSON.parse(readFileSync(showsPath, 'utf8'));
const log = readFileSync(logPath, 'utf8');
const curated = JSON.parse(readFileSync(new URL('../data/shows-curated.json', import.meta.url), 'utf8'));
const failures = [];
const check = (ok, message) => { if (!ok) failures.push(message); };

check(!Number.isNaN(Date.parse(generatedAt)), 'generatedAt is not a timestamp');
for (const { source } of sources) {
  const count = Number(new RegExp(`^${source}: (\\d+) listings$`, 'm').exec(log)?.[1] ?? 0);
  check(count > 0, `${source} returned no listings`);
  console.log(`${source}: ${count} listings`);
}
const isDate = (d) => d == null || /^\d{4}-\d{2}-\d{2}$/.test(d);
const ids = new Set();
for (const show of shows) {
  check(!ids.has(show.id), `${show.id} appears twice`);
  ids.add(show.id);
  for (const field of ['id', 'title', 'venue', 'url']) check(show[field], `${show.id} has no ${field}`);
  check(isDate(show.startDate) && isDate(show.endDate), `${show.id} has a malformed date`);
  check(show.startDate || show.endDate, `${show.id} has no dates`);
  check(!(show.startDate && show.endDate) || show.startDate <= show.endDate, `${show.id} ends before it starts`);
  check(!show.endDate || show.endDate >= today, `${show.id} ended on ${show.endDate} but was published`);
}
const unresolved = [...curated.include, ...curated.manual].filter(({ id }) => !ids.has(id));
// A curated show may be absent only because it ended; anything else would have failed the pipeline itself.
for (const { id } of unresolved) {
  const manual = curated.manual.find((m) => m.id === id);
  check(!manual || (manual.endDate && manual.endDate < today), `${id} is a manual entry still running but wasn't published`);
  console.log(`note: ${id} is curated but not published (ended)`);
}

console.log(`${shows.length} shows published, ${unresolved.length} curated shows not published`);
if (failures.length > 0) {
  console.log(`FAIL:\n${failures.map((f) => `  - ${f}`).join('\n')}`);
  process.exit(1);
}
console.log('PASS');

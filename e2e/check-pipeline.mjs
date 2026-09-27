// Assertions for e2e/pipeline.sh against a freshly built awards file. Exits non-zero with every failure listed.
import { readFileSync } from 'node:fs';

const [, , generatedPath, publishedPath] = process.argv;
const generated = JSON.parse(readFileSync(generatedPath, 'utf8')).nominations;
const published = JSON.parse(readFileSync(publishedPath, 'utf8')).nominations;
const failures = [];
const check = (ok, message) => { if (!ok) failures.push(message); };

const entries = (award, year, status) =>
  generated.filter((n) => n.award === award && n.year === year && (!status || n.status === status));
const titles = (list) => list.map((n) => n.title).sort();
const sameTitles = (actual, expected) => JSON.stringify(titles(actual)) === JSON.stringify([...expected].sort());

// Booker 2026: the full longlist, with the shortlist marked and no winner before 9 November.
const booker2026Shortlist = ['Black Bag', 'John of John', 'May We Feed the King', 'The Disappearers', 'The End of Everything', 'The Things We Never Say'];
check(entries('BOOKER', 2026).length === 13, `Booker 2026 should have 13 books, has ${entries('BOOKER', 2026).length}`);
check(sameTitles(entries('BOOKER', 2026, 'SHORTLIST'), booker2026Shortlist), `Booker 2026 shortlist is ${titles(entries('BOOKER', 2026, 'SHORTLIST'))}`);
check(entries('BOOKER', 2026, 'LONGLIST').length === 7, `Booker 2026 should have 7 longlist-only books, has ${entries('BOOKER', 2026, 'LONGLIST').length}`);
check(entries('BOOKER', 2026, 'WINNER').length === 0, 'Booker 2026 has a winner before the announcement');

// Merging didn't duplicate a year both sources already had.
check(entries('BOOKER', 2025).length === 13, `Booker 2025 should still have 13 books, has ${entries('BOOKER', 2025).length}`);

// Pulitzer 2024: the winner the Wikidata-only data was missing, plus its finalists.
check(sameTitles(entries('PULITZER', 2024, 'WINNER'), ['Night Watch']), `Pulitzer 2024 winner is ${titles(entries('PULITZER', 2024, 'WINNER'))}`);
check(entries('PULITZER', 2024, 'FINALIST').length === 2, `Pulitzer 2024 should have 2 finalists, has ${entries('PULITZER', 2024, 'FINALIST').length}`);

// National Book Award: 2025 in full, and the 2026 longlist not mislabelled as finalists before 6 October.
const nba2025Winner = entries('NATIONAL_BOOK_AWARD', 2025, 'WINNER');
check(nba2025Winner.length === 1 && nba2025Winner[0].author === 'Rabih Alameddine', `NBA 2025 winner is ${JSON.stringify(nba2025Winner.map((n) => n.author))}`);
check(entries('NATIONAL_BOOK_AWARD', 2025, 'FINALIST').length === 4, `NBA 2025 should have 4 finalists, has ${entries('NATIONAL_BOOK_AWARD', 2025, 'FINALIST').length}`);
if (new Date().toISOString().slice(0, 10) < '2026-10-06') {
  const nba2026 = entries('NATIONAL_BOOK_AWARD', 2026);
  check(nba2026.length > 0 && nba2026.every((n) => n.status === 'LONGLIST'), `NBA 2026 should be longlist only, is ${JSON.stringify(nba2026.map((n) => n.status))}`);
}

// Nothing the app would choke on, and nothing lost relative to the published file.
const ids = generated.map((n) => `${n.award}|${n.year}|${n.wikidataId ?? `${n.title}|${n.author}`}`);
check(new Set(ids).size === ids.length, 'duplicate nomination ids');
// A published book counts as kept if it's there by ID or by title (hand-added books gain IDs from Wikipedia).
const present = new Set(generated.flatMap((n) => [`${n.award}|${n.year}|${n.wikidataId}`, `${n.award}|${n.year}|${n.title}`]));
const lost = published.filter((n) => !present.has(`${n.award}|${n.year}|${n.wikidataId}`) && !present.has(`${n.award}|${n.year}|${n.title}`));
check(lost.length === 0, `${lost.length} published nominations are missing, e.g. ${lost.slice(0, 3).map((n) => `${n.award} ${n.year} ${n.title}`)}`);

if (failures.length) {
  console.log(`FAIL:\n  ${failures.join('\n  ')}`);
  process.exit(1);
}
console.log(`PASS: ${generated.length} nominations; Booker 2026 longlist and shortlist, Pulitzer 2024 and NBA 2025 present; no duplicates; nothing lost`);

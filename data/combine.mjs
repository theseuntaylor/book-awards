// Combining sources into the published dataset: merging Wikipedia into Wikidata, applying hand
// corrections, and the checks that stop a bad run from overwriting good data.

export const statusRank = { WINNER: 0, SHORTLIST: 1, FINALIST: 2, LONGLIST: 3, NOMINEE: 4 };

/** Loose text identity: ignores case, accents, punctuation, "&" vs "and", and a leading article. */
export const matchKey = (title) =>
  title.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/&/g, ' and ').replace(/[’‘]/g, "'").replace(/[^a-z0-9' ]+/g, ' ').replace(/'/g, '')
    .replace(/\s+/g, ' ').trim().replace(/^(the|a|an) /, '');

const surname = (author) => matchKey(author.split(',')[0]).split(' ').at(-1);

// Wikidata often has several items for one novel, and Wikipedia links sometimes resolve to a namesake,
// so IDs alone under-match. Within one prize year an author has one book, so the author is the strong
// signal; title plus surname covers name variants, and keeps March (Brooks) apart from The March (Doctorow).
const sameBook = (a, b) =>
  a.award === b.award && a.year === b.year && (
    (a.wikidataId && a.wikidataId === b.wikidataId) ||
    (Boolean(a.author) && matchKey(a.author) === matchKey(b.author)) ||
    (matchKey(a.title) === matchKey(b.title) && surname(a.author) === surname(b.author))
  );

/** Wikipedia fills gaps and upgrades statuses; where both sources have a book, the better status wins. */
export function mergeWikipedia(nominations, wikipediaEntries) {
  const report = {};
  for (const entry of wikipediaEntries) {
    const counts = (report[entry.award] ??= { rows: 0, added: 0, upgraded: 0 });
    counts.rows++;
    const existing = nominations.find((n) => sameBook(n, entry));
    if (!existing) {
      nominations.push({ ...entry });
      counts.added++;
      continue;
    }
    if (statusRank[entry.status] < statusRank[existing.status]) {
      existing.status = entry.status;
      counts.upgraded++;
    }
    existing.wikidataId ??= entry.wikidataId;
    if (!existing.author) existing.author = entry.author;
  }
  return report;
}

/**
 * A correction targets a book by Wikidata ID or, when it has none, by title. `until` (YYYY-MM-DD) makes a
 * fix temporary: it stops applying on that date, for source errors expected to be corrected by then.
 */
export function applyCorrections(nominations, corrections, today = new Date().toISOString().slice(0, 10)) {
  const describe = (t) => `${t.award} ${t.year} ${t.wikidataId ?? t.title}`;
  const active = (t) => {
    if (t.until && today >= t.until) { console.warn(`Expired correction (until ${t.until}), delete it: ${describe(t)}`); return false; }
    return true;
  };
  const targets = (t) => (n) => n.award === t.award && n.year === t.year &&
    (t.wikidataId ? n.wikidataId === t.wikidataId : matchKey(n.title) === matchKey(t.title));

  for (const target of corrections.remove.filter(active)) {
    const index = nominations.findIndex(targets(target));
    if (index === -1) console.warn(`Stale removal: ${describe(target)}`);
    else nominations.splice(index, 1);
  }
  for (const target of corrections.update.filter(active)) {
    const matching = nominations.filter(targets(target));
    if (matching.length === 0) console.warn(`Stale update: ${describe(target)}`);
    for (const nomination of matching) Object.assign(nomination, target.set);
  }
  for (const { award, year, nominations: additions } of corrections.add) {
    for (const { title, author, status, wikidataId = null } of additions) {
      if (!(status in statusRank)) throw new Error(`Invalid addition: ${award} ${year} ${title}`);
      const addition = { award, year, title, author, status, wikidataId };
      const existing = nominations.find((n) => sameBook(n, addition));
      if (existing) {
        console.warn(`Addition now in the sources, delete it: ${award} ${year} ${title}`);
        existing.status = status;
      } else {
        nominations.push(addition);
      }
    }
  }
}

const idOf = (n) => `${n.award}|${n.year}|${n.wikidataId ?? `${n.title}|${n.author}`}`; // matches Nomination.id in the app

/** Throws on anything the app can't show correctly, including two entries for the same book. */
export function checkNominations(nominations, awardKeys) {
  const lastYear = new Date().getFullYear() + 1;
  const invalid = nominations.filter((n) =>
    !awardKeys.includes(n.award) || !(n.status in statusRank) || !n.title || !n.author || !(n.year >= 1900 && n.year <= lastYear));
  if (invalid.length) throw new Error(`${invalid.length} invalid nominations, e.g. ${JSON.stringify(invalid.slice(0, 3))}`);

  const seenIds = new Set();
  const duplicates = [];
  for (const [index, n] of nominations.entries()) {
    if (seenIds.has(idOf(n))) duplicates.push(`${n.title} (${n.award} ${n.year})`);
    seenIds.add(idOf(n));
    const twin = nominations.slice(0, index).find((other) => sameBook(other, n));
    if (twin) duplicates.push(`${twin.title} / ${n.title} (${n.award} ${n.year})`);
  }
  if (duplicates.length) throw new Error(`Duplicate nominations, add a correction:\n  ${duplicates.join('\n  ')}`);
}

/** Refuses to publish if an award lost more than 10% of its entries, e.g. from a vandalised page. */
export function checkShrinkage(previous, next, allowShrink) {
  const countByAward = (list) => list.reduce((counts, n) => ({ ...counts, [n.award]: (counts[n.award] ?? 0) + 1 }), {});
  const before = countByAward(previous);
  const after = countByAward(next);
  const lines = [];
  let shrank = false;
  for (const award of new Set([...Object.keys(before), ...Object.keys(after)])) {
    const was = before[award] ?? 0;
    const now = after[award] ?? 0;
    lines.push(`${award}: ${was} → ${now}`);
    if (now < was * 0.9) shrank = true;
  }
  console.log(`Totals: ${lines.join(', ')}`);
  if (shrank && !allowShrink) throw new Error('An award lost more than 10% of its nominations; rerun with --allow-shrink if that is intended.');
}

// Nominations from Wikipedia's prize tables, which list new longlists and shortlists within days,
// long before Wikidata records them. Columns are found by header name so a reordered table still parses.
import { cleanCell, fetchWikitext, isBold, linkTarget, parseTables, userAgent } from './wikitext.mjs';

const lastYear = new Date().getFullYear() + 1;

function columnsOf(table, names) {
  const headers = table.headers.map((h) => h.toLowerCase());
  return Object.fromEntries(names.map((name) => [name, headers.indexOf(name)]));
}

/** Tables that have every required column; throws if a page has none (its layout changed). */
function tablesWith(tables, required, page) {
  const matching = tables.filter((table) => required.every((name) => columnsOf(table, [name])[name] !== -1));
  if (matching.length === 0) throw new Error(`${page}: no table with columns ${required.join(', ')}; has the page layout changed?`);
  return matching;
}

const yearOf = (raw) => Number(cleanCell(raw).match(/\b(1[89]|20)\d{2}\b/)?.[0]);
const isNoAward = (text) => /no award|not awarded|withheld/i.test(text);

const sources = [
  {
    award: 'BOOKER',
    page: 'List_of_winners_and_nominated_authors_of_the_Booker_Prize',
    columns: ['year', 'award', 'author', 'title'],
    // The page also lists the Lost Man Booker (awarded in 2010 for 1970's novels), a separate prize.
    include: (row, c) => !/lost man booker/i.test(row[c.year] ?? ''),
    status(row, c) {
      const label = cleanCell(row[c.award] ?? "").toLowerCase();
      if (label.startsWith('winner')) return 'WINNER';
      if (label.startsWith('shortlist')) return 'SHORTLIST';
      if (label.startsWith('longlist')) return 'LONGLIST';
      return null;
    },
  },
  {
    award: 'NATIONAL_BOOK_AWARD',
    page: 'National_Book_Award_for_Fiction',
    columns: ['year', 'author', 'title', 'result'],
    // 1980–83 split the prize into categories; only Hardcover is the main fiction award.
    include: (row, c) => c.category === -1 || cleanCell(row[c.category] ?? "").toLowerCase() === 'hardcover',
    status(row, c) {
      const label = cleanCell(row[c.result] ?? "").toLowerCase();
      if (label.startsWith('winner')) return 'WINNER';
      if (label.startsWith('finalist')) return 'FINALIST';
      if (label.startsWith('longlist')) return 'LONGLIST';
      return null;
    },
  },
  {
    award: 'PULITZER',
    page: 'Pulitzer_Prize_for_Fiction',
    columns: ['year', 'author', 'work'],
    // The winner's title is bold; the other rows for a year are finalists.
    status: (row, c) => (isBold(row[c.work] ?? "") ? 'WINNER' : 'FINALIST'),
  },
];

function extract(source, tables) {
  const entries = [];
  const problems = [];
  for (const table of tablesWith(tables, source.columns, source.page)) {
    const c = columnsOf(table, [...source.columns, 'category']);
    const titleColumn = c.title ?? c.work;
    for (const row of table.rows) {
      if (source.include && !source.include(row, c)) continue;
      const titleRaw = row[titleColumn] ?? '';
      const title = cleanCell(titleRaw);
      const author = cleanCell(row[c.author] ?? '');
      if (!title && !author) continue; // e.g. a row holding only a year cell
      if (isNoAward(title) || isNoAward(author)) continue;
      const year = yearOf(row[c.year] ?? '');
      const status = source.status(row, c);
      if (!title && !status) continue; // a note spanning the row, e.g. "No runners up were recognized."
      const entry = { award: source.award, year, title, author, status, pageTitle: linkTarget(titleRaw)?.split('#')[0] ?? null };
      if (!title || !author || !status || !(year >= 1900 && year <= lastYear)) problems.push(entry);
      else entries.push(entry);
    }
  }
  if (problems.length > 0) {
    throw new Error(`${source.page}: ${problems.length} rows didn't parse, e.g. ${JSON.stringify(problems.slice(0, 3))}`);
  }
  return entries;
}

/** Maps article titles to Wikidata IDs, following redirects. Titles without an article map to null. */
async function resolveWikidataIds(pageTitles) {
  const ids = new Map();
  const unique = [...new Set(pageTitles.filter(Boolean))];
  for (let i = 0; i < unique.length; i += 50) {
    const batch = unique.slice(i, i + 50);
    const params = new URLSearchParams({
      action: 'query', prop: 'pageprops', ppprop: 'wikibase_item', redirects: '1',
      format: 'json', formatversion: '2', titles: batch.join('|'),
    });
    const response = await fetch(`https://en.wikipedia.org/w/api.php?${params}`, { headers: { 'User-Agent': userAgent } });
    if (!response.ok) throw new Error(`Wikipedia ID lookup failed: ${response.status}`);
    const { query } = await response.json();
    const next = new Map([...(query.normalized ?? []), ...(query.redirects ?? [])].map(({ from, to }) => [from, to]));
    const idByTitle = new Map(query.pages.map((page) => [page.title, page.pageprops?.wikibase_item ?? null]));
    for (const title of batch) {
      let resolved = title;
      for (let hops = 0; hops < 3 && next.has(resolved); hops++) resolved = next.get(resolved);
      ids.set(title, idByTitle.get(resolved) ?? null);
    }
  }
  return ids;
}

export async function fetchWikipediaNominations() {
  const entries = [];
  for (const source of sources) {
    const found = extract(source, parseTables(await fetchWikitext(source.page)));
    console.log(`${source.award}: ${found.length} rows from Wikipedia`);
    entries.push(...found);
  }
  const ids = await resolveWikidataIds(entries.map((entry) => entry.pageTitle));
  return entries.map(({ pageTitle, ...entry }) => ({ ...entry, wikidataId: ids.get(pageTitle) ?? null }));
}

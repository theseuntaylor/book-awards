// Open Library cover IDs for each book, so the app can show thumbnails without a lookup per book.
// IDs from the previous run are reused; only books never looked up are fetched, at about one request a second.
import { matchKey } from './combine.mjs';
import { userAgent } from './wikitext.mjs';

const bookKey = (n) => n.wikidataId ?? `${n.title}|${n.author}`; // matches Nomination.bookKey in the app
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function search(params) {
  const query = new URLSearchParams({ ...params, fields: 'title,cover_i', limit: '5' });
  for (let attempt = 0; attempt < 2; attempt++) {
    const response = await fetch(`https://openlibrary.org/search.json?${query}`, { headers: { 'User-Agent': userAgent } });
    if (response.status === 429 && attempt === 0) { await sleep(60_000); continue; }
    if (!response.ok) throw new Error(`Open Library search failed: ${response.status}`);
    return (await response.json()).docs ?? [];
  }
}

/**
 * The title+author search is precise but misses books Open Library files under a slightly different title
 * ("Bee Sting" for "The Bee Sting"); the general search finds those but also returns study guides and the
 * like, so its results must match the title.
 */
async function lookupCover(title, author) {
  const firstAuthor = author.split(', ')[0];
  const exact = (await search({ title, author: firstAuthor })).find((doc) => doc.cover_i);
  if (exact) return exact.cover_i;
  await sleep(1000);
  const loose = (await search({ q: `${title} ${firstAuthor}` }))
    .find((doc) => doc.cover_i && doc.title && matchKey(doc.title) === matchKey(title));
  return loose?.cover_i ?? null;
}

/**
 * Sets `coverId` on every nomination: a number, or null when Open Library has no cover. A book whose lookup
 * failed is left without the field so the next run tries again. `lookup: false` only reuses known IDs;
 * `retryMissing` looks up books previously found to have no cover again.
 */
export async function addCovers(nominations, previous, { lookup = true, retryMissing = false } = {}) {
  const known = new Map(
    previous
      .filter((n) => 'coverId' in n && !(retryMissing && n.coverId === null))
      .map((n) => [bookKey(n), n.coverId]),
  );
  const pending = new Map();
  for (const n of nominations) {
    const key = bookKey(n);
    if (known.has(key)) n.coverId = known.get(key);
    else if (pending.has(key)) pending.get(key).push(n);
    else pending.set(key, [n]);
  }
  const reused = nominations.length - [...pending.values()].flat().length;
  if (!lookup) {
    console.log(`Covers: reused ${reused}, skipped ${pending.size} lookups`);
    return;
  }

  let found = 0;
  let failed = 0;
  let done = 0;
  for (const [, books] of pending) {
    try {
      const coverId = await lookupCover(books[0].title, books[0].author);
      for (const n of books) n.coverId = coverId;
      if (coverId) found++;
    } catch {
      failed++;
    }
    if (++done % 100 === 0) console.log(`Covers: looked up ${done} of ${pending.size}`);
    await sleep(1000);
  }
  console.log(`Covers: reused ${reused}, looked up ${pending.size}, found ${found}, failed ${failed} (retried next run)`);
}

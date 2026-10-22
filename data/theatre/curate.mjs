// Turns every scraped listing plus data/shows-curated.json into the shows the app gets, and the
// listings still waiting for a decision. Pure: no network or files, so it can be tested on its own.

// Fields that help curation but don't belong in the published file.
const pipelineOnly = ['source', 'dateText'];
const publishedFields = ['id', 'title', 'credits', 'venue', 'startDate', 'endDate', 'url', 'imageUrl', 'description', 'note'];
// Official London Theatre also lists productions we scrape from the venue itself; the venue's own page wins.
const aggregatorSource = 'olt';

const normalizeTitle = (title) => title.toLowerCase().replace(/^the\s+/, '').replace(/[^a-z0-9]+/g, '');

function publishable(show) {
  const out = {};
  for (const field of publishedFields) {
    if (show[field] !== undefined && show[field] !== null && !pipelineOnly.includes(field)) out[field] = show[field];
  }
  if (!('startDate' in out) && 'startDate' in show) out.startDate = show.startDate;
  return out;
}

const hasEnded = (show, today) => show.endDate != null && show.endDate < today;

function runsOverlap(a, b) {
  const start = (s) => s.startDate ?? '0000-00-00';
  const end = (s) => s.endDate ?? '9999-12-31';
  return start(a) <= end(b) && start(b) <= end(a);
}

/** Drops aggregator listings that duplicate a venue's own listing of the same production. */
function withoutAggregatorDuplicates(listings) {
  const own = listings.filter((l) => l.source !== aggregatorSource);
  return listings.filter((l) =>
    l.source !== aggregatorSource ||
    !own.some((o) => normalizeTitle(o.title) === normalizeTitle(l.title) && runsOverlap(o, l)));
}

function mentionsCompany(listing, company) {
  const escaped = company.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const pattern = new RegExp(`(^|[^\\p{L}])${escaped}($|[^\\p{L}])`, 'iu');
  return [listing.credits, listing.description, listing.title].some((text) => text && pattern.test(text));
}

const byDateThenTitle = (a, b) =>
  (a.startDate ?? '').localeCompare(b.startDate ?? '') || a.title.localeCompare(b.title) || a.id.localeCompare(b.id);

/**
 * @param {{listings: object[], curated: {include, exclude, companies, manual}, previous: object[], today: string}} input
 *   previous is the shows array of the last published file.
 * @returns {{shows: object[], candidates: object[], errors: string[], warnings: string[]}}
 */
export function curate({ listings, curated, previous, today }) {
  const errors = [];
  const warnings = [];
  const { include = [], exclude = [], companies = [], manual = [] } = curated;

  const unique = withoutAggregatorDuplicates(listings);
  const byId = new Map(unique.map((l) => [l.id, l]));
  const previousById = new Map(previous.map((s) => [s.id, s]));
  const excluded = new Set(exclude);
  const includeById = new Map(include.map((entry) => [entry.id, entry]));

  for (const id of includeById.keys()) {
    if (excluded.has(id)) errors.push(`${id} is both included and excluded in shows-curated.json`);
  }

  const chosen = [];
  for (const [id, entry] of includeById) {
    if (excluded.has(id)) continue;
    const scraped = byId.get(id);
    if (scraped) {
      chosen.push({ ...scraped, ...entry });
      continue;
    }
    const earlier = previousById.get(id);
    if (earlier && !hasEnded(earlier, today)) {
      warnings.push(`${id} is no longer on its venue's page; keeping the previous copy (ends ${earlier.endDate ?? 'unknown'})`);
      chosen.push({ ...earlier, ...entry });
    } else if (!earlier) {
      errors.push(`${id} is included but no venue lists it and it isn't in the previous shows.json`);
    }
  }

  for (const listing of unique) {
    if (includeById.has(listing.id) || excluded.has(listing.id)) continue;
    const company = companies.find((c) => mentionsCompany(listing, c));
    if (company) chosen.push({ ...listing, note: company });
  }

  for (const entry of manual) {
    const missing = ['id', 'title', 'venue', 'url'].filter((field) => !entry[field]);
    if (!entry.startDate && !entry.endDate) missing.push('startDate or endDate');
    if (missing.length > 0) {
      errors.push(`manual entry ${entry.id ?? '(no id)'} is missing ${missing.join(', ')}`);
      continue;
    }
    if (byId.has(entry.id)) {
      errors.push(`manual entry ${entry.id} has the same ID as a scraped listing; include it instead`);
      continue;
    }
    chosen.push(entry);
  }

  const seen = new Set();
  const shows = [];
  for (const show of chosen) {
    if (seen.has(show.id)) {
      errors.push(`${show.id} appears twice`);
      continue;
    }
    seen.add(show.id);
    if (!show.startDate && !show.endDate) {
      errors.push(`${show.id}: couldn't read its dates ("${show.dateText ?? ''}"); add startDate/endDate to its include entry`);
      continue;
    }
    if (show.startDate && show.endDate && show.endDate < show.startDate) {
      errors.push(`${show.id} ends (${show.endDate}) before it starts (${show.startDate})`);
      continue;
    }
    if (hasEnded(show, today)) continue;
    shows.push(publishable(show));
  }
  shows.sort(byDateThenTitle);

  const decided = new Set([...seen, ...excluded]);
  const candidates = unique
    .filter((l) => !decided.has(l.id) && !hasEnded(l, today))
    .sort((a, b) => a.id.localeCompare(b.id));

  return { shows, candidates, errors, warnings };
}

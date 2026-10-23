import { decodeEntities, fetchPage, slugOf, text } from '../html.mjs';

// Covers the West End, where a Black-led show may transfer. Curation drops its copy of any production
// a venue source also lists.
export const source = 'olt';
export const page = 'https://officiallondontheatre.com/shows/';

export function parse(html) {
  const events = [];
  for (const [, json] of html.matchAll(/<script[^>]*application\/ld\+json[^>]*>([\s\S]*?)<\/script>/g)) {
    let data;
    try {
      data = JSON.parse(json);
    } catch {
      continue;
    }
    for (const item of [data].flat()) if (item?.['@type'] === 'TheaterEvent' && item.url) events.push(item);
  }
  return events.map((event) => ({
    id: `${source}:${slugOf(event.url)}`,
    source,
    title: decodeEntities(event.name),
    credits: null,
    description: text(event.description),
    venue: event.location?.title ?? null,
    dateText: `${event.startDate ?? ''} – ${event.endDate ?? ''}`,
    // The date part is the listed day; OLT stamps every event with the same arbitrary time.
    startDate: event.startDate?.slice(0, 10) ?? null,
    endDate: event.endDate?.slice(0, 10) ?? null,
    url: event.url,
    imageUrl: [event.image].flat()[0] ?? null,
  }));
}

export const fetchListings = async () => parse(await fetchPage(page));

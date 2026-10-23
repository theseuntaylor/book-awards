import { parseDateRange } from '../dates.mjs';
import { attribute, cards, fetchPage, inner, slugOf, text } from '../html.mjs';

export const source = 'royal-court';
export const page = 'https://royalcourttheatre.com/whats-on/';

// Cards carry machine-readable <time datetime> for the first and last performance; the printed range
// ("Wed 21 – Sat 24 Oct") is the fallback.
export function parse(html, today) {
  return cards(html, 'c-media--event').map((card) => {
    const url = /goToUrl\('([^']+)'/.exec(card)?.[1];
    if (!url) return null;
    const dateText = text(inner(card, 'c-media__datetime'));
    const times = [...card.matchAll(/<time datetime="(\d{4}-\d{2}-\d{2})T[^"]*"\s*itemprop="(startDate|endDate)"/g)];
    const fromTimes = Object.fromEntries(times.map(([, date, kind]) => [kind, date]));
    const parsed = parseDateRange(dateText ?? '', today);
    return {
      id: `${source}:${slugOf(url)}`,
      source,
      title: text(inner(card, 'c-media__title')),
      credits: text(inner(card, 'c-media__posttitle')),
      description: null,
      venue: 'Royal Court Theatre',
      dateText,
      startDate: fromTimes.startDate ?? parsed?.startDate ?? null,
      endDate: fromTimes.endDate ?? fromTimes.startDate ?? parsed?.endDate ?? null,
      url,
      // The CDN signs each sized URL (the s= parameter); without its query the image is refused.
      imageUrl: attribute(card, 'src', 'o-image__img'),
    };
  }).filter(Boolean);
}

export const fetchListings = async (today) => parse(await fetchPage(page), today);

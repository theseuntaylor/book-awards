import { parseDateRange } from '../dates.mjs';
import { attribute, cards, fetchPage, inner, slugOf, text } from '../html.mjs';

export const source = 'almeida';
export const page = 'https://almeida.co.uk/whats-on/';

// Cards give only the title and date range; credits are on each show's own page.
export function parse(html, today) {
  return cards(html, 'class="c-event-card"').map((card) => {
    const url = attribute(card, 'href', 'c-event-card__permalink');
    if (!url) return null;
    const dateText = text(inner(card, 'c-event-card__daterange'));
    const parsed = parseDateRange(dateText ?? '', today);
    return {
      id: `${source}:${slugOf(url)}`,
      source,
      title: text(inner(card, 'c-event-card__title')),
      credits: null,
      description: null,
      venue: 'Almeida Theatre',
      dateText,
      startDate: parsed?.startDate ?? null,
      endDate: parsed?.endDate ?? null,
      url,
      // The card's src is a 16x10px lazy-load placeholder; ask the CDN for a usable size instead.
      imageUrl: attribute(card, 'src', 'c-event-card__image')?.replace(/\?.*$/, '?resize=640%2C400') ?? null,
    };
  }).filter(Boolean);
}

export const fetchListings = async (today) => parse(await fetchPage(page), today);

import { parseDateRange } from '../dates.mjs';
import { attribute, cards, fetchPage, inner, slugOf, text } from '../html.mjs';

export const source = 'bush';
export const page = 'https://www.bushtheatre.co.uk/whats-on/';

// Each card's meta block is three paragraphs: credits ("By Titas Halder"), dates, and the space.
export function parse(html, today) {
  return cards(html, 'class="events_item"').map((card) => {
    const url = attribute(card, 'href', 'events_item__link');
    if (!url) return null;
    const meta = [...(inner(card, 'events_item__meta') ?? '').matchAll(/<p[^>]*>([\s\S]*?)<\/p>/g)].map(([, p]) => text(p));
    const [credits = null, dateText = null, space = null] = meta;
    const parsed = parseDateRange(dateText ?? '', today);
    return {
      id: `${source}:${slugOf(url)}`,
      source,
      title: text(/<h3[^>]*>([\s\S]*?)<\/h3>/.exec(card)?.[1]),
      credits,
      description: null,
      venue: space ? `Bush Theatre (${space.replace(/\s*\(Bush Theatre'?s? [^)]*\)/i, '')})` : 'Bush Theatre',
      dateText,
      startDate: parsed?.startDate ?? null,
      endDate: parsed?.endDate ?? null,
      url,
      imageUrl: attribute(card, 'src', 'events_item__image'),
    };
  }).filter(Boolean);
}

export const fetchListings = async (today) => parse(await fetchPage(page), today);

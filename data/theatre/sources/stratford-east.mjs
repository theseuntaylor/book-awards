import { parseDateRange } from '../dates.mjs';
import { attribute, cards, fetchPage, inner, slugOf, text } from '../html.mjs';

export const source = 'stratford-east';
export const page = 'https://www.stratfordeast.com/whats-on';

// The date range is the label of the card's booking button ("Tue 20th Oct - <br /> Sat 31st Oct").
export function parse(html, today) {
  return cards(html, 'class="c-card c-card--event"').map((card) => {
    const url = attribute(card, 'href', 'c-card__title');
    if (!url) return null;
    const dateText = text(inner(card, 'c-button__label'));
    const parsed = parseDateRange(dateText ?? '', today);
    const category = text(inner(card, 'c-card__category'));
    return {
      id: `${source}:${slugOf(url)}`,
      source,
      title: text(inner(card, 'c-card__title-text')),
      credits: null,
      description: category,
      venue: 'Stratford East',
      dateText,
      startDate: parsed?.startDate ?? null,
      endDate: parsed?.endDate ?? null,
      url,
      // Signed like the Royal Court's: keep the query.
      imageUrl: attribute(card, 'src', 'c-image'),
    };
  }).filter(Boolean);
}

export const fetchListings = async (today) => parse(await fetchPage(page), today);

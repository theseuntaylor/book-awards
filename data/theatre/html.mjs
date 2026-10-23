// Small helpers for reading venue pages without an HTML parser dependency. Each source splits its
// "what's on" page into show cards on a class name, then reads fields from inside each card.
import { userAgent } from '../wikitext.mjs';

const lastRequest = new Map();

/** Fetches a page as this project (no browser disguise), at most once a second per host. */
export async function fetchPage(url) {
  const host = new URL(url).host;
  const wait = (lastRequest.get(host) ?? 0) + 1000 - Date.now();
  if (wait > 0) await new Promise((resolve) => setTimeout(resolve, wait));
  lastRequest.set(host, Date.now());
  const response = await fetch(url, { headers: { 'User-Agent': userAgent, Accept: 'text/html' } });
  if (!response.ok) throw new Error(`${url} answered ${response.status}`);
  return response.text();
}

const entities = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', ndash: '–', mdash: '—', rsquo: '’', lsquo: '‘', ldquo: '“', rdquo: '”', hellip: '…' };

export function decodeEntities(text) {
  return text.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/gi, (match, name) => {
    if (name[0] === '#') {
      const code = name[1].toLowerCase() === 'x' ? parseInt(name.slice(2), 16) : Number(name.slice(1));
      return String.fromCodePoint(code);
    }
    return entities[name.toLowerCase()] ?? match;
  });
}

/** Visible text of an HTML fragment, on one line. `<br>` becomes a space. */
export function text(fragment) {
  if (fragment == null) return null;
  const value = decodeEntities(fragment.replace(/<br\s*\/?>/gi, ' ').replace(/<[^>]+>/g, ' ')).replace(/\s+/g, ' ').trim();
  return value === '' ? null : value;
}

/** The pieces of `html` that start at each occurrence of `marker`, up to the next one. */
export function cards(html, marker) {
  return html.split(marker).slice(1);
}

/** Inner HTML of the first element whose class attribute contains `className`. */
export function inner(fragment, className) {
  const pattern = new RegExp(`<([a-z0-9-]+)[^>]*class="[^"]*\\b${className}\\b[^"]*"[^>]*>([\\s\\S]*?)</\\1>`, 'i');
  return pattern.exec(fragment)?.[2] ?? null;
}

export function attribute(fragment, name, after = '') {
  const pattern = new RegExp(`${after}[\\s\\S]*?\\b${name}="([^"]*)"`, 'i');
  const value = pattern.exec(fragment)?.[1];
  return value == null ? null : decodeEntities(value);
}

/** The last path segment of a show URL, used in stable IDs (`venue:slug`). */
export function slugOf(url) {
  return new URL(url).pathname.split('/').filter(Boolean).pop();
}

// Reads tables out of Wikipedia wikitext. Only handles what the prize pages use: rowspans, header rows,
// per-cell attributes, links, {{sortname}}, and templates/refs that should be dropped.

export const userAgent = 'book-awards/0.1 (https://github.com/theseuntaylor/book-awards)';

export async function fetchWikitext(page) {
  const url = `https://en.wikipedia.org/w/index.php?title=${encodeURIComponent(page)}&action=raw`;
  const response = await fetch(url, { headers: { 'User-Agent': userAgent } });
  if (!response.ok) throw new Error(`Wikipedia fetch failed for ${page}: ${response.status}`);
  return response.text();
}

/** Splits at `separator` only where it isn't inside [[links]] or {{templates}}. */
function splitTopLevel(text, separator) {
  const parts = [];
  let depth = 0;
  let start = 0;
  for (let i = 0; i < text.length; i++) {
    const pair = text.slice(i, i + 2);
    if (pair === '[[' || pair === '{{') { depth++; i++; continue; }
    if ((pair === ']]' || pair === '}}') && depth > 0) { depth--; i++; continue; }
    if (depth === 0 && text.startsWith(separator, i)) {
      parts.push(text.slice(start, i));
      start = i + separator.length;
      i += separator.length - 1;
    }
  }
  parts.push(text.slice(start));
  return parts;
}

/** `style="…" rowspan=3 |content` → { rowspan: 3, content }. Only treats the prefix as attributes if it looks like them. */
function parseCell(raw) {
  const [first, ...rest] = splitTopLevel(raw, '|');
  const hasAttributes = rest.length > 0 && /^\s*[a-z-]+\s*=/i.test(first);
  const attributes = hasAttributes ? first : '';
  const content = hasAttributes ? rest.join('|') : raw;
  const rowspan = Number(attributes.match(/rowspan\s*=\s*"?(\d+)/i)?.[1] ?? 1);
  return { rowspan, content: content.trim() };
}

function cellsOfLine(line) {
  const marker = line[0];
  return splitTopLevel(line.slice(1), marker === '!' ? '!!' : '||').map(parseCell);
}

const count = (text, token) => text.split(token).length - 1;
const isOpen = (text) => count(text, '{{') > count(text, '}}') || count(text, '[[') > count(text, ']]');

/**
 * Returns each table as { headers: [cleaned header text], rows: [[raw cell content]] }, with rowspans
 * expanded so every row has a value in every column it spans. Rows with no cells of their own are dropped.
 */
export function parseTables(wikitext) {
  const tables = [];
  let table = null;
  let row = null;
  let pending = [];

  const finishRow = () => {
    if (!row || row.length === 0) { row = null; return; }
    const expanded = [];
    let own = 0;
    for (let column = 0; own < row.length || pending[column]?.remaining > 0; column++) {
      const carried = pending[column];
      if (carried?.remaining > 0) {
        expanded.push(carried.content);
        carried.remaining--;
      } else {
        const cell = row[own++];
        expanded.push(cell.content);
        pending[column] = cell.rowspan > 1 ? { content: cell.content, remaining: cell.rowspan - 1 } : undefined;
      }
    }
    if (table.headers === null) table.headers = expanded.map(cleanCell);
    else table.rows.push(expanded);
    row = null;
  };

  for (const line of wikitext.split('\n')) {
    if (line.startsWith('{|') && !(row?.length && isOpen(row.at(-1).content))) { table = { headers: null, rows: [] }; row = null; pending = []; continue; }
    if (!table) continue;
    // Multi-line citation templates put `| url=…` lines inside a cell; they aren't new cells.
    if (row?.length && isOpen(row.at(-1).content)) { row.at(-1).content += `\n${line}`; continue; }
    if (line.startsWith('|}')) { finishRow(); tables.push(table); table = null; continue; }
    if (line.startsWith('|+')) continue;
    if (line.startsWith('|-')) { finishRow(); row = []; continue; }
    if (line.startsWith('|') || line.startsWith('!')) {
      row ??= [];
      row.push(...cellsOfLine(line));
    } else if (row?.length) {
      // A continuation line (e.g. a bulleted judges list) belongs to the previous cell.
      row.at(-1).content += `\n${line}`;
    }
  }
  return tables;
}

/** Finds a template's closing braces, allowing nested templates. */
function templateEnd(text, start) {
  let depth = 0;
  for (let i = start; i < text.length - 1; i++) {
    if (text.startsWith('{{', i)) { depth++; i++; } else if (text.startsWith('}}', i)) { depth--; i++; if (depth === 0) return i + 1; }
  }
  return text.length;
}

function templateParams(inner) {
  const [name, ...params] = splitTopLevel(inner, '|');
  const named = {};
  const positional = [];
  for (const param of params) {
    const match = param.match(/^\s*([a-z0-9]+)\s*=(.*)$/is);
    if (match) named[match[1].toLowerCase()] = match[2].trim(); else positional.push(param.trim());
  }
  return { name: name.trim().toLowerCase(), named, positional };
}

/** {{sortname|First|Last}}, {{sortname|first=…|last=…}}, {{sortname|2=Guardian|1=The}} → display text. */
function sortnameText({ named, positional }) {
  const first = named.first ?? named['1'] ?? positional[0] ?? '';
  const last = named.last ?? named['2'] ?? positional[1] ?? '';
  return `${first} ${last}`.trim();
}

/** {{sort|key|display}} → display. Some pages misuse it as {{sort|2=Rector of Justin|1=The}}, meaning "The Rector of Justin". */
function sortText(template) {
  const { named, positional } = template;
  if (/^(the|a|an)$/i.test(named['1'] ?? '') && named['2']) return sortnameText(template);
  return named['2'] ?? positional[1] ?? named['1'] ?? positional[0] ?? '';
}

/** Drops refs and templates, keeping only what {{sortname}}, {{sort}} and {{nowrap}} display. */
function stripTemplates(text) {
  let out = text.replace(/<ref[^>]*\/>/gi, '').replace(/<ref[^>]*>[\s\S]*?<\/ref>/gi, '');
  let result = '';
  for (let i = 0; i < out.length; ) {
    if (!out.startsWith('{{', i)) { result += out[i++]; continue; }
    const end = templateEnd(out, i);
    const template = templateParams(out.slice(i + 2, end - 2));
    if (template.name === 'sortname') result += sortnameText(template);
    else if (template.name === 'sort') result += sortText(template);
    else if (template.name === 'nowrap') result += template.positional[0] ?? '';
    i = end;
  }
  return result;
}

export function cleanCell(raw) {
  return stripTemplates(raw)
    .replace(/\[\[(?:[^\]|]*\|)?([^\]]*)\]\]/g, '$1')
    .replace(/'{2,}/g, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;|&apos;/g, "'")
    .replace(/\s+/g, ' ')
    .trim();
}

export const isBold = (raw) => stripTemplates(raw).includes("'''");

/** The article a cell links to: the first [[link]], or a linking {{sortname}}. Null for plain text. */
export function linkTarget(raw) {
  const text = raw.replace(/<ref[^>]*\/>/gi, '').replace(/<ref[^>]*>[\s\S]*?<\/ref>/gi, '');
  for (let i = 0; i < text.length; ) {
    if (text.startsWith('[[', i)) {
      const target = text.slice(i + 2, text.indexOf(']]', i)).split('|')[0].trim();
      return target || null;
    }
    if (text.startsWith('{{', i)) {
      const end = templateEnd(text, i);
      const template = templateParams(text.slice(i + 2, end - 2));
      if (template.name === 'sortname' && template.named.nolink === undefined) return template.named.link ?? sortnameText(template);
      i = end;
      continue;
    }
    i++;
  }
  return null;
}

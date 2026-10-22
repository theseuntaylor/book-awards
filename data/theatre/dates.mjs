// Reads the date ranges theatres print on "what's on" pages ("Wed 21 – Sat 24 Oct", "Until 24 October 2026",
// "Tue 20th Oct - Sat 31st Oct") into ISO dates. Returns null for anything it isn't sure of, so a listing
// with unreadable dates is flagged instead of shown with wrong ones.

const months = {
  jan: 1, january: 1, feb: 2, february: 2, mar: 3, march: 3, apr: 4, april: 4, may: 5, jun: 6, june: 6,
  jul: 7, july: 7, aug: 8, august: 8, sep: 9, sept: 9, september: 9, oct: 10, october: 10,
  nov: 11, november: 11, dec: 12, december: 12,
};

const weekday = String.raw`(?:mon|tue|wed|thu|fri|sat|sun)[a-z]*\.?`;
const monthName = `(?:${Object.keys(months).sort((a, b) => b.length - a.length).join('|')})`;
// Day, optional month, optional year. The weekday is optional and dropped.
const datePart = String.raw`(?:${weekday}\s+)?(\d{1,2})(?:st|nd|rd|th)?(?:\s+(${monthName})\.?)?(?:\s+(\d{4}))?`;
const rangePattern = new RegExp(String.raw`^${datePart}\s*(?:-|–|—|to)\s*${datePart}$`, 'i');
const singlePattern = new RegExp(String.raw`^(until|from)?\s*${datePart}$`, 'i');

function clean(text) {
  return text
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/&ndash;/g, '–').replace(/&mdash;/g, '—').replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function iso(year, month, day) {
  // Built from numbers, never through Date, so the machine's time zone can't move it a day.
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  if (month < 1 || month > 12 || day < 1 || day > daysInMonth) return null;
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

const dayNumber = (isoDate) => Date.UTC(...isoDate.split('-').map((n, i) => (i === 1 ? n - 1 : Number(n)))) / 86_400_000;

/** The year that puts month/day nearest to today. */
function nearestYear(month, day, today) {
  const thisYear = Number(today.slice(0, 4));
  const candidates = [thisYear - 1, thisYear, thisYear + 1]
    .map((year) => ({ year, date: iso(year, month, day) }))
    .filter((c) => c.date);
  if (candidates.length === 0) return null;
  candidates.sort((a, b) => Math.abs(dayNumber(a.date) - dayNumber(today)) - Math.abs(dayNumber(b.date) - dayNumber(today)));
  return candidates[0].year;
}

const monthOf = (name) => (name ? months[name.toLowerCase()] : undefined);

/**
 * @param {string} text as printed by the venue
 * @param {string} today ISO date, used only to choose a year when the text gives none
 * @returns {{startDate: string|null, endDate: string|null} | null} startDate null means already running
 *   ("Until …"); endDate null means no end announced ("From …").
 */
export function parseDateRange(text, today) {
  const value = clean(text);

  const r = rangePattern.exec(value);
  if (r) {
    const [, startDay, startMonthName, startYearText, endDay, endMonthName, endYearText] = r;
    const endMonth = monthOf(endMonthName);
    if (!endMonth) return null;
    const startMonth = monthOf(startMonthName) ?? endMonth;
    let endYear = endYearText ? Number(endYearText) : undefined;
    let startYear = startYearText ? Number(startYearText) : undefined;

    if (startYear === undefined && endYear !== undefined) {
      startYear = startMonth > endMonth ? endYear - 1 : endYear;
    }
    if (startYear === undefined) {
      startYear = nearestYear(startMonth, Number(startDay), today);
      if (startYear === null) return null;
    }
    const startDate = iso(startYear, startMonth, Number(startDay));
    if (!startDate) return null;

    let crossesYear = false;
    if (endYear === undefined) {
      endYear = startYear;
      crossesYear = true;
    }
    let endDate = iso(endYear, endMonth, Number(endDay));
    if (!endDate) return null;
    if (endDate < startDate) {
      // Only an inferred end year may roll over; stated years that contradict each other are an error.
      if (!crossesYear) return null;
      endDate = iso(endYear + 1, endMonth, Number(endDay));
      if (!endDate) return null;
    }
    return { startDate, endDate };
  }

  const s = singlePattern.exec(value);
  if (s) {
    const [, keyword, day, monthText, yearText] = s;
    const month = monthOf(monthText);
    if (!month) return null;
    const year = yearText ? Number(yearText) : nearestYear(month, Number(day), today);
    if (year === null) return null;
    const date = iso(year, month, Number(day));
    if (!date) return null;
    const kind = keyword?.toLowerCase();
    if (kind === 'until') return { startDate: null, endDate: date };
    if (kind === 'from') return { startDate: date, endDate: null };
    return { startDate: date, endDate: date };
  }

  return null;
}

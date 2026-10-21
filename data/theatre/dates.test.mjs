// Run: node --test data/theatre/
// How parseDateRange can fail, one test each: unknown text must give null rather than a guess; every
// separator and weekday/ordinal form venues use; a missing month or year on the start; no year at all;
// a run crossing New Year; "Until" (no start) and "From" (no end); impossible dates; and dates shifting
// a day through time zones.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseDateRange } from './dates.mjs';

const today = '2026-10-20';
const range = (text, at = today) => parseDateRange(text, at);

test('returns null for text that is not a date range', () => {
  assert.equal(range(''), null);
  assert.equal(range('Sold Out'), null);
  assert.equal(range('Coming soon'), null);
  assert.equal(range('Dates to be announced'), null);
});

test('reads a range with both months and the year at the end', () => {
  assert.deepEqual(range('Fri 16 - Fri 23 Oct 2026'), { startDate: '2026-10-16', endDate: '2026-10-23' });
  assert.deepEqual(range('Tue 8 Sep – Sat 31 Oct 2026'), { startDate: '2026-09-08', endDate: '2026-10-31' });
});

test('accepts hyphen, en dash, em dash, "to" and HTML entities as separators', () => {
  const expected = { startDate: '2026-10-28', endDate: '2026-11-11' };
  assert.deepEqual(range('28 Oct - 11 Nov'), expected);
  assert.deepEqual(range('28 Oct – 11 Nov'), expected);
  assert.deepEqual(range('28 Oct — 11 Nov'), expected);
  assert.deepEqual(range('28 Oct to 11 Nov'), expected);
  assert.deepEqual(range('28 Oct&ndash;11 Nov'), expected);
  assert.deepEqual(range('28 Oct-11 Nov'), expected);
});

test('ignores weekdays, ordinals, line breaks and extra spaces', () => {
  assert.deepEqual(range('Tue 20th Oct - <br /> Sat 31st Oct'), { startDate: '2026-10-20', endDate: '2026-10-31' });
  assert.deepEqual(range('  Wednesday 21st   October  –  Saturday 7th November '),
    { startDate: '2026-10-21', endDate: '2026-11-07' });
});

test('takes the start month from the end when the start has only a day', () => {
  assert.deepEqual(range('Wed 21 – Sat 24 Oct'), { startDate: '2026-10-21', endDate: '2026-10-24' });
});

test('reads full and abbreviated month names, including Sept', () => {
  assert.deepEqual(range('4 February — 13 March'), { startDate: '2027-02-04', endDate: '2027-03-13' });
  assert.deepEqual(range('1 Sept – 3 Oct 2027'), { startDate: '2027-09-01', endDate: '2027-10-03' });
});

test('with no year, picks the year that puts the start nearest today', () => {
  // October 2026: a September start is last month, a February start is next year.
  assert.deepEqual(range('Tue 8 Sep – Sat 31 Oct'), { startDate: '2026-09-08', endDate: '2026-10-31' });
  assert.deepEqual(range('4 Feb – 13 Mar'), { startDate: '2027-02-04', endDate: '2027-03-13' });
  // In January, a December start was last month.
  assert.deepEqual(range('3 Dec – 10 Jan', '2027-01-05'), { startDate: '2026-12-03', endDate: '2027-01-10' });
});

test('moves the end into the next year when a run crosses New Year', () => {
  assert.deepEqual(range('Thu 3 Dec – Sat 9 Jan'), { startDate: '2026-12-03', endDate: '2027-01-09' });
  assert.deepEqual(range('Sat 14 Nov 2026 – Sat 16 Jan 2027'), { startDate: '2026-11-14', endDate: '2027-01-16' });
});

test('with only the end year given, the start takes that year or the one before', () => {
  assert.deepEqual(range('Thu 14 Jan – Sat 13 Mar 2027'), { startDate: '2027-01-14', endDate: '2027-03-13' });
  assert.deepEqual(range('Sat 21 Nov - Sat 17 Apr 2027'), { startDate: '2026-11-21', endDate: '2027-04-17' });
});

test('"Until" means already running: no start date', () => {
  assert.deepEqual(range('Until 24 Oct'), { startDate: null, endDate: '2026-10-24' });
  assert.deepEqual(range('Until 24 October 2026'), { startDate: null, endDate: '2026-10-24' });
});

test('"From" means no end date is known yet', () => {
  assert.deepEqual(range('From 5 Nov'), { startDate: '2026-11-05', endDate: null });
});

test('a single date is a one-day run', () => {
  assert.deepEqual(range('Sun 6 Dec'), { startDate: '2026-12-06', endDate: '2026-12-06' });
  assert.deepEqual(range('Thu 3 Dec 2026'), { startDate: '2026-12-03', endDate: '2026-12-03' });
});

test('rejects impossible dates instead of rolling them over', () => {
  assert.equal(range('31 Feb – 3 Mar 2027'), null);
  assert.equal(range('0 Oct'), null);
  assert.equal(range('32 Oct'), null);
});

test('rejects a range whose end is before its start in the same stated year', () => {
  assert.equal(range('24 Oct 2026 – 3 Oct 2026'), null);
});

test('does not shift dates through the local time zone', () => {
  const saved = process.env.TZ;
  try {
    process.env.TZ = 'Pacific/Kiritimati';
    assert.deepEqual(range('1 Jan – 2 Jan 2027'), { startDate: '2027-01-01', endDate: '2027-01-02' });
  } finally {
    process.env.TZ = saved;
  }
});

// Run: node --test data/theatre/
// How curation can fail, one test each: a show you approved goes missing from its venue's page (sold out)
// or for good; an exclusion loses to a company match; one ID both included and excluded; a company name
// matching inside another word; manual and scraped entries clashing; your overrides not applied; ended
// shows leaking into the app or the candidates list; unreadable or backwards dates on an approved show; the
// same production listed by its venue and by Official London Theatre; and output order changing run to run.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { curate } from './curate.mjs';

const today = '2026-10-20';

const listing = (id, fields = {}) => ({
  id,
  source: id.split(':')[0],
  title: id.split(':')[1],
  credits: null,
  description: null,
  venue: 'Royal Court Theatre',
  dateText: '1 Nov – 30 Nov',
  startDate: '2026-11-01',
  endDate: '2026-11-30',
  url: `https://example.org/${id}`,
  imageUrl: null,
  ...fields,
});

const curated = (fields = {}) => ({ include: [], exclude: [], companies: [], manual: [], ...fields });
const run = ({ listings = [], rules = curated(), previous = [] } = {}) => curate({ listings, curated: rules, previous, today });
const ids = (shows) => shows.map((s) => s.id);

test('an included listing becomes a show carrying its note', () => {
  const result = run({
    listings: [listing('royal-court:afronauts')],
    rules: curated({ include: [{ id: 'royal-court:afronauts', note: 'Written by Ryan Calais Cameron' }] }),
  });
  assert.deepEqual(result.errors, []);
  assert.equal(result.shows.length, 1);
  assert.equal(result.shows[0].note, 'Written by Ryan Calais Cameron');
  assert.equal(result.shows[0].source, undefined, 'pipeline-only fields stay out of the published file');
  assert.equal(result.shows[0].dateText, undefined);
});

test('listings not included are left out of the shows', () => {
  const result = run({ listings: [listing('bush:darkling')] });
  assert.deepEqual(result.shows, []);
});

test('an include entry can override scraped fields, such as a missing end date', () => {
  const result = run({
    listings: [listing('nt:story', { startDate: '2026-11-05', endDate: null, dateText: 'From 5 Nov' })],
    rules: curated({ include: [{ id: 'nt:story', endDate: '2027-01-10' }] }),
  });
  assert.deepEqual(result.errors, []);
  assert.equal(result.shows[0].endDate, '2027-01-10');
});

test('a show missing from its venue page is carried over from the previous file while it is still running', () => {
  const previous = [{ id: 'royal-court:afronauts', title: 'The Afronauts', venue: 'Royal Court Theatre',
    startDate: '2026-11-14', endDate: '2026-12-19', url: 'https://example.org/a' }];
  const result = run({ rules: curated({ include: [{ id: 'royal-court:afronauts' }] }), previous });
  assert.deepEqual(result.errors, []);
  assert.deepEqual(ids(result.shows), ['royal-court:afronauts']);
  assert.equal(result.warnings.length, 1);
  assert.match(result.warnings[0], /royal-court:afronauts/);
});

test('an included show found nowhere is an error', () => {
  const result = run({ rules: curated({ include: [{ id: 'royal-court:gone' }] }) });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /royal-court:gone/);
});

test('an included show that ended and left the venue page is dropped quietly', () => {
  const previous = [{ id: 'bush:old', title: 'Old', venue: 'Bush Theatre', startDate: '2026-09-01',
    endDate: '2026-10-01', url: 'https://example.org/o' }];
  const result = run({ rules: curated({ include: [{ id: 'bush:old' }] }), previous });
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.shows, []);
});

test('shows that have ended are never published, even when included', () => {
  const result = run({
    listings: [listing('bush:ended', { startDate: '2026-09-01', endDate: '2026-10-19' }),
      listing('bush:last-night', { startDate: '2026-09-01', endDate: '2026-10-20' })],
    rules: curated({ include: [{ id: 'bush:ended' }, { id: 'bush:last-night' }] }),
  });
  assert.deepEqual(ids(result.shows), ['bush:last-night']);
});

test('a company name in the credits or description includes the show, matched as whole words', () => {
  const result = run({
    listings: [
      listing('royal-court:co-pro', { credits: 'A Talawa Theatre Company and Royal Court co-production' }),
      listing('nt:desc', { description: 'Presented with tiata fahodzi.' }),
      listing('bush:other', { credits: 'By Talawasa Smith' }),
    ],
    rules: curated({ companies: ['Talawa', 'Tiata Fahodzi'] }),
  });
  assert.deepEqual(ids(result.shows).sort(), ['nt:desc', 'royal-court:co-pro']);
  assert.equal(result.shows.find((s) => s.id === 'royal-court:co-pro').note, 'Talawa');
});

test('an exclusion beats a company match', () => {
  const result = run({
    listings: [listing('royal-court:talk', { credits: 'In conversation with Talawa' })],
    rules: curated({ companies: ['Talawa'], exclude: ['royal-court:talk'] }),
  });
  assert.deepEqual(result.shows, []);
});

test('the same ID both included and excluded is an error', () => {
  const result = run({
    listings: [listing('bush:darkling')],
    rules: curated({ include: [{ id: 'bush:darkling' }], exclude: ['bush:darkling'] }),
  });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /bush:darkling/);
});

test('manual entries are published as written', () => {
  const manual = { id: 'talawa:tambo', title: 'Tambo & Bones', venue: 'Stratford East',
    startDate: '2026-11-01', endDate: '2026-11-21', url: 'https://example.org/t' };
  const result = run({ rules: curated({ manual: [manual] }) });
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.shows, [manual]);
});

test('a manual entry missing required fields is an error', () => {
  const result = run({ rules: curated({ manual: [{ id: 'talawa:x', title: 'X', endDate: '2026-11-21' }] }) });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /talawa:x/);
});

test('a manual entry with the same ID as a scraped listing is an error', () => {
  const manual = { id: 'bush:darkling', title: 'Darkling', venue: 'Bush Theatre', startDate: null,
    endDate: '2026-10-24', url: 'https://example.org/d' };
  const result = run({ listings: [listing('bush:darkling')], rules: curated({ manual: [manual] }) });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /bush:darkling/);
});

test('an included show with unreadable dates is an error that quotes the text', () => {
  const result = run({
    listings: [listing('almeida:odd', { startDate: null, endDate: null, dateText: 'Spring 2027' })],
    rules: curated({ include: [{ id: 'almeida:odd' }] }),
  });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /Spring 2027/);
});

test('an included show ending before it starts is an error', () => {
  const result = run({
    listings: [listing('bush:backwards')],
    rules: curated({ include: [{ id: 'bush:backwards', startDate: '2026-12-01', endDate: '2026-11-01' }] }),
  });
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0], /bush:backwards/);
});

test('a production also listed by Official London Theatre is kept once, from its own venue', () => {
  const result = run({
    listings: [
      listing('olt:the-story', { title: 'The Story', venue: 'National Theatre', source: 'olt' }),
      listing('nt:the-story', { title: 'The Story', venue: 'Olivier Theatre, National Theatre', source: 'nt' }),
    ],
  });
  assert.deepEqual(result.candidates.map((c) => c.id), ['nt:the-story']);
});

test('same-titled shows whose runs do not overlap are both kept', () => {
  const result = run({
    listings: [
      listing('olt:hamlet', { title: 'Hamlet', source: 'olt', startDate: '2027-03-01', endDate: '2027-04-01' }),
      listing('nt:hamlet', { title: 'Hamlet', source: 'nt' }),
    ],
  });
  assert.deepEqual(result.candidates.map((c) => c.id).sort(), ['nt:hamlet', 'olt:hamlet']);
});

test('candidates are running listings you have not decided on yet', () => {
  const result = run({
    listings: [
      listing('bush:new'),
      listing('bush:approved'),
      listing('bush:rejected'),
      listing('bush:finished', { startDate: '2026-09-01', endDate: '2026-10-01' }),
      listing('bush:undated', { startDate: null, endDate: null, dateText: 'TBC' }),
    ],
    rules: curated({ include: [{ id: 'bush:approved' }], exclude: ['bush:rejected'] }),
  });
  assert.deepEqual(result.candidates.map((c) => c.id), ['bush:new', 'bush:undated']);
});

test('shows come out in date order, then by title, however the sources were ordered', () => {
  const listings = [
    listing('a:late', { title: 'Zed', startDate: '2026-12-01', endDate: '2026-12-20' }),
    listing('a:running', { title: 'Running', startDate: null, endDate: '2026-10-30' }),
    listing('a:soon-b', { title: 'Beta', startDate: '2026-11-01' }),
    listing('a:soon-a', { title: 'Alpha', startDate: '2026-11-01' }),
  ];
  const rules = curated({ include: listings.map((l) => ({ id: l.id })) });
  const forward = ids(run({ listings, rules }).shows);
  const backward = ids(run({ listings: [...listings].reverse(), rules }).shows);
  assert.deepEqual(forward, ['a:running', 'a:soon-a', 'a:soon-b', 'a:late']);
  assert.deepEqual(backward, forward);
});

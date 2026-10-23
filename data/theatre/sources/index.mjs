// Every scraped source. National Theatre, Talawa, Young Vic and Eclipse refuse scripted requests, so their
// shows are `manual` entries in data/shows-curated.json.
import * as almeida from './almeida.mjs';
import * as bush from './bush.mjs';
import * as officialLondonTheatre from './official-london-theatre.mjs';
import * as royalCourt from './royal-court.mjs';
import * as stratfordEast from './stratford-east.mjs';

export const sources = [royalCourt, bush, almeida, stratfordEast, officialLondonTheatre];

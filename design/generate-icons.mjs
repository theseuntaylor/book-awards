import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { argbFromHex, Hct, MaterialDynamicColors, SchemeTonalSpot } from '@material/material-color-utilities';

// The "Laurel & Book" app icon: a laurel wreath with a star, rising from an open book, in gold foil on the
// theme's deep green. One geometry feeds every output, so the platforms can't drift apart:
//   Android: adaptive icon (vector foreground, colour background, monochrome for themed icons).
//   iOS: AppIcon with light, dark and tinted 1024px PNGs (rendered by render-png.swift, so it needs macOS).
//   design/icon/app-icon.svg: the light master, for store listings and docs.

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const seed = readFileSync(join(root, 'design/seed'), 'utf8').trim();

function themeColor(role, isDark) {
  const scheme = new SchemeTonalSpot(Hct.fromInt(argbFromHex(`#${seed.slice(0, 6)}`)), isDark, 0);
  return `#${(MaterialDynamicColors[role].getArgb(scheme) & 0xffffff).toString(16).toUpperCase().padStart(6, '0')}`;
}

const FOIL = '#D9B860';
const GROUND = themeColor('onPrimaryContainer', false);
const NIGHT = themeColor('surface', true);

// ---- Geometry, drawn in a 1024 square; fitMark() then scales and centres it for each output. ----

const rad = (deg) => (deg * Math.PI) / 180;
const WREATH = { cx: 512, cy: 464, r: 236 };

// Each shape: { kind: 'fill', d: [commands] } or { kind: 'stroke', width, d }, where a command is
// [letter, ...points] with absolute points, so transforming a shape is transforming its points.
function leaf(base, dir, length, halfWidth) {
  const normal = [-dir[1], dir[0]];
  const mid = [base[0] + dir[0] * length * 0.5, base[1] + dir[1] * length * 0.5];
  const tip = [base[0] + dir[0] * length, base[1] + dir[1] * length];
  const side = (s) => [mid[0] + normal[0] * halfWidth * 2 * s, mid[1] + normal[1] * halfWidth * 2 * s];
  return { kind: 'fill', d: [['M', base], ['Q', side(1), tip], ['Q', side(-1), base], ['Z']] };
}

function leftBranch() {
  const { cx, cy, r } = WREATH;
  const at = (a) => [cx + r * Math.cos(rad(a)), cy + r * Math.sin(rad(a))];
  const start = 128, end = 250;
  const stem = [['M', at(start)]];
  for (let a = start + 4; a <= end; a += 4) stem.push(['L', at(a)]);
  const shapes = [{ kind: 'stroke', width: 15, d: stem }];
  // Five pairs of leaves pointing up the stem, one outside and one inside, shrinking toward the top.
  for (let k = 1; k <= 5; k++) {
    const a = start + k * 21;
    const scale = 1 - (k - 1) * 0.08;
    const travel = [-Math.sin(rad(a)), Math.cos(rad(a))];
    const outward = [Math.cos(rad(a)), Math.sin(rad(a))];
    for (const side of [1, -1]) {
      const tilt = rad(38) * side;
      const dir = [travel[0] * Math.cos(tilt) + outward[0] * Math.sin(tilt), travel[1] * Math.cos(tilt) + outward[1] * Math.sin(tilt)];
      shapes.push(leaf(at(a), dir, 96 * scale, 22 * scale));
    }
  }
  // A last leaf continues the stem's line at its tip.
  shapes.push(leaf(at(end), [-Math.sin(rad(end)), Math.cos(rad(end))], 56, 13));
  return shapes;
}

function mirror(shape) {
  return { ...shape, d: shape.d.map(([cmd, ...pts]) => [cmd, ...pts.map(([x, y]) => [1024 - x, y])]) };
}

function star(cx, cy, outer, inner) {
  const d = [];
  for (let i = 0; i < 10; i++) {
    const a = rad(-90 + i * 36), len = i % 2 ? inner : outer;
    d.push([i ? 'L' : 'M', [cx + len * Math.cos(a), cy + len * Math.sin(a)]]);
  }
  return { kind: 'fill', d: [...d, ['Z']] };
}

function book() {
  // Left page, its mirror, and the curved page-block edge beneath both.
  const page = { kind: 'fill', d: [['M', [503, 690]], ['C', [430, 646], [340, 638], [262, 656]], ['L', [262, 768]], ['C', [340, 752], [430, 760], [503, 802]], ['Z']] };
  const edge = {
    kind: 'fill',
    d: [
      ['M', [262, 786]], ['C', [340, 770], [430, 778], [504, 820]], ['L', [520, 820]], ['C', [594, 778], [684, 770], [762, 786]],
      ['L', [762, 804]], ['C', [684, 790], [594, 798], [520, 840]], ['L', [504, 840]], ['C', [430, 798], [340, 790], [262, 804]], ['Z'],
    ],
  };
  return [page, mirror(page), edge];
}

const MARK = [...leftBranch(), ...leftBranch().map(mirror), star(512, 206, 46, 19), ...book()];

// Scales the mark so every point lies within `radius` of (`centre`, `centre`): the circle a launcher may crop to.
function fitMark(centre, radius) {
  const points = MARK.flatMap((s) => s.d.flatMap(([, ...pts]) => pts));
  const xs = points.map((p) => p[0]), ys = points.map((p) => p[1]);
  const mx = (Math.min(...xs) + Math.max(...xs)) / 2, my = (Math.min(...ys) + Math.max(...ys)) / 2;
  const reach = Math.max(...points.map(([x, y]) => Math.hypot(x - mx, y - my)));
  const k = radius / reach;
  return MARK.map((s) => ({
    ...s,
    width: s.width && s.width * k,
    d: s.d.map(([cmd, ...pts]) => [cmd, ...pts.map(([x, y]) => [centre + (x - mx) * k, centre + (y - my) * k])]),
  }));
}

const pathData = (d, digits) => d.map(([cmd, ...pts]) => cmd + pts.map((p) => p.map((n) => +n.toFixed(digits)).join(' ')).join(' ')).join(' ');

// ---- Outputs ----

function svg(size, background, color, shapes) {
  // Every shape carries its own fill: macOS's SVG renderer ignores fills inherited from a <g>.
  const body = shapes
    .map((s) => s.kind === 'fill'
      ? `<path d="${pathData(s.d, 1)}" fill="${color}"/>`
      : `<path d="${pathData(s.d, 1)}" fill="none" stroke="${color}" stroke-width="${+s.width.toFixed(1)}" stroke-linecap="round" stroke-linejoin="round"/>`)
    .join('\n  ');
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="${size}" height="${size}" fill="${background}"/>
  ${body}
</svg>
`;
}

function vectorDrawable(shapes, color) {
  const body = shapes
    .map((s) => s.kind === 'fill'
      ? `    <path\n        android:fillColor="${color}"\n        android:pathData="${pathData(s.d, 2)}" />`
      : `    <path\n        android:strokeColor="${color}"\n        android:strokeWidth="${+s.width.toFixed(2)}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n        android:pathData="${pathData(s.d, 2)}" />`)
    .join('\n');
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by design/generate-icons.mjs. Do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${body}
</vector>
`;
}

function write(path, text) {
  mkdirSync(dirname(join(root, path)), { recursive: true });
  writeFileSync(join(root, path), text);
}

// iOS shows the whole rounded square, so the mark can be large.
const iosMark = fitMark(512, 384);
write('design/icon/app-icon.svg', svg(1024, GROUND, FOIL, iosMark));

// Android: a 108dp layer of which launchers show at least the centre 66dp circle (radius 33).
const androidMark = fitMark(54, 30);
const res = 'composeApp/src/androidMain/res';
write(`${res}/drawable/ic_launcher_foreground.xml`, vectorDrawable(androidMark, FOIL));
// Themed icons recolour this layer from its alpha, so its colour doesn't matter.
write(`${res}/drawable/ic_launcher_monochrome.xml`, vectorDrawable(androidMark, '#FFFFFFFF'));
write(`${res}/values/ic_launcher_background.xml`, `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by design/generate-icons.mjs. Do not edit by hand. -->
<resources>
    <color name="ic_launcher_background">${GROUND}</color>
</resources>
`);
const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by design/generate-icons.mjs. Do not edit by hand. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`;
write(`${res}/mipmap-anydpi-v26/ic_launcher.xml`, adaptive);
write(`${res}/mipmap-anydpi-v26/ic_launcher_round.xml`, adaptive);
console.log(`Android adaptive icon -> ${res}`);

// iOS 18 picks the dark and tinted variants by appearance; earlier versions use the light one.
const appIcon = 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset';
const variants = [
  { file: 'AppIcon.png', background: GROUND, color: FOIL },
  { file: 'AppIcon-Dark.png', background: NIGHT, color: FOIL, appearance: 'dark' },
  // Tinted icons are grayscale; the system applies the tint.
  { file: 'AppIcon-Tinted.png', background: '#000000', color: '#FFFFFF', appearance: 'tinted' },
];
const scratch = mkdtempSync(join(tmpdir(), 'book-awards-icon-'));
try {
  for (const v of variants) {
    const source = join(scratch, `${v.file}.svg`);
    writeFileSync(source, svg(1024, v.background, v.color, iosMark));
    mkdirSync(join(root, appIcon), { recursive: true });
    execFileSync('swift', [join(root, 'design/render-png.swift'), source, join(root, appIcon, v.file), '1024'], { stdio: 'inherit' });
  }
} finally {
  rmSync(scratch, { recursive: true, force: true });
}
write(`${appIcon}/Contents.json`, `${JSON.stringify({
  images: variants.map((v) => ({
    ...(v.appearance && { appearances: [{ appearance: 'luminosity', value: v.appearance }] }),
    filename: v.file,
    idiom: 'universal',
    platform: 'ios',
    size: '1024x1024',
  })),
  info: { author: 'xcode', version: 1 },
}, null, 2)}\n`);
write('iosApp/iosApp/Assets.xcassets/Contents.json', `${JSON.stringify({ info: { author: 'xcode', version: 1 } }, null, 2)}\n`);
console.log(`iOS AppIcon -> ${appIcon}`);

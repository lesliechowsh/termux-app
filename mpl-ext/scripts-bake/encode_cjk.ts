import { readFileSync, writeFileSync } from 'node:fs';
import { encodeFace } from './scripts/encode_fonts.ts';
import type { FaceData } from './src/core/fonts.ts';
import { FACE_CATALOG } from './src/core/fontcodec.ts';

const d: FaceData = JSON.parse(readFileSync(process.argv[2], 'utf8'));
// 临时目录项
const entry = { id: 'Noto Sans CJK SC:400', family: 'Noto Sans CJK SC', weight: 400, module: 'noto-sans-cjk', binding: 'notoSansCjk' } as any;
const baked = encodeFace(d, entry);
const header = `// Generated from NotoSansCJK-Regular.ttc (SC). Do not edit.\nimport type { BakedFace } from '../core/fontcodec.ts';\n`;
let body = 'export const notoSansCjk: BakedFace = {\n';
body += `  id: ${JSON.stringify(baked.id)}, upm: ${baked.upm}, typo: [${baked.typo.join(',')}],\n`;
for (const k of ['cmap', 'cover', 'glyphs', 'outlines', 'offsets', 'kern', 'gsub'] as const) {
  const v = (baked as any)[k];
  body += typeof v === 'string' ? `  ${k}: ${JSON.stringify(v)},\n` : `  ${k}: ${JSON.stringify(v)},\n`;
}
body += '};\n';
writeFileSync(process.argv[3], header + body);
console.log('module bytes:', body.length);

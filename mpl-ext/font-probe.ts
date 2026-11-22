import { registerFonts } from "mpl";
import { allFonts } from "mpl/fonts/all";
import { notoSansCjk } from "./noto-sans-cjk.ts";
import { resolveFace, registeredFonts } from "./node_modules/mpl/src/core/fonts.ts";

registerFonts(...allFonts);
registerFonts(notoSansCjk);
console.log("registered:", registeredFonts().filter((i) => i.includes("Noto")));
const face = resolveFace([["Inter"], ["Noto Sans CJK SC"]], 400);
console.log("primary id:", face.id, "| fallback 数:", face.fallback.length);
console.log("primary cmap 有 证:", face.cmap.has("证"));
if (face.fallback[0]) {
	console.log("fallback[0]:", face.fallback[0].id, "| 有 证:", face.fallback[0].cmap.has("证"));
}

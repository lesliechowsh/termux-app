# node_modules/mpl 本地补丁(重装扩展后需重放)

mpl 包(npm)运行时走 dist/browser 预构建产物。以下 3 处改动使其支持 CJK:

1. `src/core/fontcodec.ts` FACE_CATALOG 追加:
   `{ id: 'Noto Sans CJK SC:400', family: 'Noto Sans CJK SC', weight: 400, module: 'noto-sans-cjk', binding: 'notoSansCjk' }`
2. `src/core/rc.ts` DEFAULTS: `'font.family': 'DejaVu Sans'` → `'font.family': ['DejaVu Sans']`
   (样式解析器按默认值类型决定逗号列表切分)
3. 重建 dist:`bun run scripts/build_browser_bun.ts`
   (原 build_browser.ts 依赖 node:module 的 stripTypeScriptTypes,Termux 无 node,本仓库附 bun 版)

烤制新字体模块:`bake_cjk.py`(本机无 matplotlib 的简化版 gen_fonts.py)→
`encode_cjk.ts`(scripts/ 内)→ noto-sans-cjk.ts。

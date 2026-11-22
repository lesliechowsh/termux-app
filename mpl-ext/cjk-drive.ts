import { renderChart } from "./render.ts";
const r = renderChart({
	calls: [
		["fig.suptitle", ["中文标题测试:汉字渲染验证"], {}],
		["plot", [[0, 1, 2, 3, 4], [2, 4, 3, 5, 4.5]], { label: "中文图例:正弦曲线" }],
		["set_title", ["坐标轴标题也可以是中文"], {}],
		["set_xlabel", ["横轴 x"], {}],
		["set_ylabel", ["纵轴 y"], {}],
		["legend", [], {}],
	],
	theme: "catppuccin-macchiato",
	name: "cjk-test",
});
console.log("png:", r.pngPath);

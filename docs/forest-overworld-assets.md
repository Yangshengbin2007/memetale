# Forest Overworld 资源说明

**资源分门别类与文件名约定请以 → [forest-assets-organization.md](forest-assets-organization.md) 为准。**

## 当前流程

1. **主菜单** → **Start Game**
2. **森林入口** → 背景 `forest1.jpg`，王子左 / 达拉崩吧右对话，底部对话框，点击推进
3. **地图对话** → 背景 `map1forest.jpg`，二人看地图商量
4. **选点** → 仅可点击地标（有提示），选完后黑屏，点击 Continue
5. **大地图** → 同张 `map1forest.jpg`，点击地标进入子场景（占位），Return to Map 返回

## 地标（后续接事件/小游戏）

Mushroom Ring, Doge Shrine, Troll Cave, Radio Tower, Merchant Camp, Waterfall, Gigachad Arena。  
子场景背景图命名见 `ForestLandmarkScene.bgFileForLandmark()`，未放图则占位。

## 代码位置

- `game.scene.ForestEntranceScene` — 入口 + 地图对话 + 选点 + 黑屏
- `game.scene.ForestOverworldMapScene` — 大地图
- `game.scene.ForestLandmarkScene` — 地标占位

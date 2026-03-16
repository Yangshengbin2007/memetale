# 森林章节资源 — 分门别类说明

所有森林相关图片统一放在 **`image/Chapter one/`** 或 **`image/forest/`** 下，代码通过 `ForestResources` + `ForestImageLoader` 加载。

---

## 一、目录结构

```
image/
└── Chapter one/          （或 image/forest/）
    ├── forest1.jpg       ← 森林入口背景
    ├── map1forest.jpg    ← 森林大地图（入口地图对话 + 大地图场景共用）
    └── characters/       ← 角色立绘（王子、达拉崩吧各 4 张）
        ├── prince_default.png
        ├── prince_surprise.png
        ├── prince_annoyed.png
        ├── prince_thoughtful.png
        ├── darabongba_default.png
        ├── darabongba_surprise.png
        ├── darabongba_resentment.png
        └── darabongba_nervous.png
```

---

## 二、背景图（Backgrounds）

| 文件名 | 用途 | 常量 |
|--------|------|------|
| `forest1.jpg` | 森林入口场景背景 | `ForestResources.BG_ENTRANCE` |
| `map1forest.jpg` | 地图对话 + 大地图场景 | `ForestResources.BG_MAP` |

支持格式：`.png`、`.jpg`、`.jpeg`。若使用 `forest1.png`，在 `ForestResources` 中把 `BG_ENTRANCE` 改为 `"forest1.png"` 即可。

---

## 三、角色立绘（Characters）

**优先路径：`Stickers/people/`**（与 icon、title 等贴图同级）。未找到时再查找 `image/Chapter one/characters/` 或 `image/forest/characters/`。

### 王子 Prince（4 张，固定左侧）

| 文件名 | 用途 |
|--------|------|
| `prince_default.png` | 默认 |
| `prince_surprise.png` | 惊讶 |
| `prince_annoyed.png` | 不爽 |
| `prince_thoughtful.png` | 思考 |

### 骑士达拉崩吧 Darabongba（4 张，固定右侧）

| 文件名 | 用途 | 说明 |
|--------|------|------|
| `darabongba_default.png` | 默认 | — |
| `darabongba_surprise.png` | 惊讶 | **绘制时水平镜像**，保证人贴右 |
| `darabongba_resentment.png` | 不满/不爽 | **绘制时水平镜像** |
| `darabongba_nervous.png` | 紧张/怂 | — |

对话中：**谁说话谁正常亮度，不说话的立绘变暗**。镜像逻辑在 `ForestEntranceData.mirrorDarabongba(expr)`。

---

## 四、剧情与流程

1. **森林入口**：王子 + 达拉崩吧对话（`ForestEntranceData.LINES`），底部对话框，点击推进。
2. **地图对话**：显示 `map1forest.jpg`，王子与达拉崩吧看地图商量（`ForestMapData.LINES`）。
3. **选点**：提示“Click where to go”，仅可点击地标（Troll Cave、Doge Shrine、Radio Tower、Merchant Camp、Waterfall、Mushroom Ring、Gigachad Arena），不能点其他地方。
4. **黑屏**：选完后黑屏，点击 Continue 进入大地图场景（后续地标内容待做）。

---

## 五、代码引用位置

- 路径常量：`game.model.forest.ForestResources`
- 加载逻辑：`game.model.forest.ForestImageLoader`
- 入口对话 + 表情：`game.model.forest.ForestEntranceData`
- 地图对话 + 选点地标：`game.model.forest.ForestMapData`
- 入口场景绘制：`game.scene.ForestEntranceScene`
- 大地图场景：`game.scene.ForestOverworldMapScene`（使用 `ForestImageLoader.loadBackground(ForestResources.BG_MAP)`）

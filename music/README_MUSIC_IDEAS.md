# 音乐与音效推荐 (Memetale RPG)

## 已有
- **主菜单 BGM**：beginmusic.wav
- **Jerry 出现**：beginsound.wav
- **设置试听 / 点击**：ding.wav、click.wav

## 建议新增

### BGM（背景音乐）
- **主菜单**：已有；可保留或换成更轻松、带一点幽默感的短循环。
- **第一章（阳台 / 龙 / 国王）**：  
  - 轻松、偏日常的钢琴或吉他小曲（阳台）；  
  - 龙出现时可加一段短促紧张 BGM 或保持静音突出对白；  
  - 国王给任务时可用同一首稍严肃一点的短曲。
- **地图/探索（黑屏后点击地图）**：  
  - 轻快的 RPG 探索 BGM，循环播放，便于长时间听。

### 音效（Sound Effects）
- **对话推进**：可选短促的“翻页”或“打字”声（音量小、不抢戏）。
- **存档/读档**：短确认音（如 ding 或更柔和的“保存成功”提示音）。
- **UI 按钮**：点击/悬停可用现有 click、ding，或统一一种更轻的 UI 声。
- **章节标题出现**：淡入时加一段短旋律或单音，增强“进入章节”的感觉。
- **龙/剧情高潮**：咆哮、撞击等短音效（可用免费素材库）。

### 格式与来源
- 格式：WAV 或 OGG，便于 `javax.sound.sampled` 播放。
- 免费素材示例关键词：  
  - BGM：royalty-free RPG BGM, pixel game music, visual novel BGM  
  - 音效：freesound.org, OpenGameArt, itch.io 音效包  
- 注意版权：使用 CC0 / CC-BY 或注明出处，避免商用纠纷。

### 工程内放置
- BGM：`music/` 或资源路径中已有 `music/`。
- 音效：`sound/`（与现有 ding、click 一致）。

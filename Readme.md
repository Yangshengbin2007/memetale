# memetale RPG

**A clichéd story, told in a different way.**

In the distant land of *memetale*, the princess has been taken by an evil dragon. As the prince, you explore, talk, choose, and sometimes fight—your decisions shape which ending you see (including bad ends).

> *When you have to make a choice, don’t let yourself regret it.*

This is a **story-driven RPG / mystery adventure**: dialogue, exploration, saves, settings, mini-games, and bullet-hell style battles. A longer design outline lives in [`story-outline.md`](story-outline.md).

---

## Screenshots

<p align="center">
  <img src="image/Chapter%20one/start.jpg" alt="Title / start screen" width="45%" />
  &nbsp;
  <img src="image/Chapter%20one/cg2.jpg" alt="Chapter one CG" width="45%" />
</p>

<p align="center">
  <img src="image/Chapter%20one/forest1.jpg" alt="Forest scene" width="45%" />
  &nbsp;
  <img src="image/Chapter%20one/map1forest.jpg" alt="Overworld map" width="45%" />
</p>

<p align="center">
  <img src="image/choose%20your%20way.jpg" alt="Choices" width="60%" />
</p>



---

## Requirements

| Item | Notes |
|------|--------|
| **Java** | **JDK 18** (recommended). Scripts use `--release 18`. |
| **OS** | Windows, macOS, or Linux |
| **Folders** | Keep repo layout: `src/`, `image/`, `music/` (and `saves/` created at runtime) |

Optional: On macOS, if you have Oracle JDK 18 at the path used in the scripts (`jdk-18.0.2.1.jdk`), it is picked automatically; otherwise the system `java` / `javac` is used.

---

## How to run

### Quick reference

| Platform | 中文启动 | English launcher |
|----------|-----------|------------------|
| **macOS (double-click)** | `run.command` | `run-en.command` |
| **macOS / Linux (terminal)** | `./run.sh` | `./run-en.sh` |
| **Windows** | `run.bat` | `run-en.bat` |

First time on Mac/Linux, you may need:

```bash
chmod +x run.sh run.command run-en.sh run-en.command
```

Scripts **clean `out/game`**, **compile** `Main.java` + `TrollCaveData.java`, then **run** `game.launcher.Main`.

### Manual compile (if you prefer)

```bash
mkdir -p out
javac -encoding UTF-8 --release 18 -d out -sourcepath src \
  src/game/launcher/Main.java \
  src/game/model/forest/TrollCaveData.java
java -cp out game.launcher.Main
```

Windows: same `javac` / `java` lines in `cmd`, paths with `\`.

---

## macOS security & permissions (common issues)

Apple often blocks **downloaded** scripts or apps that are not notarized. You may see messages like *“cannot be opened because the developer cannot be verified”* or *“malware”* for a local script. Three practical fixes:

### 1. Open the launcher the “safe” way (recommended)

- **Finder** → select `run.command` or `run-en.command`  
- **Right-click → Open** (not double-click the first time)  
- Confirm **Open** in the dialog  

This whitelists that file for your user.

### 2. Remove the quarantine flag (if Terminal still blocks scripts)

In **Terminal**, `cd` to the **project root** (where `run.command` lives), then:

```bash
xattr -cr .
```

Or only the script:

```bash
xattr -d com.apple.quarantine run.command 2>/dev/null
xattr -d com.apple.quarantine run-en.command 2>/dev/null
```

Then `chmod +x run.command run-en.command` and try again.

### 3. Executable bit for `.sh` / `.command`

```bash
chmod +x run.sh run-en.sh run.command run-en.command
```

If **Gatekeeper** still complains, use **System Settings → Privacy & Security** and look for an **“Allow anyway”** line right after a blocked attempt—or run the game via Terminal with `./run.sh` after `xattr -cr .`.

> **Note:** Disabling Gatekeeper globally (`spctl --master-disable`) is **not** recommended; the steps above are enough for a local dev project.

---

## Controls & UX (Chapter One + forest flow)

- **Dialogue**: click / **Space**; hold **Space** to fast-forward (where implemented).  
- **Pause / menu**: **Esc** — Save, Load, Settings, History, Quit (scene-dependent).  
- **Troll battle**: arrow keys to move the heart inside the box; hell mode shows a frozen survival time after game over.  
- **Saves**: 8 slots under `saves/`; progress includes scene ID and dialogue indices.

---

## What’s implemented (high level)

- Main menu: start, continue, settings (volume / mute), mini-game hub, quit.  
- **Chapter One** intro flow, quotes, and transition into the world.  
- **Forest / map**: exploration landmarks, dialogue, BGM, Esc menu + history.  
- **Troll cave** (dialogue) → **boss battle** (normal + optional difficulty relief on story loss) → **post-battle** story → return to map.  
- Save / load / delete saves; dialogue history lists for menus.  
- Mini-game collection entry (including hell / normal troll battle variants where wired in `Main`).

Planned / WIP: later chapters, more endings, extra branches and assets (see `story-outline.md`).

---

## Project layout (short)

```
src/game/          — Java sources (launcher, scenes, model, io)
image/             — Backgrounds, CGs, UI art (chapter subfolders)
music/             — BGM / SFX (wav/mp3 as loaded by code)
saves/             — Created when you save in-game
out/               — Build output (generated; safe to delete)
run*.sh / run*.command / run*.bat — Launchers (CN + EN pairs)
```

---

## 中文简要说明

- **运行**：macOS 双击 `run.command`（英文界面用 `run-en.command`）；终端可用 `./run.sh` / `./run-en.sh`；Windows 用 `run.bat` / `run-en.bat`。  
- **权限**：若提示无法验证开发者，请 **右键 → 打开**；仍不行则在项目根目录执行 `xattr -cr .`，并 `chmod +x run.command run.sh` 等。  
- **环境**：需要 **Java 18**，脚本会自动编译并启动游戏。

---

## License / credits

Course / individual project for advanced programming; assets and story belong to the author. Update this section if you add a formal license.

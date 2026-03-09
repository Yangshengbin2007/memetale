# memetale rpg
# A clichéd story, told in a different way.
In the distant land，memetale, the princess has been captured by an evil dragon. As the prince, you must defeat the dragon and rescue the princess.
Gameplay: RPG game
The gameplay involves exploring different caves based on the hints provided (there are save points), and the choices you make in these caves determine your ending (there may be bad endings).

In this mystery adventure game, players gradually uncover the truth of the world by talking to people, engaging in conversations, and solving puzzles and mini-games. The game features multiple endings, including one true ending and two hidden endings.

"When you have to make a choice, don't let yourself regret it."
![running app](https://github.com/Yangshengbin2007/Jerry-advprogramming2026-Individual-project-Repo/blob/main/image/5a40e06b-234c-4ddd-9f3f-7a05cb231185.png)
#game cg
![running app](https://github.com/Yangshengbin2007/Jerry-advprogramming2026-Individual-project-Repo/blob/main/image/Chapter%20one/gamecg1.jpg)
![running app](https://github.com/Yangshengbin2007/Jerry-advprogramming2026-Individual-project-Repo/blob/main/image/choose%20your%20way.jpg)
![running app](https://github.com/Yangshengbin2007/Jerry-advprogramming2026-Individual-project-Repo/blob/main/image/Chapter%20Two/seeashark.jpg)
![running app](https://github.com/Yangshengbin2007/Jerry-advprogramming2026-Individual-project-Repo/blob/main/image/Chapter%20Three/goodendcg.jpg)
![running app](https://github.com/Yangshengbin2007/memetale/blob/main/image/start.jpg)

## Gameplay Overview

- **Genre**: Story-driven RPG with mystery and puzzle elements.
- **Core Loop**:
  - Explore different areas and caves based on hints.
  - Talk to NPCs, collect information, and unlock new paths.
  - Solve puzzles and play mini-games to progress the story.
  - Make impactful choices that branch the storyline.
- **Progression**:
  - Multiple chapters/areas with save points.
  - Each key choice affects your relationship with the world and leads to different routes.
- **Endings**:
  - 1 true ending that reveals the full truth of memetale.
  - 2 hidden endings for players who explore deeply and make specific choices.
  - Multiple bad endings if you ignore warnings or make reckless decisions.
- **Theme**:
  - "When you have to make a choice, don't let yourself regret it."

## How to Run

### Prerequisites
- Java 18 or later installed
- macOS, Windows, or Linux

### Running the Game

**macOS:**
1. Double-click `run.command` (first time: right-click → Open)
2. Or in Terminal: `./run.command`

**Windows:**
1. Double-click `run.bat`
2. Or in Command Prompt: `run.bat`

**Linux:**
1. In Terminal: `./run.sh`
2. Or: `bash run.sh`

The script will automatically compile and run the game.

### Manual Compilation (if needed)
```bash
# Compile
javac -encoding UTF-8 -d out -sourcepath src src/game/launcher/Main.java

# Run
java -cp out game.launcher.Main
```

## Current Working Features

### ✅ Implemented Features
- **Main Menu System**
  - Start Game button
  - Continue/Load Game functionality
  - Settings (volume control, mute options)
  - Mini Games collection menu
  - Quit option

- **Chapter One - Meme Forest**
  - Full dialogue system with typewriter effect
  - Multiple CG (background) transitions
  - Character dialogue with speaker names
  - Click-to-advance dialogue
  - Space key for quick advance
  - Chapter title sequence

- **Save/Load System**
  - Save to 8 different slots
  - Load from saved slots
  - Delete save slots
  - Chapter progress tracking

- **Settings**
  - Master volume control
  - Mute music option
  - Mute sound effects option
  - Volume test preview

- **Audio System**
  - Background music (BGM) for different scenes
  - Sound effects (button clicks, dialogue sounds)
  - Volume control integration
  - Audio file loading from resources

- **Scene Management**
  - Scene switching system
  - Fade in/out transitions
  - Scene stack for navigation

- **Game State Management**
  - Story state tracking
  - Dialogue history
  - Chapter progress
  - Flag system for choices

### 🚧 In Progress / Planned
- Chapter Two and Three
- Multiple endings implementation
- Mini-games collection
- More dialogue branches
- Additional CGs and assets

## Story Outline

A detailed story outline for **memetale rpg** — including chapter structure, key choices, endings, and meme references — is maintained in `story-outline.md`.

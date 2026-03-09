# Project Structure

## Directory Layout

```
Jerry-advprogramming2026-Individual-project-Repo/
├── src/                          # Source code
│   └── game/
│       ├── launcher/            # Main entry point
│       │   └── Main.java
│       ├── scene/               # Scene classes
│       │   ├── Scene.java (interface)
│       │   ├── SceneManager.java
│       │   ├── StartScene.java
│       │   ├── ChapterOneScene.java
│       │   └── MiniGameCollectionScene.java
│       ├── model/               # Data models
│       │   ├── GameState.java
│       │   ├── StoryState.java
│       │   ├── DialogueRecord.java
│       │   └── ChapterOneData.java
│       └── io/                  # File I/O
│           └── SaveLoad.java
│
├── out/                         # Compiled classes
│   └── game/                    # Compiled package structure
│
├── image/                       # Image resources
│   ├── Chapter one/            # Chapter 1 images
│   ├── Chapter Two/             # Chapter 2 images
│   └── Chapter Three/           # Chapter 3 images
│
├── sound/                       # Sound effects
│   ├── beginsound.wav
│   ├── ding.wav
│   └── ...
│
├── music/                       # Background music
│   ├── beginmusic.wav
│   ├── cg1 birdsound.wav
│   └── ...
│
├── Stickers/                    # UI icons
│   ├── icon.png
│   └── title.png
│
├── saves/                       # Save game files
│   └── slot*.dat
│
├── docs/                        # Documentation
│   ├── class-diagram.md
│   └── project-structure.md
│
├── run.command                  # macOS launcher
├── run.bat                      # Windows launcher
├── run.sh                       # Linux launcher
├── Readme.md                    # Main README
└── story-outline.md             # Story documentation
```

## Code Organization

### Separation of Concerns
- **launcher**: Application entry point
- **scene**: UI and scene management
- **model**: Data structures and game state
- **io**: File operations

### Resource Management
- Images loaded from `image/` directory
- Audio loaded from `sound/` and `music/` directories
- Icons loaded from `Stickers/` directory
- Resources can be loaded from classpath or file system

### Build System
- Manual compilation with `javac`
- Output directory: `out/`
- Source path: `src/`
- Encoding: UTF-8

## Key Design Patterns

1. **MVC-like Structure**: Separation of model, view (scene), and controller (manager)
2. **Singleton**: GameState for global state
3. **Strategy/Interface**: Scene interface for different game scenes
4. **Factory**: Scene creation in Main class

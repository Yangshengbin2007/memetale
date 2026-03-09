# Class Diagram - memetale RPG

## Overview
This document describes the class structure and relationships in the memetale RPG game.

## Core Architecture

### Package: `game.launcher`
- **Main**: Entry point of the application
  - Creates JFrame window
  - Initializes SceneManager
  - Sets up scene connections

### Package: `game.scene`
- **Scene** (Interface)
  - `onEnter()`: Called when scene becomes active
  - `onExit()`: Called when scene is deactivated
  - `getPanel()`: Returns the Swing panel to display

- **SceneManager**: Manages scene transitions
  - Maintains current scene
  - Handles scene stack for navigation
  - Switches between scenes

- **StartScene** (implements Scene, extends JPanel)
  - Main menu interface
  - Handles game start, continue, settings, mini-games
  - Audio management (BGM, sound effects)
  - Save/Load dialogs

- **ChapterOneScene** (implements Scene, extends JPanel)
  - Chapter 1 gameplay
  - Dialogue system with typewriter effect
  - CG transitions
  - Pause menu (save, load, settings, history, quit)

- **MiniGameCollectionScene** (implements Scene, extends JPanel)
  - Mini-games menu
  - Navigation back to main menu

### Package: `game.model`
- **GameState**: Singleton pattern for global game state
  - Static access to StoryState
  - Centralized state management

- **StoryState** (implements Serializable)
  - Game progress tracking
  - Chapter index
  - Dialogue history
  - Flags map for choices
  - Save slot tracking

- **DialogueRecord** (implements Serializable)
  - Stores individual dialogue entries
  - Speaker and text content
  - Used in dialogue history

- **ChapterOneData**: Static data for Chapter 1
  - Dialogue lines array (2D array)
  - CG mapping functions
  - Background file mapping

### Package: `game.io`
- **SaveLoad**: File I/O operations
  - `save()`: Serializes StoryState to file
  - `load()`: Deserializes StoryState from file
  - `getSavedChapter()`: Reads chapter from save file
  - Exception handling for file operations

## Class Relationships

```
Main
  ├── SceneManager
  │     └── Scene (interface)
  │           ├── StartScene
  │           ├── ChapterOneScene
  │           └── MiniGameCollectionScene
  │
  └── GameState
        └── StoryState
              ├── Map<String, Boolean> (flags)
              └── List<DialogueRecord> (history)
                    └── DialogueRecord

SaveLoad
  └── StoryState (serialization)

ChapterOneData (static data)
  └── String[][] (dialogue lines)
```

## OOP Concepts Used

1. **Interface**: `Scene` interface defines contract for all scenes
2. **Inheritance**: All scene classes extend `JPanel`
3. **Composition**: SceneManager contains Scene references
4. **Singleton**: GameState uses singleton pattern
5. **Serialization**: StoryState and DialogueRecord implement Serializable

## Programming Concepts Used

1. **Collections**:
   - `ArrayList` for dialogue history
   - `HashMap` for flags
   - `Deque` (ArrayDeque) for scene stack
   - `List` interface usage

2. **File I/O**:
   - `FileInputStream` / `FileOutputStream`
   - `ObjectInputStream` / `ObjectOutputStream`
   - File operations for resources

3. **Exception Handling**:
   - Try-catch blocks throughout
   - Try-with-resources for file operations
   - Exception propagation

4. **Enhanced For Loops**:
   - Used for iterating over collections
   - Used for iterating over arrays

5. **Multidimensional Arrays**:
   - `String[][]` in ChapterOneData for dialogue lines

# Logic Flow Diagram

Optional logic flow diagram for gameplay progression (Chapter One + current playable branch).

```mermaid
flowchart TD
    A[StartScene] -->|Start Game| B[ChapterOneScene]
    A -->|Mini Games| M[MiniGameCollectionScene]

    B --> C[ForestEntranceScene]
    C --> D[ForestOverworldMapScene]
    D -->|Choose Troll Cave| E[TrollCaveScene]
    E --> F[TrollBattleScene]
    F -->|Victory| G[TrollCavePostBattleScene]
    G --> D

    F -->|Defeat / Retry| F
    E -->|ESC Quit| A
    C -->|ESC Quit| A
    D -->|ESC Quit| A
    G -->|ESC Quit| A

    M -->|Back| A
```

## Save/load logic (high-level)

```mermaid
flowchart LR
    S[Player opens ESC menu] --> SV[SaveLoad.save StoryState]
    LD[Player chooses Load] --> RL[SaveLoad.load slot]
    RL --> GS[GameState.setState loadedState]
    GS --> SM[SceneManager routes by currentScene]
```

## Scene key routing reference

- `chapter_one` -> `ChapterOneScene`
- `forest_entrance` -> `ForestEntranceScene`
- `forest_overworld_map` -> `ForestOverworldMapScene`
- `troll_cave` -> `TrollCaveScene`
- `troll_cave_post_battle` -> `TrollCavePostBattleScene`

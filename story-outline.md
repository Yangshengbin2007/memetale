# memetale RPG — Story Outline

> A clichéd story, told in a different way.  
> Heavy on narrative, player choices, and memes.

This document is the **design reference** for plot, tone, chapters, endings, and flags. For how to run the project, see **README.md**.

---

## 1. World & Tone

### 1.1 Setting: memetale

**Surface layer**

- Classic fairy-tale kingdom: castle, princess, evil dragon, hero prince.
- The continent is named **memetale** — in-universe gloss: *a land where memory and legend intertwine*.

**Deeper truth (meta) — *to be expanded***

- Example direction: a fairy-tale “save slot” that keeps resetting.
- Characters are **reused templates**, slightly rewritten each cycle.
- Occasional “data cracks” (bugs, glitches, empty rooms) hint that the world is not what it seems.

**Tone split**

- ~70% traditional RPG storytelling + light puzzles.
- ~30% meta, self-aware humour, and internet/game memes (fourth wall, in-jokes).

### 1.2 Core Themes

- **Choices and living with them**  
  The player is pushed to choose; some “minor” picks matter more than they look. The true ending stresses: not every choice can be perfect, but you own the person you are *at that moment*.

- **Truth vs story**  
  “Rescue the princess” is the **shell**; the real question is *who is telling the story*.

- **How memes are used**

  - Not only for laughs: jokes can **hide clues** (only players who get the reference glimpse part of the truth).
  - Overused memes can show a **worn-out script** — too many resets.

---

## 2. Main Characters

### 2.1 The Prince (Player)

**Surface**

- Standard RPG hero: brave, straightforward, “you are the chosen one.”

**Deeper — *optional / TBD***

- Vessel for **leftover memory** across loops; déjà vu lines played for comedy.
- Occasional “I’ve heard this before” reactions to NPCs.

**Arc**

- From “I just need to beat the game and save her” → “Is this world real?” → “Do I break the script or not?”

### 2.2 The Princess

**Surface**

- Kind, gentle, classic damsel-in-distress framing.

**Twist — *to detail later***

- Might be a **core process** of the world, or a stand-in for the author.
- The player may not be “saving” her in the obvious sense — she might be saving *you*.

**Per ending**

- **True ending:** her real stance after the truth is known.
- **Hidden paths:** e.g. darker princess route, or “escape the system together.”

### 2.3 The Dragon

**Surface**

- Traditional final boss: evil dragon, big fight.

**Truth — *to detail later***

- Could act as **garbage collector / firewall** for memetale: stops the script from breaking.
- “Eats” anomalies and characters who leave their role.

**By route**

- Normal: classic boss fight.
- Hidden: talks to the player — *I’m just the designated villain.*

### 2.4 Key NPCs (examples)

- **NPC A — Innkeeper**  
  Seems ordinary; offers save points. Secretly aware that “saving” means something heavier (loop fragments). Jokes about save-scumming and facing reality.

- **NPC B — Hooded stranger**  
  Riddles and cryptic hints. Possibly residue from a past loop or the author’s shadow.

- **NPC C — Meta commentator**  
  Breaks the fourth wall, recaps plot, calls out weird choices.

**Implemented allies (Chapter One tone)**

- **Darabongba** — knight / companion voice during forest and troll-cave arcs; reacts to failure and victory in the bullet-hell boss.

---

## 3. Chapter Structure

> Each chapter maps to one or more **scenes** in code (`Scene` / `StoryState.currentScene`).

### 3.0 Current build — Chapter One flow (*implemented*)

Rough order in the playable demo:

1. **Title / start** → **Chapter One** scripted opening (quotes, CGs, dialogue with save/load via Esc where supported).
2. **Forest entrance** — arrival dialogue, then **overworld map** (landmarks).
3. **Troll cave** — long dialogue with Prince, Darabongba, Troll Boss; then **boss battle** (Undertale-style box; optional difficulty reduction on story loss).
4. **Post-battle** — cave aftermath, then **Doge shrine** discussion on the map background, black screen, return to **overworld**.
5. Map state can mark the troll cave as completed after the Doge route (`hasCompletedTrollCaveAndChoseDoge`).

Scene keys used in saves include (non-exhaustive): `chapter_one`, `forest_entrance`, `forest_overworld_map`, `troll_cave`, `troll_cave_post_battle`.

### 3.1 Prologue — The Beginning (Chapter 0) *design*

- **Where:** throne room / eve of departure.
- **Goals:** establish the princess–dragon premise; first meme beat (“this tired old plot again”).
- **Beats:** king’s briefing (skippable / interruptible); optional room search for **old loop diaries** if multi-loop lore is kept.
- **Example flags:** patience with the king; finding a diary (raises sensitivity to truth later).

### 3.2 First Cave — Tutorial & First Meme (Chapter 1) *design*

- **Where:** path to the first cave + interior.
- **Focus:** teach movement, dialogue, light puzzles.
- **Meme beats:** tutorial NPC jokes about hammering the advance key; lone chest with “starter gear” gag item.
- **Choices:** help a minor NPC or not (affects late appearance); skip save point → opportunity for a bad end later.

### 3.3 Second Area — Cracks in the Story (Chapter 2) *planned*

- **Where:** larger cave / village + cave.
- **Goals:** world feels *wrong*; more meta (rewinds, repeated lines, wrong dialogue order).
- **Examples:** NPC line “rewritten” after a screen glitch; empty room with a “NullPointer” joke character.
- **Flags:** help a villager; enter a glitched room — feed **hidden endings**.

### 3.4 Third Area — Meta Overload (Chapter 3) *planned*

- **Where:** approach to the dragon’s domain / damaged data zone.
- **Goals:** heavy meta + partial truth; player senses the story can **reset**.
- **Examples:** commentator NPC asks “how many times have you played?”; placeholder text / variable names leak into dialogue.
- **Flags:** confront truth vs deny; trust or doubt a key NPC.

### 3.5 Finale — Dragon’s Lair & Endings *planned*

- **Where:** dragon castle / highest tower.
- **Core:** dragon fight and/or negotiation (combat + puzzle + dialogue mix), routed by accumulated flags.
- **Final forks:** strike the dragon or not; “save” the princess vs **break the script**; sacrifice / loop / escape.

---

## 4. Endings

### 4.1 True Ending *design*

- **Rough conditions:** many important flags seen (`flag_found_diary`, `flag_entered_glitch_room`, `flag_trust_mysterious_npc`, `flag_confront_truth`, etc.); final choice to **face truth and own the outcome**.
- **Thematic payoff (TBD in prose):** prince and princess reshape memetale’s rules; dragon as guardian steps aside or helps; redemption = accepting imperfection, not just “clearing” the story once.

### 4.2 Hidden Ending A — Deep exploration

- **Conditions:** secret rooms, obscure puzzles, one-shot challenges.
- **Direction:** “author room,” debug-flavoured scene, cast as variable names briefly.

### 4.3 Hidden Ending B — Black comedy / high meta

- **Conditions:** many “wrong” joke choices that secretly satisfy hidden logic.
- **Direction:** player treated as the bug; fake “admin mode”; UI chaos (must stay controllable in implementation).

### 4.4 Bad endings (templates)

- Rush a cave unprepared → instant death gag.
- Always deny truth → stuck in starter-village loop.
- Abuse save/load → system flags you as timeline pollution → save wiped (if designed).

---

## 5. Flags & Save Model (*aligned with code where noted*)

> Use these names (or rename in one pass in **StoryState** + content) so saves and ending checks stay consistent.

### 5.1 Boolean flags in `StoryState` (defaults in constructor)

| Key | Meaning |
|-----|--------|
| `flag_listened_to_king` | Heard the king’s full briefing (prologue tone). |
| `flag_found_diary` | Found the old diary / log. |
| `flag_helped_mime` | Helped the mime (or equivalent Chapter One side beat). |
| `flag_entered_glitch_room` | Entered a glitch / bug room. |

*Add more as chapters grow:* e.g. `flag_trust_mysterious_npc`, `flag_confront_truth`, `flag_spare_dragon`, `flag_save_princess`.

### 5.2 Progress fields (serialized in saves)

| Field | Role |
|-------|------|
| `currentScene` | Resume point (`chapter_one`, `forest_entrance`, `troll_cave`, …). |
| `savedChapter` | Display / validation (1–3). |
| `chapterOneDialogueIndex` | Line index in Chapter One script. |
| `chapterOneHistory` | Dialogue log for History UI. |
| `trollCaveDialogueIndex` | Index in troll cave script. |
| `trollCaveHistory` | Cave + post-cave lines for History. |
| `postBattleBlockIndex` / `postBattleLineIndex` | Post-battle scene blocks. |
| `postBattleBlackScreen` | On black screen before map return. |
| `hasCompletedTrollCaveAndChoseDoge` | Post–Doge-shrine completion for map UI. |
| `lastUsedSaveSlot` | Last save slot (1–8). |

---

## 6. Meme Distribution (by chapter)

- **Chapter 0:** fairy-tale clichés, long opening speeches, tutorial jabs.
- **Chapter 1:** starter village energy, trash loot, save points, weak-mob self-awareness.
- **Chapter 2:** bugs, rewritten lines, time rollback, map errors.
- **Chapter 3:** script variables, comments, dev-style jokes.
- **Finale:** log/stack flavour (soft), author voice, direct meta Q&A.

*Add specific jokes here as they ship; then mirror them in dialogue data files.*

---

## 7. From Outline to Implementation (checklist)

### 7.1 Scenes & flow

- Name each major location as a **Scene** class or scene key.
- For each: entry conditions (flags / progress), exit conditions (puzzle / choice / battle), transitions (state diagram).

### 7.2 Dialogue & events

- Pick a data format (tables in code, JSON, etc.) with at least: id, speaker, text, next, choices (label → next id + flag changes), `requiredFlags` / `setFlags`.

### 7.3 `StoryState` / `GameState`

- Single source of truth: flags, scene id, indices, histories, ending unlocks for a gallery later.

### 7.4 Save / load

- Persist everything needed to resume mid-line: scene + dialogue indices + flag map + optional position.
- Slots and file paths (e.g. `saves/slotN.dat`) as in current game.

### 7.5 Puzzles & mini-games

- Per puzzle: chapter, scene, success/failure flags, repeatable or not, ties to hidden endings.
- Optional shared interface (`start`, `update`, `completed`, `result`).

### 7.6 UI / UX

- Dialogue: portraits, names, skip / hold-to-fast-forward, Esc menus (save, load, settings, history).
- Transitions: fades, black screens (“memory rebuilding…”).
- Endings: title cards, CGs, gallery unlock list on title screen.

---

## 8. Document history

- **English rewrite:** full translation + sync with Chapter One implementation (forest, troll cave, battle, post-battle, `StoryState` keys).
- Update this section when major plot or flag renames land.

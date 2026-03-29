package game.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryState implements Serializable {
    private static final long serialVersionUID = 6L;

    // simple flag storage; can be expanded
    private final Map<String, Boolean> flags = new HashMap<>();
    private String currentScene;

    /** Save slot chapter id (1 / 2 / 3) for load UI and routing. */
    private int savedChapter = 1;

    /** Chapter-one dialogue index (0-based); saved and restored on load. */
    private int chapterOneDialogueIndex = 0;
    /** Chapter-one dialogue history for the History menu; grows as the player progresses. */
    private final List<DialogueRecord> chapterOneHistory = new ArrayList<>();
    /** Last save slot used in-game (load uses the same slot). */
    private int lastUsedSaveSlot = 1;

    /** Troll Cave dialogue index (0-based). Save/Load. */
    private int trollCaveDialogueIndex = 0;
    /** Troll Cave dialogue history for History menu. */
    private final List<DialogueRecord> trollCaveHistory = new ArrayList<>();

    public int getTrollCaveDialogueIndex() { return trollCaveDialogueIndex; }
    public void setTrollCaveDialogueIndex(int i) { this.trollCaveDialogueIndex = Math.max(0, i); }
    public List<DialogueRecord> getTrollCaveHistory() { return new ArrayList<>(trollCaveHistory); }
    public void addTrollCaveHistory(String speaker, String text) { trollCaveHistory.add(new DialogueRecord(speaker, text)); }
    public void setTrollCaveHistory(List<DialogueRecord> list) {
        trollCaveHistory.clear();
        if (list != null) trollCaveHistory.addAll(list);
    }

    /** After finishing Troll Cave flow and Doge Shrine dialogue, set so map shows "We've been there" for Troll Cave. */
    private boolean hasCompletedTrollCaveAndChoseDoge = false;
    public boolean hasCompletedTrollCaveAndChoseDoge() { return hasCompletedTrollCaveAndChoseDoge; }
    public void setHasCompletedTrollCaveAndChoseDoge(boolean v) { this.hasCompletedTrollCaveAndChoseDoge = v; }

    /** Troll cave post-battle scene progress (block 0–1, line within block). */
    private int postBattleBlockIndex = 0;
    private int postBattleLineIndex = 0;

    public int getPostBattleBlockIndex() { return postBattleBlockIndex; }
    public void setPostBattleBlockIndex(int postBattleBlockIndex) { this.postBattleBlockIndex = Math.max(0, Math.min(1, postBattleBlockIndex)); }
    public int getPostBattleLineIndex() { return postBattleLineIndex; }
    public void setPostBattleLineIndex(int postBattleLineIndex) { this.postBattleLineIndex = Math.max(0, postBattleLineIndex); }

    /** Post-battle scene: black screen before returning to map (save/load resume). */
    private boolean postBattleBlackScreen = false;
    public boolean isPostBattleBlackScreen() { return postBattleBlackScreen; }
    public void setPostBattleBlackScreen(boolean postBattleBlackScreen) { this.postBattleBlackScreen = postBattleBlackScreen; }

    public StoryState() {
        flags.put("flag_listened_to_king", false);
        flags.put("flag_found_diary", false);
        flags.put("flag_helped_mime", false);
        flags.put("flag_entered_glitch_room", false);
        currentScene = "Prologue";
    }

    public void setFlag(String key, boolean value) {
        flags.put(key, value);
    }

    public boolean getFlag(String key) {
        Boolean v = flags.get(key);
        return v != null && v;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public String getCurrentScene() {
        return currentScene;
    }

    public void setCurrentScene(String currentScene) {
        this.currentScene = currentScene;
    }

    public int getChapterOneDialogueIndex() { return chapterOneDialogueIndex; }
    public void setChapterOneDialogueIndex(int chapterOneDialogueIndex) { this.chapterOneDialogueIndex = chapterOneDialogueIndex; }

    public List<DialogueRecord> getChapterOneHistory() { return new ArrayList<>(chapterOneHistory); }
    public void addChapterOneHistory(String speaker, String text) { chapterOneHistory.add(new DialogueRecord(speaker, text)); }
    public void setChapterOneHistory(List<DialogueRecord> list) {
        chapterOneHistory.clear();
        if (list != null) chapterOneHistory.addAll(list);
    }

    public int getLastUsedSaveSlot() { return lastUsedSaveSlot; }
    public void setLastUsedSaveSlot(int lastUsedSaveSlot) { this.lastUsedSaveSlot = lastUsedSaveSlot; }

    public int getSavedChapter() { return savedChapter; }
    public void setSavedChapter(int savedChapter) { this.savedChapter = Math.max(1, Math.min(3, savedChapter)); }

    /** Epilogue scene progress when {@link #currentScene} is {@code dragon_reunion_ending}. */
    private int dragonEpiloguePhase;
    private int dragonEpilogueMainIndex;
    private int dragonEpilogueCreditIndex;
    private int dragonEpilogueExtraIndex;

    public int getDragonEpiloguePhase() { return dragonEpiloguePhase; }
    public void setDragonEpiloguePhase(int v) { this.dragonEpiloguePhase = Math.max(0, Math.min(3, v)); }
    public int getDragonEpilogueMainIndex() { return dragonEpilogueMainIndex; }
    public void setDragonEpilogueMainIndex(int v) { this.dragonEpilogueMainIndex = Math.max(0, v); }
    public int getDragonEpilogueCreditIndex() { return dragonEpilogueCreditIndex; }
    public void setDragonEpilogueCreditIndex(int v) { this.dragonEpilogueCreditIndex = Math.max(0, v); }
    public int getDragonEpilogueExtraIndex() { return dragonEpilogueExtraIndex; }
    public void setDragonEpilogueExtraIndex(int v) { this.dragonEpilogueExtraIndex = Math.max(0, v); }

    @Override
    public String toString() {
        return "StoryState{currentScene=" + currentScene + ", flags=" + flags + "}";
    }
}

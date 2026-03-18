package game.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryState implements Serializable {
    private static final long serialVersionUID = 3L;

    // simple flag storage; can be expanded
    private final Map<String, Boolean> flags = new HashMap<>();
    private String currentScene;

    /** 存档所属章节（1=第一章 2=第二章 3=第三章），用于读档时判断与显示 */
    private int savedChapter = 1;

    /** 第一章当前对话下标（0-based），存/读档用，读档从此位置继续 */
    private int chapterOneDialogueIndex = 0;
    /** 第一章已出现过的对话（History 用），随进度追加 */
    private final List<DialogueRecord> chapterOneHistory = new ArrayList<>();
    /** 当前使用的存档槽位（与游戏内 Save/Load 一致，读档时用同一槽） */
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

    @Override
    public String toString() {
        return "StoryState{currentScene=" + currentScene + ", flags=" + flags + "}";
    }
}

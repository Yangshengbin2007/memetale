package main.java.game;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StoryState implements Serializable {
    private static final long serialVersionUID = 1L;

    // simple flag storage; can be expanded
    private Map<String, Boolean> flags = new HashMap<>();
    private String currentScene;

    public StoryState() {
        // default initial flags
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

    @Override
    public String toString() {
        return "StoryState{currentScene=" + currentScene + ", flags=" + flags + "}";
    }
}

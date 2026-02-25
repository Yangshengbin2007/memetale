package main.java.game;

public final class GameState {
    private static StoryState state = new StoryState();

    private GameState() {
    }

    public static StoryState getState() {
        return state;
    }

    public static void setState(StoryState s) {
        if (s != null)
            state = s;
    }
}

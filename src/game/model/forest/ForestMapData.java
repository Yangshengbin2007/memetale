package game.model.forest;

/**
 * Map dialogue: Prince and Darabongba read the map.
 * Each row: [speaker, text, princeExpression, darabongbaExpression].
 * After the script, the player picks a landmark (clickable regions only, with a hint).
 */
public final class ForestMapData {
    private ForestMapData() {}

    // Each row: speaker, text, princeExpr, darabongbaExpr
    public static final String[][] LINES = {
        {"", "Prince pulls out a map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Alright. According to the king... the dragon flew this way.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Darabongba squints.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "This map looks suspicious.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Why?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Because it's hand-drawn.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "So?", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "The castle cartographer is a five-year-old.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "He's eight.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "That explains the spelling errors.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince looks at the map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Why is \"Forest\" spelled with three S's?", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Atmosphere.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Why is there a dinosaur here?", ForestEntranceData.EXPR_SURPRISE, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Motivation.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Motivation for what?", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Not going there.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince sighs.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Alright... first stop on our list.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Prince points at the map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "\"Troll Cave.\" That's where we're heading first.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Hard pass. I've seen enough internet comments to last a lifetime.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "We haven't even been there yet.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Exactly. Perfect record.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "What's wrong with trolls?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "They argue in comment sections.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "What?", ForestEntranceData.EXPR_SURPRISE, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Never read troll comments. You'll lose faith in humanity.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "We're in a medieval fantasy kingdom.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Exactly. Humanity is already struggling.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince points again.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Next place. \"Doge Shrine.\"", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Darabongba gasps.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Legendary.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_SURPRISE},
        {"Prince", "You've heard of it?", ForestEntranceData.EXPR_SURPRISE, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Of course.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "What is it?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "A sacred temple dedicated to a wise dog.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "That sounds fake.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "So does half this map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "What's inside the shrine?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Rumor says... treasure.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Good.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Also memes.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Less useful.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Don't underestimate memes. Empires have fallen for less.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince looks again.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Next location. \"Radio Tower.\"", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Darabongba freezes.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Why is there a radio tower in a medieval forest?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_SURPRISE},
        {"Prince", "Good question.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Does the dragon listen to podcasts?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Maybe battle strategy podcasts.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "\"How to Kidnap Princesses - Episode 12.\"", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Not helpful.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"", "Prince rubs his forehead.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Next location.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Prince points to the edge of the map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Waterfall.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Darabongba leans closer.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "That waterfall looks suspicious.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "It's water. Falling.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Suspiciously falling.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Water does that.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "But look at the rocks.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "What about them?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "They look puzzle-shaped.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Everything looks puzzle-shaped to you.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "That's because life is a puzzle.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "You fell out of a tree ten minutes ago.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "A philosophical tree.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Focus.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"", "Prince taps another doodle on the parchment.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "\"Gigachad Arena.\"", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "That sounds like a gym for people who yell at mirrors.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Or a tournament.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Same thing.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince studies the map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "There's something scribbled here.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "What does it say?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"", "Prince squints.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "\"Three guardians hold the keys.\"", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Keys to what?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_SURPRISE},
        {"Prince", "It doesn't say.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "That's definitely not suspicious.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Why would someone hide keys in a forest?", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Because dungeons were already full.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Maybe the keys open a treasure.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Or a curse.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Or a door.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Or something that should absolutely stay closed.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "You're being dramatic.", ForestEntranceData.EXPR_ANNOYED, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "I am a knight. Drama is part of the job.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"", "Prince folds the map.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Prince", "Alright. Plan time.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "I love plans.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "First we visit the locations.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Bold.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "We defeat the guardians.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Ambitious.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "We collect the keys.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Risky.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Then we continue toward the dragon.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Terrible idea.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "You're coming with me.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "I regret agreeing earlier.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "Adventure awaits.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "So does regret.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_NERVOUS},
        {"Prince", "Let's go. First stop: Troll Cave.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Of course it is. The one place that sounds like a comment section.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "One of the keys is there. Or so the king said. I might have zoned out.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "If something explodes I'm blaming you.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
        {"Prince", "If something explodes I'm running.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_DEFAULT},
        {"Darabongba", "Good. We're finally thinking the same way.", ForestEntranceData.EXPR_DEFAULT, ForestEntranceData.EXPR_RESENTMENT},
    };

    // First destination in this phase is troll_cave; wrong landmarks show a hint (see ForestEntranceScene).
    public static final String FIRST_DESTINATION_LANDMARK_ID = "troll_cave";

    // Landmark ids for the pick phase; order must match ForestOverworldMapScene.
    public static final String[] CHOICE_LANDMARK_IDS = {
        "troll_cave", "waterfall", "radio_tower", "doge_shrine", "gigachad_arena"
    };
    public static final String[] CHOICE_LANDMARK_NAMES = {
        "Troll Cave", "Waterfall", "Radio Tower", "Doge Shrine", "Gigachad Arena"
    };

    // Hit rectangles in 800x600 space (x, y, w, h); same order as CHOICE_LANDMARK_IDS.
    public static final int[][] CHOICE_BOUNDS_800x600 = {
        {80, 120, 110, 70},
        {600, 320, 100, 80},
        {580, 180, 100, 70},
        {120, 360, 100, 80},
        {340, 60, 100, 75}
    };
}

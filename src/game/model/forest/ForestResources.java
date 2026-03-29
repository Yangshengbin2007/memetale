package game.model.forest;

/**
 * Forest chapter asset path constants. Images live under image/Chapter one/ or image/forest/.
 * Categories: backgrounds, map, character portraits.
 */
public final class ForestResources {
    private ForestResources() {}

    /** Forest entrance background: forest1.jpg */
    public static final String BG_ENTRANCE = "forest1.jpg";
    /** Overworld map image: map1forest.jpg */
    public static final String BG_MAP = "map1forest.jpg";

    /** Character portrait subfolder name */
    public static final String CHARACTERS_DIR = "characters/";

    /** Prince portraits: prince_default, prince_surprise, prince_annoyed, prince_thoughtful (four files). */
    public static final String PRINCE_DEFAULT   = "prince_default";
    public static final String PRINCE_SURPRISE = "prince_surprise";
    public static final String PRINCE_ANNOYED  = "prince_annoyed";
    public static final String PRINCE_THOUGHTFUL = "prince_thoughtful";

    /** Darabongba portraits: darabongba_default, surprise, resentment, nervous (four files). Mirror for resentment/surprise. */
    public static final String DARABONGBA_DEFAULT    = "darabongba_default";
    public static final String DARABONGBA_SURPRISE   = "darabongba_surprise";
    public static final String DARABONGBA_RESENTMENT  = "darabongba_resentment";
    public static final String DARABONGBA_NERVOUS     = "darabongba_nervous";

    public static final String[] IMAGE_EXTENSIONS = { ".png", ".jpg", ".jpeg" };
}

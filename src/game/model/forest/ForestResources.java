package game.model.forest;

/**
 * 森林章节资源路径常量。图片放在 image/Chapter one/ 或 image/forest/ 下。
 * 分门别类：背景、地图、角色立绘。
 */
public final class ForestResources {
    private ForestResources() {}

    /** 森林入口背景：forest1.jpg */
    public static final String BG_ENTRANCE = "forest1.jpg";
    /** 森林大地图：map1forest.jpg */
    public static final String BG_MAP = "map1forest.jpg";

    /** 角色立绘子目录 */
    public static final String CHARACTERS_DIR = "characters/";

    /** 王子立绘：prince_default, prince_surprise, prince_annoyed, prince_thoughtful（共4张） */
    public static final String PRINCE_DEFAULT   = "prince_default";
    public static final String PRINCE_SURPRISE = "prince_surprise";
    public static final String PRINCE_ANNOYED  = "prince_annoyed";
    public static final String PRINCE_THOUGHTFUL = "prince_thoughtful";

    /** 骑士达拉崩吧立绘：darabongba_default, darabongba_surprise, darabongba_resentment, darabongba_nervous（共4张）。resentment/surprise 时需水平镜像以贴右。 */
    public static final String DARABONGBA_DEFAULT    = "darabongba_default";
    public static final String DARABONGBA_SURPRISE   = "darabongba_surprise";
    public static final String DARABONGBA_RESENTMENT  = "darabongba_resentment";
    public static final String DARABONGBA_NERVOUS     = "darabongba_nervous";

    public static final String[] IMAGE_EXTENSIONS = { ".png", ".jpg", ".jpeg" };
}

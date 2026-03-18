package game.model.forest;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.File;
import java.net.URL;

/**
 * 森林章节图片加载。优先 classpath（/image/Chapter one/、/image/forest/），再文件路径。
 * 支持 .png、.jpg。角色立绘在 characters/ 下：prince_*.png, darabongba_*.png。
 */
public final class ForestImageLoader {

    private static final String[] BASE_PATHS = {
        "Chapter one",
        "chapter%20one",
        "forest"
    };

    private static final String[] EXTENSIONS = { ".png", ".jpg", ".jpeg" };

    public static Image loadBackground(String filename) {
        for (String base : BASE_PATHS) {
            for (String ext : EXTENSIONS) {
                String name = filename;
                if (!name.contains(".")) name = name + ext;
                Image img = fromClasspath("/image/" + base + "/" + name);
                if (img != null) return img;
                img = fromFile("image/" + base.replace("%20", " ") + "/" + name);
                if (img != null) return img;
            }
        }
        return null;
    }

    /** 王子立绘子目录与表情→文件名映射（Stickers/people/prince/ 下） */
    private static final String[][] PRINCE_EXPR_FILES = {
        { "default", "The Peaceful Prince.png" },
        { "surprise", "The Happy Prince.png" },
        { "annoyed", "The Angry Prince.png" },
        { "thoughtful", "Disdainful Prince.png" }
    };
    /** 达拉崩吧立绘子目录与表情→文件名映射（Stickers/people/Dala Bengba/ 下） */
    private static final String[][] DARABONGBA_EXPR_FILES = {
        { "default", "happy.png" },
        { "surprise", "surprise.png" },
        { "resentment", "resentment.png" },
        { "nervous", "Confuse.png" },
        { "proud", "happy.png" }
    };

    /** 加载角色立绘：优先 Stickers/people/ 子目录（prince/、Dala Bengba/）的真实文件名，再扁平名 prince_default 等 */
    public static Image loadCharacter(String character, String expression) {
        String expr = expression == null ? "default" : expression.toLowerCase();
        if ("prince".equalsIgnoreCase(character)) {
            for (String[] pair : PRINCE_EXPR_FILES) {
                if (pair[0].equals(expr)) {
                    String fn = pair[1];
                    Image img = fromClasspath("/Stickers/people/prince/" + fn);
                    if (img == null) img = fromClasspath("/Stickers/people/prince/" + fn.replace(" ", "%20"));
                    if (img != null) return img;
                    img = fromFile("Stickers/people/prince/" + fn);
                    if (img != null) return img;
                    break;
                }
            }
            if (expr.equals("default")) {
                Image img = fromFile("Stickers/people/prince/The Peaceful Prince.png");
                if (img != null) return img;
            }
        } else if ("darabongba".equalsIgnoreCase(character)) {
            for (String[] pair : DARABONGBA_EXPR_FILES) {
                if (pair[0].equals(expr)) {
                    Image img = fromClasspath("/Stickers/people/Dala Bengba/" + pair[1]);
                    if (img == null) img = fromClasspath("/Stickers/people/Dala%20Bengba/" + pair[1]);
                    if (img != null) return img;
                    img = fromFile("Stickers/people/Dala Bengba/" + pair[1]);
                    if (img != null) return img;
                    break;
                }
            }
            if (expr.equals("default")) {
                Image img = fromFile("Stickers/people/Dala Bengba/happy.png");
                if (img != null) return img;
            }
        } else if ("troll_boss".equalsIgnoreCase(character)) {
            // Actual assets: Stickers/people/Troll face/Trollface (1).png, angry (1).png
            String trollFaceDir = "Stickers/people/Troll face/";
            String defaultFn = "Trollface (1).png";
            String angryFn = "angry (1).png";
            if ("default".equals(expr)) {
                Image img = fromFile(trollFaceDir + defaultFn);
                if (img != null) return img;
                img = fromClasspath("/Stickers/people/Troll%20face/" + defaultFn.replace(" ", "%20"));
                if (img != null) return img;
            } else if ("angry".equals(expr)) {
                Image img = fromFile(trollFaceDir + angryFn);
                if (img != null) return img;
                img = fromClasspath("/Stickers/people/Troll%20face/" + angryFn.replace(" ", "%20"));
                if (img != null) return img;
            }
            // Fallback: troll_boss.png / troll_boss_angry.png in other dirs
            String fn = "default".equals(expr) ? "troll_boss.png" : "troll_boss_" + expr + ".png";
            for (String base : new String[]{"Stickers/people/troll_boss/", "image/Chapter one/", "image/forest/"}) {
                Image img = fromFile(base + fn);
                if (img != null) return img;
            }
            Image img = fromClasspath("/image/Chapter one/" + fn);
            if (img != null) return img;
            img = fromClasspath("/image/forest/" + fn);
            if (img != null) return img;
            img = fromFile("image/Chapter one/troll_boss.png");
            if (img != null) return img;
        }
        String name = character + "_" + expression;
        for (String ext : EXTENSIONS) {
            Image img = fromClasspath("/Stickers/people/" + name + ext);
            if (img != null) return img;
            img = fromFile("Stickers/people/" + name + ext);
            if (img != null) return img;
        }
        for (String base : BASE_PATHS) {
            String dir = base + "/" + ForestResources.CHARACTERS_DIR;
            for (String ext : EXTENSIONS) {
                Image img = fromClasspath("/image/" + dir + name + ext);
                if (img != null) return img;
                img = fromFile("image/" + base.replace("%20", " ") + "/" + ForestResources.CHARACTERS_DIR + name + ext);
                if (img != null) return img;
            }
        }
        return null;
    }

    private static Image fromClasspath(String path) {
        URL url = ForestImageLoader.class.getResource(path);
        if (url == null) return null;
        ImageIcon icon = new ImageIcon(url);
        return icon.getImage();
    }

    private static Image fromFile(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        return icon.getImage();
    }
}

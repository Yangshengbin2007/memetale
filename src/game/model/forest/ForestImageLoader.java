package game.model.forest;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.File;
import java.net.URL;

/**
 * Loads forest chapter images: classpath first (/image/Chapter one/, /image/forest/), then disk paths.
 * Supports .png and .jpg. Legacy flat names: prince_*.png, darabongba_*.png.
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

    /** Prince folder under Stickers/people/ and expression-to-filename map. */
    private static final String[][] PRINCE_EXPR_FILES = {
        { "default", "The Peaceful Prince.png" },
        { "surprise", "The Happy Prince.png" },
        { "annoyed", "The Angry Prince.png" },
        { "thoughtful", "Disdainful Prince.png" }
    };
    /** Darabongba folder under Stickers/people/ and expression-to-filename map. */
    private static final String[][] DARABONGBA_EXPR_FILES = {
        { "default", "happy.png" },
        { "surprise", "surprise.png" },
        { "resentment", "resentment.png" },
        { "nervous", "Confuse.png" },
        { "proud", "happy.png" }
    };

    /** Load a portrait: try Stickers/people/prince|Dala Bengba/ real filenames, then flat names like prince_default. */
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
            // Stickers/people/Troll face/: try English names first (default.png, laugh.png, defeat.png, scared.png, angry.png), then legacy (1).png names
            String trollFaceDir = "Stickers/people/Troll face/";
            String[][] trollExprFiles = {
                { "default", "default.png", "Trollface (1).png" },
                { "angry", "angry.png", "angry (1).png" },
                { "defeat", "defeat.png", "defead (1).png" },
                { "scared", "scared.png", "scard (1).png" },
                { "laugh", "laugh.png", "superlaugh (1).png" }
            };
            for (String[] pair : trollExprFiles) {
                if (pair[0].equals(expr)) {
                    for (int i = 1; i < pair.length; i++) {
                        String fn = pair[i];
                        Image img = fromFile(trollFaceDir + fn);
                        if (img != null) return img;
                        img = fromClasspath("/Stickers/people/Troll%20face/" + fn.replace(" ", "%20"));
                        if (img != null) return img;
                    }
                    break;
                }
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

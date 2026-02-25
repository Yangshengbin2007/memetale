package main.java.game;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public final class SaveLoad {
    public static final String DEFAULT_SLOT = "saves/slot1.dat";

    public static void save(StoryState state, String path) throws Exception {
        File f = new File(path);
        File dir = f.getParentFile();
        if (dir != null && !dir.exists())
            dir.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(f);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(state);
        }
    }

    public static StoryState load(String path) throws Exception {
        File f = new File(path);
        if (!f.exists())
            return null;
        try (FileInputStream fis = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object o = ois.readObject();
            if (o instanceof StoryState)
                return (StoryState) o;
            return null;
        }
    }
}

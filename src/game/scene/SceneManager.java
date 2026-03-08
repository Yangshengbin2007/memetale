package game.scene;

import java.util.Deque;
import java.util.ArrayDeque;
import javax.swing.JFrame;

public class SceneManager {
    private final JFrame frame;
    private Scene currentScene;
    private final Deque<Scene> sceneStack = new ArrayDeque<>();

    public SceneManager(JFrame frame) {
        this.frame = frame;
    }

    public void setScene(Scene scene) {
        if (currentScene != null) currentScene.onExit();
        currentScene = scene;
        if (frame != null && currentScene != null) {
            frame.setContentPane(currentScene.getPanel());
            frame.validate();
            frame.repaint();
        }
        if (currentScene != null) currentScene.onEnter();
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public void pushScene(Scene scene) {
        if (currentScene != null) sceneStack.push(currentScene);
        setScene(scene);
    }

    public void popScene() {
        Scene s = sceneStack.isEmpty() ? null : sceneStack.pop();
        setScene(s);
    }
}

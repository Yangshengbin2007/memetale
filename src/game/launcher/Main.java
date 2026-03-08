package game.launcher;

import game.scene.*;

import javax.swing.*;

public class Main {
	private JFrame frame;
	private SceneManager sceneManager;

	public void start() {
		SwingUtilities.invokeLater(() -> {
			frame = new JFrame("memetale rpg");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);
			frame.setLocationRelativeTo(null);
			frame.setResizable(false);

			sceneManager = new SceneManager(frame);
			StartScene startScene = new StartScene();
			ChapterOneScene chapterOne = new ChapterOneScene(() -> {
				startScene.setSkipJerryNextEnter(true);
				sceneManager.setScene(startScene);
			});
			MiniGameCollectionScene miniGameCollectionScene = new MiniGameCollectionScene(() ->
				sceneManager.setScene(startScene));
			startScene.setOnStartGame(() -> sceneManager.setScene(chapterOne));
			startScene.setOnMiniGames(() -> sceneManager.setScene(miniGameCollectionScene));

			sceneManager.setScene(startScene);
			frame.setVisible(true);
		});
	}

	public static void main(String[] args) {
		new Main().start();
	}
}

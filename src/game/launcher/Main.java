package game.launcher;

import game.scene.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
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
			Image icon = null;
			URL iconUrl = Main.class.getResource("/Stickers/title.png");
			if (iconUrl != null) {
				icon = new ImageIcon(iconUrl).getImage();
			}
			if (icon == null) {
				File f = new File("Stickers/title.png");
				if (f.exists()) {
					icon = new ImageIcon(f.getAbsolutePath()).getImage();
				}
			}
			if (icon != null) {
				frame.setIconImage(icon);
			}

			sceneManager = new SceneManager(frame);
			StartScene startScene = new StartScene();
			MiniGameCollectionScene miniGameCollectionScene = new MiniGameCollectionScene(() -> {
				startScene.setSkipJerryNextEnter(true);
				sceneManager.setScene(startScene);
			});
			ForestOverworldMapScene forestOverworldMapScene = new ForestOverworldMapScene(sceneManager);
			ForestEntranceScene forestEntranceScene = new ForestEntranceScene(
				() -> sceneManager.setScene(forestOverworldMapScene),
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				}
			);
			ChapterOneScene chapterOne = new ChapterOneScene(
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				},
				() -> sceneManager.setScene(forestEntranceScene)
			);
			forestEntranceScene.setOnLoadSwitchToChapterOne(() -> {
				ChapterOneScene.setSkipQuoteNextEnter(true);
				sceneManager.setScene(chapterOne);
			});
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


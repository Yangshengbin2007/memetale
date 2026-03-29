package game.launcher;

import game.model.GameState;
import game.model.StoryState;
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
			MiniGameCollectionScene miniGameCollectionScene = new MiniGameCollectionScene(
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				});
			TrollBattleScene trollBattleMinigameNormal = new TrollBattleScene(false, false,
				() -> sceneManager.setScene(miniGameCollectionScene),
				() -> sceneManager.setScene(miniGameCollectionScene));
			TrollBattleScene trollBattleMinigameHell = new TrollBattleScene(true, false,
				() -> sceneManager.setScene(miniGameCollectionScene),
				() -> sceneManager.setScene(miniGameCollectionScene));
			miniGameCollectionScene.setLaunchTrollBattle(() -> sceneManager.setScene(trollBattleMinigameNormal),
				() -> sceneManager.setScene(trollBattleMinigameHell));
			final ForestOverworldMapScene[] forestHolder = new ForestOverworldMapScene[1];
			final DragonReunionEndingScene[] dragonHolder = new DragonReunionEndingScene[1];
			final Runnable[] navigateHolder = new Runnable[1];
			dragonHolder[0] = new DragonReunionEndingScene(
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.clearStackAndSetScene(startScene);
				},
				() -> {
					if (navigateHolder[0] != null) navigateHolder[0].run();
				}
			);
			forestHolder[0] = new ForestOverworldMapScene(sceneManager, () -> {
				startScene.setSkipJerryNextEnter(true);
				sceneManager.clearStackAndSetScene(startScene);
			}, dragonHolder[0]);
			ForestEntranceScene forestEntranceScene = new ForestEntranceScene(
				() -> sceneManager.setScene(forestHolder[0]),
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				}
			);
			TrollCavePostBattleScene trollCavePostBattleScene = new TrollCavePostBattleScene(
				() -> sceneManager.setScene(forestHolder[0]),
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				}
			);
			TrollBattleScene trollBattleScene = new TrollBattleScene(
				false, true,
				() -> sceneManager.setScene(trollCavePostBattleScene),
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				}
			);
			TrollCaveScene trollCaveScene = new TrollCaveScene(
				() -> {
					startScene.setSkipJerryNextEnter(true);
					sceneManager.setScene(startScene);
				},
				() -> sceneManager.setScene(trollBattleScene)
			);
			forestEntranceScene.setOnLandmarkChosen(landmarkId -> {
				if ("troll_cave".equals(landmarkId)) {
					sceneManager.setScene(trollCaveScene);
				} else {
					sceneManager.setScene(forestHolder[0]);
				}
			});
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
			navigateHolder[0] = () -> {
				StoryState st = GameState.getState();
				String k = st == null ? null : st.getCurrentScene();
				if ("forest_entrance".equals(k)) sceneManager.setScene(forestEntranceScene);
				else if ("troll_cave".equals(k)) sceneManager.setScene(trollCaveScene);
				else if ("forest_overworld_map".equals(k)) sceneManager.setScene(forestHolder[0]);
				else if ("troll_cave_post_battle".equals(k)) sceneManager.setScene(trollCavePostBattleScene);
				else if ("dragon_reunion_ending".equals(k)) {
					dragonHolder[0].setRestoreOnNextEnter(true);
					sceneManager.setScene(dragonHolder[0]);
				} else if ("chapter_one".equals(k)) {
					ChapterOneScene.setSkipQuoteNextEnter(true);
					sceneManager.setScene(chapterOne);
				} else sceneManager.setScene(chapterOne);
			};
			trollBattleScene.setNavigateAfterLoad(navigateHolder[0]);
			trollCavePostBattleScene.setNavigateAfterLoad(navigateHolder[0]);
			startScene.setOnStartGame(() -> {
				StoryState st = GameState.getState();
				String sceneKey = st == null ? null : st.getCurrentScene();
				if ("forest_entrance".equals(sceneKey)) {
					sceneManager.setScene(forestEntranceScene);
					return;
				}
				if ("troll_cave".equals(sceneKey)) {
					sceneManager.setScene(trollCaveScene);
					return;
				}
				if ("forest_overworld_map".equals(sceneKey)) {
					sceneManager.setScene(forestHolder[0]);
					return;
				}
				if ("troll_cave_post_battle".equals(sceneKey)) {
					sceneManager.setScene(trollCavePostBattleScene);
					return;
				}
				if ("dragon_reunion_ending".equals(sceneKey)) {
					dragonHolder[0].setRestoreOnNextEnter(true);
					sceneManager.setScene(dragonHolder[0]);
					return;
				}
				sceneManager.setScene(chapterOne);
			});
			startScene.setOnMiniGames(() -> sceneManager.setScene(miniGameCollectionScene));

			sceneManager.setScene(startScene);
			frame.setVisible(true);
		});
	}

	public static void main(String[] args) {
		new Main().start();
	}
}


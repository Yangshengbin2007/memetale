package main.java.game;

import javax.swing.*;

public class Game {
	private JFrame frame;
	private SceneManager sceneManager = new SceneManager();

	public void start() {
		SwingUtilities.invokeLater(() -> {
			frame = new JFrame("memetale rpg");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);
			frame.setLocationRelativeTo(null);
			frame.setResizable(false);

			StartScene startScene = new StartScene();

			// 先设置内容面板并显示窗口，再激活场景（保证面板可见，图片度量可靠）
			frame.setContentPane(startScene.getPanel());
			frame.validate();
			frame.setVisible(true);

			// 在窗口可见后切换场景，SceneManager.setScene 会调用 startScene.onEnter()
			sceneManager.setScene(startScene);
		});
	}

	public static void main(String[] args) {
		new Game().start();
	}
}

package main.java.game;

import javax.swing.JPanel;

public interface Scene {
	// Called when the scene becomes active
	public void onEnter();
	// Called when the scene is deactivated
	public void onExit();
	// Returns the Swing panel to be shown in the window
	public JPanel getPanel();
}

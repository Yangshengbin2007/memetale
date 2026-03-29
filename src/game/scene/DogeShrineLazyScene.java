package game.scene;

import javax.swing.*;
import java.awt.*;

/**
 * Entering Doge Shrine: brief fade on English placeholder text, then the dragon reunion ending sequence.
 */
public class DogeShrineLazyScene extends JPanel implements Scene {
    private static final String LAZY_TEXT = "The rest will be added later. The author is too lazy.";
    private static final int FADE_IN_MS = 900;
    private static final int HOLD_MS = 2600;
    private static final int FADE_OUT_MS = 900;

    private final SceneManager sceneManager;
    private final Runnable onDragonEndingComplete;
    private final Scene dragonEpilogueScene;

    private int phase; // 0=fade in, 1=hold, 2=fade out, 3=handoff
    private long phaseStart;
    private float textAlpha;
    private javax.swing.Timer tickTimer;

    public DogeShrineLazyScene(SceneManager sceneManager, Runnable onDragonEndingComplete, Scene dragonEpilogueScene) {
        this.sceneManager = sceneManager;
        this.onDragonEndingComplete = onDragonEndingComplete;
        this.dragonEpilogueScene = dragonEpilogueScene;
        setBackground(Color.BLACK);
        setFocusable(true);
        tickTimer = new javax.swing.Timer(32, e -> tick());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long t = now - phaseStart;
        if (phase == 0) {
            textAlpha = Math.min(1f, t / (float) FADE_IN_MS);
            if (t >= FADE_IN_MS) {
                phase = 1;
                phaseStart = now;
            }
        } else if (phase == 1) {
            textAlpha = 1f;
            if (t >= HOLD_MS) {
                phase = 2;
                phaseStart = now;
            }
        } else if (phase == 2) {
            textAlpha = Math.max(0f, 1f - t / (float) FADE_OUT_MS);
            if (t >= FADE_OUT_MS) {
                phase = 3;
                tickTimer.stop();
                handoff();
                return;
            }
        }
        repaint();
    }

    private void handoff() {
        if (sceneManager == null) return;
        sceneManager.popScene();
        if (dragonEpilogueScene != null) {
            sceneManager.pushScene(dragonEpilogueScene);
        } else {
            sceneManager.pushScene(new DragonReunionEndingScene(onDragonEndingComplete, null));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
        FontMetrics fm = g2.getFontMetrics();
        drawWrapped(g2, LAZY_TEXT, w / 2, h / 2 - fm.getHeight(), w - 80, fm);
        g2.dispose();
    }

    private static void drawWrapped(Graphics2D g2, String text, int cx, int startY, int maxW, FontMetrics fm) {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int y = startY;
        for (String word : words) {
            String trial = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(trial) > maxW && line.length() > 0) {
                String s = line.toString();
                g2.drawString(s, cx - fm.stringWidth(s) / 2, y);
                y += fm.getHeight();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) {
            String s = line.toString();
            g2.drawString(s, cx - fm.stringWidth(s) / 2, y);
        }
    }

    @Override
    public void onEnter() {
        phase = 0;
        phaseStart = System.currentTimeMillis();
        textAlpha = 0f;
        tickTimer.start();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (tickTimer != null) tickTimer.stop();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

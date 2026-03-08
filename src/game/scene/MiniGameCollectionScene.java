package game.scene;

import javax.swing.*;
import java.awt.*;

/**
 * Main menu sub-scene: Mini-Game Collection.
 * Holds endless modes of mini-games for players who only want to play mini-games.
 * All button and label text is in English.
 */
public class MiniGameCollectionScene extends JPanel implements Scene {
    private final Runnable onBackToTitle;
    private boolean hoverBack = false;
    private boolean pressedBack = false;
    private final Rectangle backBounds = new Rectangle();

    public MiniGameCollectionScene(Runnable onBackToTitle) {
        this.onBackToTitle = onBackToTitle;
        setBackground(new Color(20, 20, 40));
        setLayout(null);
        initMouse();
    }

    private void initMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (backBounds.contains(e.getPoint())) {
                    pressedBack = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (pressedBack && backBounds.contains(e.getPoint()) && onBackToTitle != null) {
                    onBackToTitle.run();
                }
                pressedBack = false;
                hoverBack = backBounds.contains(e.getPoint());
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverBack = false;
                pressedBack = false;
                repaint();
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                hoverBack = backBounds.contains(e.getPoint());
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // Title
        g2.setColor(new Color(200, 160, 60));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32f));
        FontMetrics fm = g2.getFontMetrics();
        String title = "Mini-Game Collection";
        g2.drawString(title, (w - fm.stringWidth(title)) / 2, h * 3 / 10);

        // Subtitle
        g2.setColor(new Color(220, 220, 210));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        fm = g2.getFontMetrics();
        String sub = "Endless modes from the game — for those who just want to play.";
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h * 3 / 10 + 36);

        // Placeholder area (future mini-game entries)
        int boxY = h * 4 / 10;
        int boxH = h * 35 / 100;
        int boxW = Math.min(500, w - 80);
        int boxX = (w - boxW) / 2;
        g2.setColor(new Color(40, 40, 60));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setColor(new Color(180, 180, 170));
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 14f));
        fm = g2.getFontMetrics();
        String placeholder = "No mini-games added yet. They will appear here as endless modes.";
        int pw = fm.stringWidth(placeholder);
        if (pw > boxW - 24) {
            String shortMsg = "No mini-games yet. Coming soon.";
            g2.drawString(shortMsg, (w - fm.stringWidth(shortMsg)) / 2, boxY + boxH / 2 + fm.getAscent() / 2 - 4);
        } else {
            g2.drawString(placeholder, (w - pw) / 2, boxY + boxH / 2 + fm.getAscent() / 2 - 4);
        }

        // Back to Title button
        int btnW = 180;
        int btnH = 44;
        int bx = (w - btnW) / 2;
        int by = h - 80;
        backBounds.setBounds(bx, by, btnW, btnH);

        double scale = pressedBack ? 0.96 : 1.0;
        int cw = (int) (btnW * scale);
        int ch = (int) (btnH * scale);
        int cx = bx + (btnW - cw) / 2;
        int cy = by + (btnH - ch) / 2;
        g2.setColor(new Color(10, 60, 110));
        g2.fillRoundRect(cx, cy, cw, ch, 14, 14);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(cx, cy, cw, ch, 14, 14);
        g2.setColor(new Color(245, 245, 240));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        fm = g2.getFontMetrics();
        String backText = "Back to Title";
        g2.drawString(backText, cx + (cw - fm.stringWidth(backText)) / 2, cy + (ch + fm.getAscent()) / 2 - 2);
        if (hoverBack || pressedBack) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressedBack ? 0.4f : 0.2f));
            g2.setColor(new Color(0x1E90FF));
            g2.fillRoundRect(cx, cy, cw, ch, 14, 14);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g2.dispose();
    }

    @Override
    public void onEnter() {
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

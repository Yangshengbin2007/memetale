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
    private Runnable onLaunchTrollNormal;
    private Runnable onLaunchTrollHell;
    private boolean hoverBack = false;
    private boolean pressedBack = false;
    private boolean hoverTrollNormal = false;
    private boolean hoverTrollHell = false;
    private boolean pressedTrollNormal = false;
    private boolean pressedTrollHell = false;
    private final Rectangle backBounds = new Rectangle();
    private final Rectangle trollNormalBounds = new Rectangle();
    private final Rectangle trollHellBounds = new Rectangle();

    public MiniGameCollectionScene(Runnable onBackToTitle) {
        this.onBackToTitle = onBackToTitle;
        this.onLaunchTrollNormal = null;
        this.onLaunchTrollHell = null;
        setBackground(new Color(20, 20, 40));
        setLayout(null);
        initMouse();
    }

    public void setLaunchTrollBattle(Runnable onLaunchTrollNormal, Runnable onLaunchTrollHell) {
        this.onLaunchTrollNormal = onLaunchTrollNormal;
        this.onLaunchTrollHell = onLaunchTrollHell;
    }

    private void initMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                Point p = e.getPoint();
                if (backBounds.contains(p)) pressedBack = true;
                if (trollNormalBounds.contains(p)) pressedTrollNormal = true;
                if (trollHellBounds.contains(p)) pressedTrollHell = true;
                repaint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                Point p = e.getPoint();
                if (pressedBack && backBounds.contains(p) && onBackToTitle != null) onBackToTitle.run();
                if (pressedTrollNormal && trollNormalBounds.contains(p) && onLaunchTrollNormal != null) onLaunchTrollNormal.run();
                if (pressedTrollHell && trollHellBounds.contains(p) && onLaunchTrollHell != null) onLaunchTrollHell.run();
                pressedBack = false;
                pressedTrollNormal = false;
                pressedTrollHell = false;
                hoverBack = backBounds.contains(p);
                hoverTrollNormal = trollNormalBounds.contains(p);
                hoverTrollHell = trollHellBounds.contains(p);
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverBack = hoverTrollNormal = hoverTrollHell = false;
                pressedBack = pressedTrollNormal = pressedTrollHell = false;
                repaint();
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                Point p = e.getPoint();
                hoverBack = backBounds.contains(p);
                hoverTrollNormal = trollNormalBounds.contains(p);
                hoverTrollHell = trollHellBounds.contains(p);
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
        String titleStr = "Mini-Game Collection";
        g2.drawString(titleStr, (w - fm.stringWidth(titleStr)) / 2, h * 3 / 10);

        // Subtitle
        g2.setColor(new Color(220, 220, 210));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        fm = g2.getFontMetrics();
        String sub = "Endless modes from the game — for those who just want to play.";
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h * 3 / 10 + 36);

        // Mini-game entries
        int boxY = h * 4 / 10;
        int boxW = Math.min(500, w - 80);
        int boxX = (w - boxW) / 2;
        int boxH = h * 35 / 100;
        g2.setColor(new Color(40, 40, 60));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setColor(new Color(220, 220, 210));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        fm = g2.getFontMetrics();
        String gameTitle = "Troll Judgment Battle";
        g2.drawString(gameTitle, boxX + 20, boxY + 28);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.setColor(new Color(180, 180, 170));
        g2.drawString("Undertale-style bullet hell. Dodge the cancel culture.", boxX + 20, boxY + 48);
        g2.setColor(new Color(140, 160, 150));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Always available — no story progress required.", boxX + 20, boxY + 62);
        int tBtnW = 140;
        int tBtnH = 36;
        int tBy = boxY + 76;
        int normalX = boxX + 24;
        int hellX = boxX + boxW - 24 - tBtnW;
        trollNormalBounds.setBounds(normalX, tBy, tBtnW, tBtnH);
        trollHellBounds.setBounds(hellX, tBy, tBtnW, tBtnH);
        // Normal button
        double tScale = pressedTrollNormal ? 0.96 : 1.0;
        int tCw = (int) (tBtnW * tScale);
        int tCh = (int) (tBtnH * tScale);
        int tCx = normalX + (tBtnW - tCw) / 2;
        int tCy = tBy + (tBtnH - tCh) / 2;
        g2.setColor(new Color(50, 50, 70));
        g2.fillRoundRect(tCx, tCy, tCw, tCh, 10, 10);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(tCx, tCy, tCw, tCh, 10, 10);
        g2.setColor(new Color(245, 245, 240));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString("Normal", tCx + (tCw - g2.getFontMetrics().stringWidth("Normal")) / 2, tCy + (tCh + g2.getFontMetrics().getAscent()) / 2 - 2);
        if (hoverTrollNormal || pressedTrollNormal) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressedTrollNormal ? 0.4f : 0.2f));
            g2.setColor(new Color(0x1E90FF));
            g2.fillRoundRect(tCx, tCy, tCw, tCh, 10, 10);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        // Judgment Mode button
        tScale = pressedTrollHell ? 0.96 : 1.0;
        tCw = (int) (tBtnW * tScale);
        tCh = (int) (tBtnH * tScale);
        tCx = hellX + (tBtnW - tCw) / 2;
        tCy = tBy + (tBtnH - tCh) / 2;
        g2.setColor(new Color(50, 50, 70));
        g2.fillRoundRect(tCx, tCy, tCw, tCh, 10, 10);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(tCx, tCy, tCw, tCh, 10, 10);
        g2.setColor(new Color(245, 245, 240));
        g2.drawString("Judgment Mode", tCx + (tCw - g2.getFontMetrics().stringWidth("Judgment Mode")) / 2, tCy + (tCh + g2.getFontMetrics().getAscent()) / 2 - 2);
        if (hoverTrollHell || pressedTrollHell) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressedTrollHell ? 0.4f : 0.2f));
            g2.setColor(new Color(0x1E90FF));
            g2.fillRoundRect(tCx, tCy, tCw, tCh, 10, 10);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
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

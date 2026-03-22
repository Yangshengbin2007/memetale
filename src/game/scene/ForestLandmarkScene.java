package game.scene;

import game.scene.ForestOverworldMapScene.Landmark;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

/**
 * 单个地标子场景占位。显示地标名称、占位背景与「返回地图」按钮。
 * 后续可替换为对话/小游戏/Boss 等。图片、音乐可留空。
 */
public class ForestLandmarkScene extends JPanel implements Scene {
    private final Landmark landmark;
    private final Runnable onReturnToMap;
    private Image sceneBg;
    private boolean hoverReturn = false;
    private final Rectangle returnBounds = new Rectangle();

    /** 各地标对应的背景图文件名（无则留 null，画占位） */
    private static String bgFileForLandmark(String id) {
        switch (id) {
            case "doge_shrine":      return "doge_shrine.png";
            case "troll_cave":       return "troll_cave.png";
            case "radio_tower":      return "radio_tower.png";
            case "waterfall":        return "waterfall.png";
            case "gigachad_arena":   return "gigachad_arena.png";
            default:                 return null;
        }
    }

    public ForestLandmarkScene(Landmark landmark, Runnable onReturnToMap) {
        this.landmark = landmark;
        this.onReturnToMap = onReturnToMap;
        setBackground(new Color(40, 50, 35));
        loadBackground(bgFileForLandmark(landmark.id));
        initMouse();
    }

    private void loadBackground(String filename) {
        if (filename == null) return;
        String[] classpathCandidates = {
            "/image/Chapter one/" + filename,
            "/image/chapter%20one/" + filename,
            "/image/forest/" + filename,
            "/image/" + filename
        };
        String[] fileCandidates = {
            "image/Chapter one/" + filename,
            "image/forest/" + filename,
            "image/" + filename
        };
        for (String p : classpathCandidates) {
            URL url = getClass().getResource(p);
            if (url != null) {
                sceneBg = new ImageIcon(url).getImage();
                return;
            }
        }
        for (String p : fileCandidates) {
            File f = new File(p);
            if (f.exists()) {
                sceneBg = new ImageIcon(f.getAbsolutePath()).getImage();
                return;
            }
        }
    }

    private void initMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (returnBounds.contains(e.getPoint())) repaint();
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (returnBounds.contains(e.getPoint()) && onReturnToMap != null) {
                    onReturnToMap.run();
                }
                repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverReturn = false;
                repaint();
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                hoverReturn = returnBounds.contains(e.getPoint());
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (sceneBg != null) {
            int iw = sceneBg.getWidth(this);
            int ih = sceneBg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(sceneBg, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(35, 55, 40));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(140, 170, 140));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Scene: " + landmark.displayName + " (add " + bgFileForLandmark(landmark.id) + " for bg)";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 - 30);
            g2.drawString("Event / Mini-game / Boss — placeholder.", (w - fm.stringWidth("Event / Mini-game / Boss — placeholder.")) / 2, h / 2);
        }

        int btnW = 180;
        int btnH = 40;
        int bx = (w - btnW) / 2;
        int by = h - btnH - 40;
        returnBounds.setBounds(bx, by, btnW, btnH);

        g2.setColor(hoverReturn ? new Color(60, 100, 60) : new Color(40, 80, 40));
        g2.fillRoundRect(bx, by, btnW, btnH, 10, 10);
        g2.setColor(new Color(200, 220, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx, by, btnW, btnH, 10, 10);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        String text = "Return to Map";
        g2.drawString(text, bx + (btnW - fm.stringWidth(text)) / 2, by + (btnH + fm.getAscent()) / 2 - 2);

        g2.dispose();
    }

    @Override
    public void onEnter() {
        requestFocusInWindow();
    }

    @Override
    public void onExit() {}

    @Override
    public JPanel getPanel() {
        return this;
    }
}

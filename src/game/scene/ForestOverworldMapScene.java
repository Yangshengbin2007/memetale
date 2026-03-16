package game.scene;

import game.model.forest.ForestImageLoader;
import game.model.forest.ForestResources;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 森林大地图场景。显示一张大地图（与森林入口地图同源 map1forest.jpg），上有多个可点击地标。
 * 资源路径统一见 ForestResources / ForestImageLoader。
 */
public class ForestOverworldMapScene extends JPanel implements Scene {
    private Image mapImage;
    private final List<Landmark> landmarks = new ArrayList<>();
    private Landmark hoverLandmark = null;
    private final SceneManager sceneManager;

    public static final class Landmark {
        public final String id;
        public final String displayName;
        /** 在 800x600 下的参考区域，实际绘制时按当前宽高缩放 */
        public final int refX, refY, refW, refH;

        public Landmark(String id, String displayName, int refX, int refY, int refW, int refH) {
            this.id = id;
            this.displayName = displayName;
            this.refX = refX;
            this.refY = refY;
            this.refW = refW;
            this.refH = refH;
        }

        public Rectangle getBounds(int panelW, int panelH) {
            double sx = panelW / 800.0;
            double sy = panelH / 600.0;
            return new Rectangle(
                (int) (refX * sx),
                (int) (refY * sy),
                (int) (refW * sx),
                (int) (refH * sy)
            );
        }
    }

    public ForestOverworldMapScene(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        setBackground(new Color(25, 50, 25));
        initLandmarks();
        loadImage();
        initMouse();
    }

    private void initLandmarks() {
        landmarks.add(new Landmark("troll_cave", "Troll Cave", 80, 120, 110, 70));
        landmarks.add(new Landmark("doge_shrine", "Doge Shrine", 340, 60, 100, 75));
        landmarks.add(new Landmark("radio_tower", "Radio Tower", 580, 180, 100, 70));
        landmarks.add(new Landmark("merchant_camp", "Merchant Camp", 340, 380, 120, 75));
        landmarks.add(new Landmark("waterfall", "Waterfall", 600, 320, 100, 80));
        landmarks.add(new Landmark("mushroom_ring", "Mushroom Ring", 120, 360, 100, 80));
        landmarks.add(new Landmark("gigachad_arena", "Gigachad Arena", 570, 460, 140, 90));
    }

    private void loadImage() {
        mapImage = ForestImageLoader.loadBackground(ForestResources.BG_MAP);
    }

    private void initMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {}

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (hoverLandmark != null && sceneManager != null) {
                    Scene landmarkScene = new ForestLandmarkScene(hoverLandmark, () -> sceneManager.popScene());
                    sceneManager.pushScene(landmarkScene);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverLandmark = null;
                repaint();
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                Point p = e.getPoint();
                Landmark prev = hoverLandmark;
                hoverLandmark = null;
                for (Landmark lm : landmarks) {
                    if (lm.getBounds(getWidth(), getHeight()).contains(p)) {
                        hoverLandmark = lm;
                        break;
                    }
                }
                if (hoverLandmark != prev) repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (mapImage != null) {
            int iw = mapImage.getWidth(this);
            int ih = mapImage.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.min((double) w / iw, (double) h / ih);
                int sw = (int) (iw * scale);
                int sh = (int) (ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(mapImage, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(30, 60, 30));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(120, 160, 120));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Forest Overworld Map (map1forest.jpg)";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, 50);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            fm = g2.getFontMetrics();
            String sub = "Place map1forest.jpg in image/Chapter one/ or image/forest/ — Click the boxes below to enter locations.";
            int tw = fm.stringWidth(sub);
            if (tw > w - 40) {
                g2.drawString("Place map1forest.jpg in image/Chapter one/ or image/forest/", (w - fm.stringWidth("Place map1forest.jpg in image/Chapter one/ or image/forest/")) / 2, 78);
                g2.drawString("Click the boxes below to enter locations.", (w - fm.stringWidth("Click the boxes below to enter locations.")) / 2, 96);
            } else {
                g2.drawString(sub, (w - tw) / 2, 85);
            }
        }

        for (Landmark lm : landmarks) {
            Rectangle r = lm.getBounds(w, h);
            boolean hover = (hoverLandmark == lm);
            g2.setColor(hover ? new Color(255, 220, 100, 180) : new Color(255, 255, 200, 100));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g2.setColor(hover ? new Color(220, 180, 40) : new Color(180, 160, 60));
            g2.setStroke(new BasicStroke(hover ? 3f : 1.5f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            FontMetrics fm = g2.getFontMetrics();
            String name = lm.displayName;
            if (fm.stringWidth(name) > r.width - 4) {
                int i = 0;
                while (i < name.length() && fm.stringWidth(name.substring(0, i + 1)) <= r.width - 4) i++;
                name = name.substring(0, Math.max(1, i)) + "…";
            }
            g2.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 2);
        }

        g2.setColor(new Color(0, 0, 0, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click a landmark to explore. (Music placeholder)", 10, h - 8);

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

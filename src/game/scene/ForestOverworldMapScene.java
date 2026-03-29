package game.scene;

import game.model.GameState;
import game.model.forest.ForestImageLoader;
import game.model.forest.ForestResources;

import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Forest overworld map: same art as the forest entrance map (map1forest.jpg) with clickable landmarks.
 * Asset paths: {@link game.model.forest.ForestResources} / {@link game.model.forest.ForestImageLoader}.
 */
public class ForestOverworldMapScene extends JPanel implements Scene {
    private Image mapImage;
    private final List<Landmark> landmarks = new ArrayList<>();
    private Landmark hoverLandmark = null;
    private final SceneManager sceneManager;
    /** After Doge ending sequence: clear stack and return to title (wired from Main). */
    private final Runnable onDogeArcCompleteToTitle;
    /** Shared epilogue scene instance (pushed after {@link DogeShrineLazyScene}; wired from Main). */
    private final Scene dragonEpilogueScene;
    private Clip mapMusicClip;
    /** After Troll Cave + Doge done: click Troll = "We've been there"; click other = "We're going..." then black then go. */
    private static final int PHASE_NORMAL = 0;
    private static final int PHASE_BEEN_THERE_MSG = 1;
    private static final int PHASE_GOING_MSG = 2;
    private static final int PHASE_BLACK = 3;
    private static final int PHASE_WRONG_MSG = 4;
    private int mapPhase = PHASE_NORMAL;
    private String goingToMessage = "";
    private Landmark pendingDestination = null;
    private static final long MESSAGE_DURATION_MS = 2500;
    private long messageStartTime = 0;
    private Image knightImage;

    public static final class Landmark {
        public final String id;
        public final String displayName;
        /** Reference hit box in 800x600 space; scaled to the current panel size when hit-testing and drawing. */
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

    public ForestOverworldMapScene(SceneManager sceneManager, Runnable onDogeArcCompleteToTitle, Scene dragonEpilogueScene) {
        this.sceneManager = sceneManager;
        this.onDogeArcCompleteToTitle = onDogeArcCompleteToTitle != null ? onDogeArcCompleteToTitle : () -> {};
        this.dragonEpilogueScene = dragonEpilogueScene;
        setBackground(new Color(25, 50, 25));
        initLandmarks();
        loadImage();
        initMouse();
    }

    private void openLandmark(Landmark lm) {
        if (lm == null || sceneManager == null) return;
        if ("doge_shrine".equals(lm.id)) {
            sceneManager.pushScene(new DogeShrineLazyScene(sceneManager, onDogeArcCompleteToTitle, dragonEpilogueScene));
        } else {
            sceneManager.pushScene(new ForestLandmarkScene(lm, () -> sceneManager.popScene()));
        }
    }

    private void initLandmarks() {
        landmarks.add(new Landmark("troll_cave", "Troll Cave", 80, 120, 110, 70));
        landmarks.add(new Landmark("waterfall", "Waterfall", 600, 320, 100, 80));
        landmarks.add(new Landmark("radio_tower", "Radio Tower", 580, 180, 100, 70));
        landmarks.add(new Landmark("doge_shrine", "Doge Shrine", 120, 360, 100, 80));
        landmarks.add(new Landmark("gigachad_arena", "Gigachad Arena", 340, 60, 100, 75));
    }

    private void loadImage() {
        mapImage = ForestImageLoader.loadBackground(ForestResources.BG_MAP);
        knightImage = ForestImageLoader.loadCharacter("darabongba", "resentment");
        if (knightImage == null) knightImage = ForestImageLoader.loadCharacter("darabongba", ForestResources.DARABONGBA_RESENTMENT);
        if (knightImage == null) knightImage = ForestImageLoader.loadCharacter("darabongba", ForestResources.DARABONGBA_DEFAULT);
    }

    private void initMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {}

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (sceneManager == null) return;
                if (mapPhase == PHASE_BLACK) {
                    if (pendingDestination != null) {
                        openLandmark(pendingDestination);
                        pendingDestination = null;
                    }
                    mapPhase = PHASE_NORMAL;
                    repaint();
                    return;
                }
                if (mapPhase == PHASE_GOING_MSG) {
                    mapPhase = PHASE_BLACK;
                    repaint();
                    return;
                }
                if (mapPhase == PHASE_BEEN_THERE_MSG) {
                    mapPhase = PHASE_NORMAL;
                    repaint();
                    return;
                }
                if (mapPhase == PHASE_WRONG_MSG) {
                    mapPhase = PHASE_NORMAL;
                    repaint();
                    return;
                }
                boolean completedTrollAndDoge = GameState.getState().hasCompletedTrollCaveAndChoseDoge();
                if (hoverLandmark == null) {
                    if (completedTrollAndDoge) {
                        goingToMessage = "Darabongba: Wrong place. We are going to Doge Shrine.";
                        mapPhase = PHASE_WRONG_MSG;
                        messageStartTime = System.currentTimeMillis();
                        repaint();
                    }
                    return;
                }
                if (completedTrollAndDoge && "troll_cave".equals(hoverLandmark.id)) {
                    goingToMessage = "Darabongba: We've been there. Next stop: Doge Shrine.";
                    mapPhase = PHASE_WRONG_MSG;
                    messageStartTime = System.currentTimeMillis();
                    repaint();
                    return;
                }
                if (completedTrollAndDoge) {
                    if ("doge_shrine".equals(hoverLandmark.id)) {
                        goingToMessage = "We're going to Doge Shrine.";
                        pendingDestination = hoverLandmark;
                        mapPhase = PHASE_BLACK; // click Doge -> directly black
                        messageStartTime = System.currentTimeMillis();
                        repaint();
                        return;
                    }
                    goingToMessage = "Darabongba: Wrong place. We are going to Doge Shrine.";
                    mapPhase = PHASE_WRONG_MSG;
                    messageStartTime = System.currentTimeMillis();
                    repaint();
                    return;
                }
                openLandmark(hoverLandmark);
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

        if (mapPhase == PHASE_BLACK) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(180, 180, 170));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Click to continue.", w / 2 - 55, h / 2 + 8);
            g2.dispose();
            return;
        }

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

        if (mapPhase == PHASE_WRONG_MSG) {
            drawWrongDestinationDialogue(g2, w, h, goingToMessage == null || goingToMessage.isEmpty()
                ? "Darabongba: Wrong place. We are going to Doge Shrine."
                : goingToMessage);
        } else if (mapPhase == PHASE_BEEN_THERE_MSG || mapPhase == PHASE_GOING_MSG) {
            if (System.currentTimeMillis() - messageStartTime > MESSAGE_DURATION_MS && mapPhase == PHASE_BEEN_THERE_MSG)
                mapPhase = PHASE_NORMAL;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g2.setColor(new Color(20, 20, 25));
            g2.fillRoundRect(w / 2 - 200, h / 2 - 30, 400, 60, 12, 12);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(200, 160, 60));
            g2.drawRoundRect(w / 2 - 200, h / 2 - 30, 400, 60, 12, 12);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(new Color(240, 238, 230));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(goingToMessage, w / 2 - fm.stringWidth(goingToMessage) / 2, h / 2 + 8);
            if (mapPhase == PHASE_GOING_MSG)
                g2.drawString("Click again to go.", w / 2 - fm.stringWidth("Click again to go.") / 2, h / 2 + 28);
        }

        g2.setColor(new Color(0, 0, 0, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click a landmark to explore.", 10, h - 8);

        g2.dispose();
    }

    /** Wrong landmark reminder: knight-only style similar to entrance wrong-click dialogue. */
    private void drawWrongDestinationDialogue(Graphics2D g2, int w, int h, String msg) {
        int boxH = (int) (h * 0.22);
        int boxY = h - boxH;
        int charH = (int) (h * 0.60);
        int charY = boxY - charH;
        int charW = (int) (w * 0.36);
        int rightX = (int) (w * 0.52);
        if (knightImage != null) {
            int iw = knightImage.getWidth(this), ih = knightImage.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.min((double) charW / iw, (double) charH / ih);
                int sw = (int) (iw * scale), sh = (int) (ih * scale);
                int sx = rightX + (charW - sw) / 2;
                int sy = charY + charH - sh;
                g2.drawImage(knightImage, sx, sy, sw, sh, this);
            }
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(0, boxY - 8, w, boxH + 16, 14, 14);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(2, boxY - 6, w - 4, boxH + 12, 14, 14);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        int pad = 16;
        int y = boxY + pad;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(220, 180, 80));
        g2.drawString("Darabongba", pad, y + g2.getFontMetrics().getAscent());
        y += 26;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.setColor(new Color(240, 240, 235));
        g2.drawString(msg, pad, y + g2.getFontMetrics().getAscent());
        g2.setColor(new Color(120, 120, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click to close", w - 100, h - 12);
    }

    @Override
    public void onEnter() {
        GameState.getState().setCurrentScene("forest_overworld_map");
        mapPhase = PHASE_NORMAL;
        pendingDestination = null;
        if (mapMusicClip != null) {
            try { if (mapMusicClip.isRunning()) mapMusicClip.stop(); mapMusicClip.close(); } catch (Exception ignore) {}
            mapMusicClip = null;
        }
        mapMusicClip = StartScene.loadMusicFromMusicDir("map.wav");
        if (mapMusicClip == null) mapMusicClip = StartScene.loadMusicFromMusicDir("map.mp3");
        if (mapMusicClip != null) {
            // Map BGM should always follow music volume directly (no low-volume cutoff).
            StartScene.applyVolumeToClipForSceneNoFloor(mapMusicClip, true);
            mapMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            mapMusicClip.start();
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (mapMusicClip != null) {
            try { if (mapMusicClip.isRunning()) mapMusicClip.stop(); mapMusicClip.close(); } catch (Exception ignore) {}
            mapMusicClip = null;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

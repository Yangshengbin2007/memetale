package game.scene;

import game.model.forest.ForestEntranceData;
import game.model.forest.ForestImageLoader;
import game.model.forest.ForestResources;
import game.model.GameState;
import game.model.forest.TrollCaveData;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * After Troll battle: post-battle dialogue, then Doge Shrine discussion, then black screen.
 * Prince left, Darabongba right; Troll on right when his lines show. Click/Space to advance. All English.
 */
public class TrollCavePostBattleScene extends JPanel implements Scene {
    private final Runnable onCompleteToMap;
    private final Runnable onQuitToTitle;

    private Image bgCave;
    private Image bgMap;
    private final Map<String, Image> princeImages = new HashMap<>();
    private final Map<String, Image> darabongbaImages = new HashMap<>();
    private final Map<String, Image> trollBossImages = new HashMap<>();

    /** 0 = post-battle, 1 = doge shrine. Within block we use lineIndex. */
    private int blockIndex = 0;
    private int lineIndex = 0;
    private static final float DIM_ALPHA = 0.45f;
    private static final int DIALOGUE_BOX_HEIGHT_RATIO = 22;
    private static final double CHARACTER_HEIGHT_RATIO = 2.0 / 3.0;
    private static final double CHARACTER_KNEE_ALIGN_RATIO = 0.6;
    private static final int TEXT_DURATION_MS = 1000;
    private static final int TEXT_ANIM_DELAY_MS = 40;
    private static final int SPACE_HOLD_MS = 3000;
    private static final int FAST_FORWARD_INTERVAL_MS = 100;

    private long lineStartTime = 0L;
    private boolean spaceKeyHeld = false;
    private boolean fastForwardActive = false;
    private javax.swing.Timer holdSpaceTimer;
    private javax.swing.Timer fastForwardTimer;

    /** After last line we show black screen; one more click/space calls onCompleteToMap. */
    private boolean blackScreen = false;
    private Clip postBattleMusicClip;

    public TrollCavePostBattleScene(Runnable onCompleteToMap, Runnable onQuitToTitle) {
        this.onCompleteToMap = onCompleteToMap;
        this.onQuitToTitle = onQuitToTitle;
        setBackground(new Color(30, 20, 40));
        loadAllImages();
        initSpaceTimers();
        initMouse();
        initKey();
    }

    private static String[][] getLines(int block) {
        if (block == 0) return TrollCaveData.POST_BATTLE_LINES;
        return TrollCaveData.DOGE_SHRINE_LINES;
    }

    private static int getBlockLength(int block) {
        return getLines(block).length;
    }

    private void loadAllImages() {
        bgCave = ForestImageLoader.loadBackground("troll cave.png");
        if (bgCave == null) bgCave = ForestImageLoader.loadBackground("troll_cave.png");
        bgMap = ForestImageLoader.loadBackground(ForestResources.BG_MAP);
        if (bgMap == null) bgMap = ForestImageLoader.loadBackground("map1forest.jpg");
        loadPrince(ForestResources.PRINCE_DEFAULT);
        loadPrince(ForestResources.PRINCE_SURPRISE);
        loadPrince(ForestResources.PRINCE_ANNOYED);
        loadPrince(ForestResources.PRINCE_THOUGHTFUL);
        loadDarabongba(ForestResources.DARABONGBA_DEFAULT);
        loadDarabongba(ForestResources.DARABONGBA_SURPRISE);
        loadDarabongba(ForestResources.DARABONGBA_RESENTMENT);
        loadDarabongba(ForestResources.DARABONGBA_NERVOUS);
        loadTrollBoss(TrollCaveData.TROLL_DEFAULT);
        loadTrollBoss(TrollCaveData.TROLL_ANGRY);
    }

    private void loadPrince(String expr) {
        String key = expr.startsWith("prince_") ? expr.substring(7) : expr;
        if (princeImages.containsKey(key)) return;
        Image img = ForestImageLoader.loadCharacter("prince", key);
        if (img != null) princeImages.put(key, img);
    }

    private void loadDarabongba(String expr) {
        String key = expr.startsWith("darabongba_") ? expr.substring(11) : expr;
        if (darabongbaImages.containsKey(key)) return;
        Image img = ForestImageLoader.loadCharacter("darabongba", key);
        if (img != null) darabongbaImages.put(key, img);
    }

    private void loadTrollBoss(String expr) {
        if (trollBossImages.containsKey(expr)) return;
        Image img = ForestImageLoader.loadCharacter("troll_boss", expr);
        if (img != null) trollBossImages.put(expr, img);
    }

    private Image getPrinceImage(String expr) {
        if (expr == null) expr = ForestEntranceData.EXPR_DEFAULT;
        String key = expr.startsWith("prince_") ? expr.substring(7) : expr;
        if (!princeImages.containsKey(key)) loadPrince("prince_" + key);
        Image img = princeImages.get(key);
        return img != null ? img : princeImages.get("default");
    }

    private Image getDarabongbaImage(String expr) {
        if (expr == null) expr = ForestEntranceData.EXPR_DEFAULT;
        String key = expr.startsWith("darabongba_") ? expr.substring(11) : expr;
        if (!darabongbaImages.containsKey(key)) loadDarabongba("darabongba_" + key);
        Image img = darabongbaImages.get(key);
        return img != null ? img : darabongbaImages.get("default");
    }

    private Image getTrollBossImage(String expr) {
        if (expr == null) expr = TrollCaveData.TROLL_DEFAULT;
        if (!trollBossImages.containsKey(expr)) loadTrollBoss(expr);
        Image img = trollBossImages.get(expr);
        return img != null ? img : trollBossImages.get(TrollCaveData.TROLL_DEFAULT);
    }

    private void initSpaceTimers() {
        holdSpaceTimer = new javax.swing.Timer(SPACE_HOLD_MS, e -> {
            if (spaceKeyHeld && !blackScreen) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning()) fastForwardTimer.start();
            }
            if (holdSpaceTimer != null) holdSpaceTimer.setRepeats(false);
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive) return;
            advance();
        });
    }

    private void initKey() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    spaceKeyHeld = true;
                    advance();
                    if (holdSpaceTimer != null && !holdSpaceTimer.isRunning()) {
                        holdSpaceTimer.setInitialDelay(SPACE_HOLD_MS);
                        holdSpaceTimer.start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    spaceKeyHeld = false;
                    fastForwardActive = false;
                    if (fastForwardTimer != null && fastForwardTimer.isRunning()) fastForwardTimer.stop();
                }
            }
        });
    }

    private void advance() {
        if (blackScreen) {
            GameState.getState().setHasCompletedTrollCaveAndChoseDoge(true);
            if (onCompleteToMap != null) onCompleteToMap.run();
            return;
        }
        String[][] lines = getLines(blockIndex);
        if (lineIndex >= lines.length) {
            if (blockIndex == 0) {
                blockIndex = 1;
                lineIndex = 0;
                lineStartTime = System.currentTimeMillis();
            } else {
                blackScreen = true;
            }
            repaint();
            return;
        }
        String[] line = lines[lineIndex];
        String text = line[1];
        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars) {
            lineStartTime = System.currentTimeMillis() - TEXT_DURATION_MS;
            repaint();
            return;
        }
        lineIndex++;
        lineStartTime = System.currentTimeMillis();
        if (lineIndex >= lines.length && blockIndex == 0) {
            blockIndex = 1;
            lineIndex = 0;
            lineStartTime = System.currentTimeMillis();
        } else if (lineIndex >= getLines(blockIndex).length && blockIndex == 1) {
            blackScreen = true;
        }
        repaint();
    }

    private void initMouse() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                advance();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (blackScreen) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(180, 180, 170));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Click or press Space to continue.", w / 2 - 120, h / 2 + 8);
            g2.dispose();
            return;
        }

        Image bg = (blockIndex == 1 && bgMap != null) ? bgMap : bgCave;
        if (bg != null) {
            int iw = bg.getWidth(this), ih = bg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale), sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2, y = (h - sh) / 2;
                g2.drawImage(bg, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(40, 30, 50));
            g2.fillRect(0, 0, w, h);
        }

        String[][] lines = getLines(blockIndex);
        if (lineIndex >= lines.length) {
            g2.dispose();
            return;
        }
        String[] line = lines[lineIndex];
        String speaker = line[0];
        String text = line[1];
        String princeExpr = line[2];
        String darabongbaExpr = line[3];
        String trollExpr = line[4];

        boolean princeSpeaking = "Prince".equals(speaker);
        boolean darabongbaSpeaking = "Darabongba".equals(speaker);
        boolean trollSpeaking = "Troll Boss".equals(speaker);
        boolean showTrollRight = "Troll Boss".equals(speaker) || (lineIndex > 0 && "Troll Boss".equals(lines[lineIndex - 1][0]));
        for (int i = 0; i <= lineIndex; i++) {
            if ("Troll Boss".equals(lines[i][0])) { showTrollRight = true; break; }
        }

        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars) repaint(TEXT_ANIM_DELAY_MS);

        int boxH = h * DIALOGUE_BOX_HEIGHT_RATIO / 100;
        int boxY = h - boxH;
        int charH = (int) (h * CHARACTER_HEIGHT_RATIO);
        int charY = boxY - (int) (charH * CHARACTER_KNEE_ALIGN_RATIO);
        int charW = (int) (w * 0.38);
        int leftX = (int) (w * 0.06);
        int rightX = (int) (w * 0.52);

        if (showTrollRight && "Troll Boss".equals(speaker)) {
            Image trollImg = getTrollBossImage(trollExpr);
            if (trollImg != null) {
                float alpha = trollSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, trollImg, rightX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            if (darabongbaSpeaking) {
                Image daraImg = getDarabongbaImage(darabongbaExpr);
                if (daraImg != null) drawCharacter(g2, daraImg, leftX, charY, charW, charH, true);
            } else {
                Image princeImg = getPrinceImage(princeExpr);
                if (princeImg != null) {
                    float alpha = princeSpeaking ? 1f : DIM_ALPHA;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    drawCharacter(g2, princeImg, leftX, charY, charW, charH, false);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }
        } else {
            Image princeImg = getPrinceImage(princeExpr);
            if (princeImg != null) {
                float alpha = princeSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, princeImg, leftX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            Image daraImg = getDarabongbaImage(darabongbaExpr);
            if (daraImg != null) {
                float alpha = darabongbaSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, daraImg, rightX, charY, charW, charH, ForestEntranceData.mirrorDarabongba(darabongbaExpr));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
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
        if (!speaker.isEmpty()) g2.drawString(speaker, pad, y + g2.getFontMetrics().getAscent());
        y += 26;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.setColor(new Color(240, 240, 235));
        String visibleText = text.substring(0, Math.min(visibleChars, text.length()));
        drawWrappedText(g2, visibleText, pad, y, w - pad * 2, 22);
        g2.setColor(new Color(120, 120, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click / Space | Hold Space to skip", w - 260, h - 12);
        g2.dispose();
    }

    private void drawCharacter(Graphics2D g2, Image img, int x, int y, int maxW, int maxH, boolean mirror) {
        int iw = img.getWidth(this), ih = img.getHeight(this);
        if (iw <= 0 || ih <= 0) return;
        double scale = Math.min((double) maxW / iw, (double) maxH / ih);
        int sw = (int) (iw * scale), sh = (int) (ih * scale);
        int sx = x + (maxW - sw) / 2;
        int sy = y + maxH - sh;
        if (mirror) {
            AffineTransform at = g2.getTransform();
            g2.translate(sx + sw, sy);
            g2.scale(-1, 1);
            g2.translate(-sx - sw, -sy);
            g2.drawImage(img, sx, sy, sw, sh, this);
            g2.setTransform(at);
        } else {
            g2.drawImage(img, sx, sy, sw, sh, this);
        }
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxW, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.isEmpty() ? new String[0] : text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(tryLine) <= maxW) {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            } else {
                if (line.length() > 0) {
                    g2.drawString(line.toString(), x, currentY + fm.getAscent());
                    currentY += lineHeight;
                }
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) g2.drawString(line.toString(), x, currentY + fm.getAscent());
    }

    @Override
    public void onEnter() {
        blockIndex = 0;
        lineIndex = 0;
        blackScreen = false;
        lineStartTime = System.currentTimeMillis();
        if (postBattleMusicClip != null) {
            try { if (postBattleMusicClip.isRunning()) postBattleMusicClip.stop(); postBattleMusicClip.close(); } catch (Exception ignore) {}
            postBattleMusicClip = null;
        }
        postBattleMusicClip = StartScene.loadMusicFromMusicDir("trollcave.wav");
        if (postBattleMusicClip == null) postBattleMusicClip = StartScene.loadMusicFromMusicDir("trollcave.mp3");
        if (postBattleMusicClip != null) {
            StartScene.applyVolumeToClipForScene(postBattleMusicClip, true);
            postBattleMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            postBattleMusicClip.start();
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (holdSpaceTimer != null) holdSpaceTimer.stop();
        if (fastForwardTimer != null) fastForwardTimer.stop();
        if (postBattleMusicClip != null) {
            try { if (postBattleMusicClip.isRunning()) postBattleMusicClip.stop(); postBattleMusicClip.close(); } catch (Exception ignore) {}
            postBattleMusicClip = null;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

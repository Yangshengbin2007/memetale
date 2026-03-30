package game.scene;

import game.model.DialogueRecord;
import game.model.forest.ForestEntranceData;
import game.model.forest.ForestImageLoader;
import game.model.forest.ForestResources;
import game.model.GameState;
import game.model.StoryState;
import game.io.SaveLoad;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

/**
 * After Troll battle: post-battle dialogue, then Doge Shrine discussion, then black screen.
 * Prince left, Darabongba right; Troll on right when his lines show. Click/Space to advance; hold Space to skip.
 * ESC: Save / Load / Settings / History / Quit to title (same as cave/forest). Progress and History sync with {@link StoryState}.
 * All English.
 */
public class TrollCavePostBattleScene extends JPanel implements Scene {
    /**
     * Indirection: reflectively reads the real {@code game.model.forest.TrollCaveData} class.
     * Avoids IDE/linter false positives about cross-package constants in this project layout.
     */
    private static final class TrollCaveData {
        private static final String DATA_CLASS_NAME = "game.model.forest.TrollCaveData";

        private static String getStringField(String fieldName) {
            try {
                Class<?> c = Class.forName(DATA_CLASS_NAME);
                Object v = c.getField(fieldName).get(null);
                return v instanceof String s ? s : String.valueOf(v);
            } catch (Exception e) {
                return fieldName;
            }
        }

        private static String[][] getString2dField(String fieldName) {
            try {
                Class<?> c = Class.forName(DATA_CLASS_NAME);
                Object v = c.getField(fieldName).get(null);
                return (String[][]) v;
            } catch (Exception e) {
                return new String[0][0];
            }
        }

        public static final String TROLL_DEFAULT = getStringField("TROLL_DEFAULT");
        public static final String TROLL_ANGRY = getStringField("TROLL_ANGRY");
        public static final String[][] POST_BATTLE_LINES = getString2dField("POST_BATTLE_LINES");
        public static final String[][] DOGE_SHRINE_LINES = getString2dField("DOGE_SHRINE_LINES");

        static int getMainCaveLinesLength() {
            try {
                Class<?> c = Class.forName(DATA_CLASS_NAME);
                Object v = c.getField("LINES").get(null);
                return ((String[][]) v).length;
            } catch (Exception e) {
                return 0;
            }
        }
    }

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
    private static final double TROLL_CHARACTER_HEIGHT_RATIO = 0.46;
    private static final double TROLL_CHARACTER_WIDTH_RATIO = 0.30;
    private static final int TEXT_DURATION_MS = 1000;
    private static final int TEXT_ANIM_DELAY_MS = 40;
    private static final int SPACE_HOLD_MS = 350;
    private static final int FAST_FORWARD_INTERVAL_MS = 100;

    private long lineStartTime = 0L;
    private boolean spaceKeyHeld = false;
    private boolean fastForwardActive = false;
    private javax.swing.Timer holdSpaceTimer;
    private javax.swing.Timer fastForwardTimer;

    /** After last line we show black screen; one more click/space calls onCompleteToMap. */
    private boolean blackScreen = false;
    private Clip postBattleMusicClip;

    private Runnable navigateAfterLoad;

    private boolean pauseMenuVisible = false;
    private boolean hoverPauseSave, hoverPauseLoad, hoverPauseSettings, hoverPauseHistory, hoverPauseQuit;
    private boolean pressedPauseSave, pressedPauseLoad, pressedPauseSettings, pressedPauseHistory, pressedPauseQuit;
    private final Rectangle pauseSaveBounds = new Rectangle();
    private final Rectangle pauseLoadBounds = new Rectangle();
    private final Rectangle pauseSettingsBounds = new Rectangle();
    private final Rectangle pauseHistoryBounds = new Rectangle();
    private final Rectangle pauseQuitBounds = new Rectangle();

    /** Dedup history: last recorded (block, line). */
    private int lastHistoryBlock = -1;
    private int lastHistoryLine = -1;

    public void setNavigateAfterLoad(Runnable navigateAfterLoad) {
        this.navigateAfterLoad = navigateAfterLoad;
    }

    public TrollCavePostBattleScene(Runnable onCompleteToMap, Runnable onQuitToTitle) {
        this.onCompleteToMap = onCompleteToMap;
        this.onQuitToTitle = onQuitToTitle;
        setBackground(new Color(30, 20, 40));
        loadAllImages();
        initSpaceTimers();
        initMouse();
        initKey();
    }

    private void stopPostBattleBgm() {
        if (postBattleMusicClip != null) {
            try { if (postBattleMusicClip.isRunning()) postBattleMusicClip.stop(); postBattleMusicClip.close(); } catch (Exception ignore) {}
            postBattleMusicClip = null;
        }
    }

    /** Background is map1forest.jpg: only map BGM (stops troll cave music). */
    private void startMapBgmOnly() {
        stopPostBattleBgm();
        postBattleMusicClip = StartScene.loadMusicFromMusicDir("map.wav");
        if (postBattleMusicClip == null) postBattleMusicClip = StartScene.loadMusicFromMusicDir("map.mp3");
        if (postBattleMusicClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(postBattleMusicClip, true);
            postBattleMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            postBattleMusicClip.start();
        }
    }

    private void startTrollCaveBgmOnly() {
        stopPostBattleBgm();
        postBattleMusicClip = StartScene.loadMusicFromMusicDir("trollcave.wav");
        if (postBattleMusicClip == null) postBattleMusicClip = StartScene.loadMusicFromMusicDir("trollcave.mp3");
        if (postBattleMusicClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(postBattleMusicClip, true);
            postBattleMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            postBattleMusicClip.start();
        }
    }

    private static String[][] getLines(int block) {
        if (block == 0) return TrollCaveData.POST_BATTLE_LINES;
        return TrollCaveData.DOGE_SHRINE_LINES;
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
            if (spaceKeyHeld && !blackScreen && !pauseMenuVisible) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning()) fastForwardTimer.start();
            }
            if (holdSpaceTimer != null) holdSpaceTimer.setRepeats(false);
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive || pauseMenuVisible) return;
            advance();
        });
    }

    private void initKey() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    pauseMenuVisible = !pauseMenuVisible;
                    repaint();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !pauseMenuVisible) {
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

    private void persistPostBattleProgress() {
        StoryState st = GameState.getState();
        st.setPostBattleBlockIndex(blockIndex);
        st.setPostBattleLineIndex(lineIndex);
        st.setPostBattleBlackScreen(blackScreen);
    }

    /** Restore block/line/blackScreen and BGM from current {@link StoryState} in {@link GameState}. */
    private void applyPostBattleProgressFromStoryState() {
        StoryState st = GameState.getState();
        blackScreen = st.isPostBattleBlackScreen();
        blockIndex = st.getPostBattleBlockIndex();
        lineIndex = st.getPostBattleLineIndex();
        String[][] L0 = TrollCaveData.POST_BATTLE_LINES;
        String[][] L1 = TrollCaveData.DOGE_SHRINE_LINES;
        if (blockIndex < 0) blockIndex = 0;
        if (blockIndex > 1) blockIndex = 1;
        if (blackScreen) {
            blockIndex = 1;
            lineIndex = Math.min(Math.max(0, lineIndex), L1.length);
            stopPostBattleBgm();
            return;
        }
        if (blockIndex == 0 && L0.length > 0 && lineIndex >= L0.length) {
            blockIndex = 1;
            lineIndex = 0;
        }
        if (blockIndex == 0) {
            lineIndex = Math.min(Math.max(0, lineIndex), L0.length == 0 ? 0 : L0.length - 1);
            startTrollCaveBgmOnly();
        } else {
            if (L1.length > 0 && lineIndex >= L1.length) {
                blackScreen = true;
                lineIndex = L1.length;
                stopPostBattleBgm();
                st.setPostBattleBlackScreen(true);
                st.setPostBattleLineIndex(lineIndex);
                return;
            }
            lineIndex = Math.min(Math.max(0, lineIndex), L1.length == 0 ? 0 : L1.length - 1);
            startMapBgmOnly();
        }
    }

    private void recordLineToHistory(int block, int lineIdx) {
        String[][] lines = getLines(block);
        if (lineIdx < 0 || lineIdx >= lines.length) return;
        if (block == lastHistoryBlock && lineIdx == lastHistoryLine) return;
        String[] line = lines[lineIdx];
        GameState.getState().addTrollCaveHistory(line[0], line[1]);
        lastHistoryBlock = block;
        lastHistoryLine = lineIdx;
    }

    private void advance() {
        if (blackScreen) {
            GameState.getState().setHasCompletedTrollCaveAndChoseDoge(true);
            GameState.getState().setPostBattleBlackScreen(false);
            stopPostBattleBgm();
            if (onCompleteToMap != null) onCompleteToMap.run();
            return;
        }
        String[][] lines = getLines(blockIndex);
        if (lineIndex >= lines.length) {
            if (blockIndex == 0) {
                blockIndex = 1;
                lineIndex = 0;
                lineStartTime = System.currentTimeMillis();
                startMapBgmOnly();
            } else {
                blackScreen = true;
                stopPostBattleBgm();
            }
            persistPostBattleProgress();
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
        recordLineToHistory(blockIndex, lineIndex);
        lineIndex++;
        lineStartTime = System.currentTimeMillis();
        if (lineIndex >= lines.length && blockIndex == 0) {
            blockIndex = 1;
            lineIndex = 0;
            lineStartTime = System.currentTimeMillis();
            startMapBgmOnly();
        } else if (lineIndex >= getLines(blockIndex).length && blockIndex == 1) {
            blackScreen = true;
            stopPostBattleBgm();
        }
        persistPostBattleProgress();
        repaint();
    }

    private void initMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (pauseMenuVisible) {
                    Point p = e.getPoint();
                    pressedPauseSave = pauseSaveBounds.contains(p);
                    pressedPauseLoad = pauseLoadBounds.contains(p);
                    pressedPauseSettings = pauseSettingsBounds.contains(p);
                    pressedPauseHistory = pauseHistoryBounds.contains(p);
                    pressedPauseQuit = pauseQuitBounds.contains(p);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                requestFocusInWindow();
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    if (pressedPauseSave && pauseSaveBounds.contains(p)) handleSave();
                    else if (pressedPauseLoad && pauseLoadBounds.contains(p)) handleLoad();
                    else if (pressedPauseSettings && pauseSettingsBounds.contains(p)) handleSettings();
                    else if (pressedPauseHistory && pauseHistoryBounds.contains(p)) handleHistory();
                    else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                        int c = JOptionPane.showConfirmDialog(TrollCavePostBattleScene.this, "Are you sure you want to quit?", "Quit to Title", JOptionPane.YES_NO_OPTION);
                        if (c == JOptionPane.YES_OPTION && onQuitToTitle != null) {
                            stopPostBattleBgm();
                            onQuitToTitle.run();
                        }
                    } else if (!pauseSaveBounds.contains(p) && !pauseLoadBounds.contains(p) && !pauseSettingsBounds.contains(p) && !pauseHistoryBounds.contains(p) && !pauseQuitBounds.contains(p)) {
                        pauseMenuVisible = false;
                    }
                    pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                    repaint();
                    return;
                }
                advance();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!pauseMenuVisible) {
                    hoverPauseSave = hoverPauseLoad = hoverPauseSettings = hoverPauseHistory = hoverPauseQuit = false;
                    return;
                }
                Point p = e.getPoint();
                hoverPauseSave = pauseSaveBounds.contains(p);
                hoverPauseLoad = pauseLoadBounds.contains(p);
                hoverPauseSettings = pauseSettingsBounds.contains(p);
                hoverPauseHistory = pauseHistoryBounds.contains(p);
                hoverPauseQuit = pauseQuitBounds.contains(p);
                repaint();
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
            g2.setColor(new Color(100, 100, 100));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString("ESC: Save / Load / Settings / History", w / 2 - 140, h / 2 + 36);
            if (pauseMenuVisible) paintPauseMenu(g2, w, h);
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
        int trollH = (int) (h * TROLL_CHARACTER_HEIGHT_RATIO);
        int trollW = (int) (w * TROLL_CHARACTER_WIDTH_RATIO);
        int trollY = boxY - trollH; // align troll bottom to dialogue top
        int leftX = (int) (w * 0.06);
        int rightX = (int) (w * 0.52);

        if (showTrollRight && "Troll Boss".equals(speaker)) {
            Image trollImg = getTrollBossImage(trollExpr);
            if (trollImg != null) {
                float alpha = trollSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, trollImg, rightX, trollY, trollW, trollH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            if (darabongbaSpeaking) {
                Image daraImg = getDarabongbaImage(darabongbaExpr);
                if (daraImg != null) drawCharacter(g2, daraImg, leftX, charY, charW, charH, false);
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
                drawCharacter(g2, daraImg, rightX, charY, charW, charH, false);
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
        g2.drawString("ESC: menu | Click / Space | Hold Space to skip", w - 340, h - 12);
        if (pauseMenuVisible) paintPauseMenu(g2, w, h);
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

    private void paintPauseMenu(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        int panelW = (int) (w * 0.5), panelH = (int) (h * 0.55);
        int px = (w - panelW) / 2, py = (h - panelH) / 2;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(40, 40, 40, 230));
        g2.fillRoundRect(px, py, panelW, panelH, 18, 18);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(px + 1, py + 1, panelW - 2, panelH - 2, 18, 18);
        int btnW = (int) (panelW * 0.7), btnH = (int) (panelH * 0.12);
        int spacing = (panelH - btnH * 5) / 6;
        int x = w / 2 - btnW / 2, y = py + spacing;
        pauseSaveBounds.setBounds(x, y, btnW, btnH);
        y += btnH + spacing;
        pauseLoadBounds.setBounds(x, y, btnW, btnH);
        y += btnH + spacing;
        pauseSettingsBounds.setBounds(x, y, btnW, btnH);
        y += btnH + spacing;
        pauseHistoryBounds.setBounds(x, y, btnW, btnH);
        y += btnH + spacing;
        pauseQuitBounds.setBounds(x, y, btnW, btnH);
        paintPauseBtn(g2, pauseSaveBounds, "Save", hoverPauseSave, pressedPauseSave);
        paintPauseBtn(g2, pauseLoadBounds, "Load", hoverPauseLoad, pressedPauseLoad);
        paintPauseBtn(g2, pauseSettingsBounds, "Settings", hoverPauseSettings, pressedPauseSettings);
        paintPauseBtn(g2, pauseHistoryBounds, "History", hoverPauseHistory, pressedPauseHistory);
        paintPauseBtn(g2, pauseQuitBounds, "Quit to Title", hoverPauseQuit, pressedPauseQuit);
    }

    private void paintPauseBtn(Graphics2D g2, Rectangle r, String text, boolean hover, boolean pressed) {
        double scale = pressed ? 0.96 : 1.0;
        int cw = (int) (r.width * scale), ch = (int) (r.height * scale);
        int cx = r.x + (r.width - cw) / 2, cy = r.y + (r.height - ch) / 2;
        g2.setColor(new Color(10, 60, 110));
        g2.fillRoundRect(cx, cy, cw, ch, 16, 16);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(cx, cy, cw, ch, 16, 16);
        g2.setColor(new Color(245, 245, 240));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx + (cw - fm.stringWidth(text)) / 2, cy + (ch + fm.getAscent()) / 2);
        if (hover || pressed) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressed ? 0.4f : 0.2f));
            g2.setColor(new Color(0x1E90FF));
            g2.fillRoundRect(cx, cy, cw, ch, 16, 16);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    private void handleSave() {
        StoryState state = GameState.getState();
        state.setSavedChapter(1);
        state.setCurrentScene("troll_cave_post_battle");
        int caveLen = TrollCaveData.getMainCaveLinesLength();
        state.setTrollCaveDialogueIndex(caveLen);
        state.setPostBattleBlockIndex(blockIndex);
        state.setPostBattleLineIndex(lineIndex);
        state.setPostBattleBlackScreen(blackScreen);
        int lastSlot = state.getLastUsedSaveSlot();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Save (last used: Slot " + lastSlot + ")", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                state.setLastUsedSaveSlot(slot);
                try {
                    SaveLoad.save(state, "saves/slot" + slot + ".dat");
                    JOptionPane.showMessageDialog(TrollCavePostBattleScene.this, "Saved to slot " + slot);
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TrollCavePostBattleScene.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            grid.add(b);
        }
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.add(grid, BorderLayout.CENTER);
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(main);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void handleLoad() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Load", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            int ch = SaveLoad.getSavedChapter("saves/slot" + slot + ".dat");
            String label = ch == 0 ? "Slot " + slot : "Slot " + slot + " (Ch." + ch + ")";
            JButton b = new JButton(label);
            b.addActionListener(ev -> {
                try {
                    StoryState loaded = SaveLoad.load("saves/slot" + slot + ".dat");
                    if (loaded == null) {
                        JOptionPane.showMessageDialog(TrollCavePostBattleScene.this, "Empty slot " + slot);
                        return;
                    }
                    if (loaded.getSavedChapter() >= 2) {
                        JOptionPane.showMessageDialog(TrollCavePostBattleScene.this, "Chapter not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    GameState.setState(loaded);
                    lastHistoryBlock = -1;
                    lastHistoryLine = -1;
                    applyPostBattleProgressFromStoryState();
                    lineStartTime = System.currentTimeMillis();
                    dialog.dispose();
                    pauseMenuVisible = false;
                    if (navigateAfterLoad != null) navigateAfterLoad.run();
                    repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TrollCavePostBattleScene.this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            grid.add(b);
        }
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.add(grid, BorderLayout.CENTER);
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(main);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void handleSettings() {
        float vol = StartScene.getMasterVolume();
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(40, 40, 50));
        JSlider slider = new JSlider(0, 100, (int) (vol * 100));
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(25);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!slider.isEnabled()) return;
                int sw = slider.getWidth(), x = e.getX();
                int min = slider.getMinimum(), max = slider.getMaximum();
                int value = min + (int) Math.round((max - min) * (double) x / sw);
                slider.setValue(Math.max(min, Math.min(max, value)));
            }
        });
        slider.addChangeListener((ChangeListener) e -> {
            if (!slider.getValueIsAdjusting()) StartScene.playVolumeTestDingAt(slider.getValue() / 100f);
        });
        JCheckBox muteMusic = new JCheckBox("Mute all music", StartScene.getMuteAllMusic());
        JCheckBox muteSfx = new JCheckBox("Mute all sound effects", StartScene.getMuteAllSoundEffects());
        muteMusic.setForeground(new Color(245, 245, 240));
        muteMusic.setBackground(new Color(40, 40, 50));
        muteSfx.setForeground(new Color(245, 245, 240));
        muteSfx.setBackground(new Color(40, 40, 50));
        JPanel checkPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        checkPanel.setOpaque(false);
        checkPanel.add(muteMusic);
        checkPanel.add(muteSfx);
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(ev -> {
            StartScene.setMasterVolume(slider.getValue() / 100f);
            StartScene.setMuteAllMusic(muteMusic.isSelected());
            StartScene.setMuteAllSoundEffects(muteSfx.isSelected());
            StartScene.applyVolumeToClipForSceneNoFloor(postBattleMusicClip, true);
            d.dispose();
        });
        JLabel volLabel = new JLabel("Sound Volume");
        volLabel.setForeground(new Color(250, 250, 240));
        p.add(volLabel, BorderLayout.NORTH);
        p.add(slider, BorderLayout.CENTER);
        p.add(checkPanel, BorderLayout.SOUTH);
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(applyBtn);
        p.add(btnPanel, BorderLayout.PAGE_END);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void handleHistory() {
        StringBuilder sb = new StringBuilder();
        for (DialogueRecord r : GameState.getState().getChapterOneHistory()) {
            if (sb.length() > 0) sb.append("\n");
            String s = r.getSpeaker();
            if (s == null || s.isEmpty()) sb.append(r.getText());
            else sb.append(s).append(": ").append(r.getText());
        }
        for (DialogueRecord r : GameState.getState().getTrollCaveHistory()) {
            if (sb.length() > 0) sb.append("\n");
            String s = r.getSpeaker();
            if (s == null || s.isEmpty()) sb.append(r.getText());
            else sb.append(s).append(": ").append(r.getText());
        }
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "History", Dialog.ModalityType.APPLICATION_MODAL);
        d.getContentPane().setBackground(new Color(30, 28, 35));
        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setBackground(new Color(30, 28, 35));
        wrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(212, 175, 55), 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JTextArea area = new JTextArea(sb.length() > 0 ? sb.toString() : "No dialogue history yet.", 14, 42);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(new Color(240, 238, 230));
        area.setBackground(new Color(45, 42, 50));
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(area.getBackground());
        wrap.add(scroll, BorderLayout.CENTER);
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(212, 175, 55));
        closeBtn.setForeground(new Color(30, 28, 35));
        closeBtn.addActionListener(ev -> d.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        wrap.add(btnPanel, BorderLayout.PAGE_END);
        d.setContentPane(wrap);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    @Override
    public void onEnter() {
        GameState.getState().setCurrentScene("troll_cave_post_battle");
        lastHistoryBlock = -1;
        lastHistoryLine = -1;
        pauseMenuVisible = false;
        applyPostBattleProgressFromStoryState();
        lineStartTime = System.currentTimeMillis();
        persistPostBattleProgress();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (holdSpaceTimer != null) holdSpaceTimer.stop();
        if (fastForwardTimer != null) fastForwardTimer.stop();
        stopPostBattleBgm();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

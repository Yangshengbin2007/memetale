package game.scene;

import game.model.forest.ForestEntranceData;
import game.model.forest.ForestImageLoader;
import game.model.forest.ForestMapData;
import game.model.forest.ForestResources;
import game.model.DialogueRecord;
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
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forest entrance: Prince (left) and Darabongba (right) with a bottom dialogue box.
 * Active speaker is bright, the other is dimmed; mirror Darabongba for surprise/resentment.
 * After the entrance script comes map dialogue, landmark picking (hinted), then a black screen into the overworld map.
 */
public class ForestEntranceScene extends JPanel implements Scene {
    private static final int PHASE_ENTRANCE_DIALOGUE = 0;
    private static final int PHASE_MAP_DIALOGUE = 1;
    private static final int PHASE_MAP_CHOICE = 2;
    private static final int PHASE_BLACK_SCREEN = 3;

    private final Runnable onEnterForest;
    /** When set, called with chosenLandmarkId on Continue; else onEnterForest.run() */
    private java.util.function.Consumer<String> onLandmarkChosen;

    private Image bgEntrance;
    private Image bgMap;
    private final Map<String, Image> princeImages = new HashMap<>();
    private final Map<String, Image> darabongbaImages = new HashMap<>();

    private int phase = PHASE_ENTRANCE_DIALOGUE;
    private int entranceLineIndex = 0;
    private int mapLineIndex = 0;
    private String chosenLandmarkId = null;

    private static final float DIM_ALPHA = 0.45f;
    private static final int DIALOGUE_BOX_HEIGHT_RATIO = 22;
    /** Portrait height is 2/3 of the screen; knees line up with the dialogue top (~60% of sprite above the box). */
    private static final double CHARACTER_HEIGHT_RATIO = 2.0 / 3.0;
    /** Fraction of portrait height below the dialogue top; 0.6 lines knees up with the box top. */
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

    private boolean pauseMenuVisible = false;
    private boolean hoverPauseSave, hoverPauseLoad, hoverPauseSettings, hoverPauseHistory, hoverPauseQuit;
    private boolean pressedPauseSave, pressedPauseLoad, pressedPauseSettings, pressedPauseHistory, pressedPauseQuit;
    private final Rectangle pauseSaveBounds = new Rectangle();
    private final Rectangle pauseLoadBounds = new Rectangle();
    private final Rectangle pauseSettingsBounds = new Rectangle();
    private final Rectangle pauseHistoryBounds = new Rectangle();
    private final Rectangle pauseQuitBounds = new Rectangle();
    private final Runnable onQuitToTitle;
    /** After load, jump to chapter one (injected from Main). */
    private Runnable onLoadSwitchToChapterOne;

    /** Forest dialogue history (entrance + map) for the ESC History panel. */
    private final List<DialogueRecord> forestDialogueHistory = new ArrayList<>();
    private int lastForestLineRecorded = -1;

    private boolean hoverContinue = false;
    private final Rectangle continueBounds = new Rectangle();
    private int hoverLandmarkIndex = -1;
    private final Rectangle[] landmarkBounds = new Rectangle[ForestMapData.CHOICE_LANDMARK_IDS.length];

    /** Wrong landmark: show a knight-only line without a modal; click clears it. */
    private String wrongDestinationMessage = null;
    private Clip forestMusicClip;
    /** When choosing Troll Cave: quick fade instead of black screen; start time for fade overlay */
    private long trollCaveFadeStartTime = 0L;
    private static final int TROLL_CAVE_FADE_MS = 380;

    public ForestEntranceScene(Runnable onEnterForest) {
        this(onEnterForest, null);
    }

    public ForestEntranceScene(Runnable onEnterForest, Runnable onQuitToTitle) {
        this.onEnterForest = onEnterForest;
        this.onQuitToTitle = onQuitToTitle;
        setBackground(new Color(20, 40, 20));
        loadAllImages();
        for (int i = 0; i < landmarkBounds.length; i++) {
            landmarkBounds[i] = new Rectangle();
        }
        initSpaceTimers();
        initMouse();
        initKey();
    }

    public void setOnLoadSwitchToChapterOne(Runnable r) {
        onLoadSwitchToChapterOne = r;
    }

    public void setOnLandmarkChosen(java.util.function.Consumer<String> c) {
        onLandmarkChosen = c;
    }

    private void initSpaceTimers() {
        holdSpaceTimer = new javax.swing.Timer(SPACE_HOLD_MS, e -> {
            if (spaceKeyHeld && (phase == PHASE_ENTRANCE_DIALOGUE || phase == PHASE_MAP_DIALOGUE)) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning()) fastForwardTimer.start();
            }
            if (holdSpaceTimer != null) holdSpaceTimer.setRepeats(false);
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive) return;
            if (phase == PHASE_ENTRANCE_DIALOGUE && entranceLineIndex < ForestEntranceData.LINES.length - 1) {
                entranceLineIndex++;
                lineStartTime = System.currentTimeMillis();
            } else if (phase == PHASE_ENTRANCE_DIALOGUE) {
                phase = PHASE_MAP_DIALOGUE;
                mapLineIndex = 0;
                lineStartTime = System.currentTimeMillis();
                switchToMapMusic();
                fastForwardTimer.stop();
            } else if (phase == PHASE_MAP_DIALOGUE && mapLineIndex < ForestMapData.LINES.length - 1) {
                mapLineIndex++;
                lineStartTime = System.currentTimeMillis();
            } else if (phase == PHASE_MAP_DIALOGUE) {
                phase = PHASE_MAP_CHOICE;
                if (fastForwardTimer != null) fastForwardTimer.stop();
            }
            repaint();
        });
    }

    /** Stop beginforest and start map.wav when entering map dialogue. */
    private void switchToMapMusic() {
        if (forestMusicClip != null) {
            try { if (forestMusicClip.isRunning()) forestMusicClip.stop(); forestMusicClip.close(); } catch (Exception ignore) {}
            forestMusicClip = null;
        }
        forestMusicClip = StartScene.loadMusicFromMusicDir("map.wav");
        if (forestMusicClip == null) forestMusicClip = StartScene.loadMusicFromMusicDir("map.mp3");
        if (forestMusicClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(forestMusicClip, true);
            forestMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            forestMusicClip.start();
        }
    }

    private void initKey() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ev) {
                if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (phase == PHASE_ENTRANCE_DIALOGUE || phase == PHASE_MAP_DIALOGUE || phase == PHASE_MAP_CHOICE || phase == PHASE_BLACK_SCREEN) {
                        pauseMenuVisible = !pauseMenuVisible;
                        repaint();
                    }
                    return;
                }
                if (ev.getKeyCode() == KeyEvent.VK_SPACE && !pauseMenuVisible && (phase == PHASE_ENTRANCE_DIALOGUE || phase == PHASE_MAP_DIALOGUE)) {
                    ev.consume();
                    spaceKeyHeld = true;
                    performAdvanceOrReveal();
                    if (holdSpaceTimer != null && !holdSpaceTimer.isRunning()) {
                        holdSpaceTimer.setInitialDelay(SPACE_HOLD_MS);
                        holdSpaceTimer.start();
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent ev) {
                if (ev.getKeyCode() == KeyEvent.VK_SPACE) {
                    ev.consume();
                    spaceKeyHeld = false;
                    fastForwardActive = false;
                    if (fastForwardTimer != null && fastForwardTimer.isRunning()) fastForwardTimer.stop();
                }
            }
        });
    }

    private void performAdvanceOrReveal() {
        String[][] lines = (phase == PHASE_ENTRANCE_DIALOGUE) ? ForestEntranceData.LINES : ForestMapData.LINES;
        int idx = phase == PHASE_ENTRANCE_DIALOGUE ? entranceLineIndex : mapLineIndex;
        if (idx >= lines.length) return;
        String text = lines[idx][1];
        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars) {
            lineStartTime = System.currentTimeMillis() - TEXT_DURATION_MS;
            repaint();
        } else {
            advanceDialogue();
        }
    }

    private void advanceDialogue() {
        recordCurrentLineToHistory();
        if (phase == PHASE_ENTRANCE_DIALOGUE) {
            if (entranceLineIndex < ForestEntranceData.LINES.length - 1) {
                entranceLineIndex++;
                lineStartTime = System.currentTimeMillis();
            } else {
                phase = PHASE_MAP_DIALOGUE;
                mapLineIndex = 0;
                lineStartTime = System.currentTimeMillis();
                switchToMapMusic();
            }
        } else if (phase == PHASE_MAP_DIALOGUE) {
            if (mapLineIndex < ForestMapData.LINES.length - 1) {
                mapLineIndex++;
                lineStartTime = System.currentTimeMillis();
            } else {
                phase = PHASE_MAP_CHOICE;
            }
        }
        repaint();
    }

    /** Append the current line to History once the typewriter finishes; advance() also records if needed. */
    private void recordCurrentLineToHistory() {
        String[][] lines = (phase == PHASE_ENTRANCE_DIALOGUE) ? ForestEntranceData.LINES : (phase == PHASE_MAP_DIALOGUE ? ForestMapData.LINES : null);
        int idx = phase == PHASE_ENTRANCE_DIALOGUE ? entranceLineIndex : (phase == PHASE_MAP_DIALOGUE ? mapLineIndex : -1);
        if (lines == null || idx < 0 || idx >= lines.length) return;
        int combined = phase == PHASE_ENTRANCE_DIALOGUE ? idx : ForestEntranceData.LINES.length + idx;
        if (lastForestLineRecorded == combined) return;
        String[] line = lines[idx];
        forestDialogueHistory.add(new DialogueRecord(line[0], line[1]));
        lastForestLineRecorded = combined;
    }

    private void loadAllImages() {
        bgEntrance = ForestImageLoader.loadBackground(ForestResources.BG_ENTRANCE);
        bgMap = ForestImageLoader.loadBackground(ForestResources.BG_MAP);
        loadPrince(ForestResources.PRINCE_DEFAULT);
        loadPrince(ForestResources.PRINCE_SURPRISE);
        loadPrince(ForestResources.PRINCE_ANNOYED);
        loadPrince(ForestResources.PRINCE_THOUGHTFUL);
        loadDarabongba(ForestResources.DARABONGBA_DEFAULT);
        loadDarabongba(ForestResources.DARABONGBA_SURPRISE);
        loadDarabongba(ForestResources.DARABONGBA_RESENTMENT);
        loadDarabongba(ForestResources.DARABONGBA_NERVOUS);
        loadDarabongba(ForestResources.DARABONGBA_NERVOUS); // proud fallback to nervous if missing
        if (!darabongbaImages.containsKey("proud")) {
            Image n = darabongbaImages.get(ForestResources.DARABONGBA_NERVOUS);
            if (n != null) darabongbaImages.put("proud", n);
        }
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

    private Image getPrinceImage(String expr) {
        if (expr == null) expr = ForestEntranceData.EXPR_DEFAULT;
        String key = expr.startsWith("prince_") ? expr.substring(7) : expr;
        if (!princeImages.containsKey(key)) {
            loadPrince("prince_" + key);
        }
        Image img = princeImages.get(key);
        return img != null ? img : princeImages.get("default");
    }

    private Image getDarabongbaImage(String expr) {
        if (expr == null) expr = ForestEntranceData.EXPR_DEFAULT;
        String key = expr.startsWith("darabongba_") ? expr.substring(11) : expr;
        if (!darabongbaImages.containsKey(key)) {
            loadDarabongba("darabongba_" + key);
        }
        Image img = darabongbaImages.get(key);
        return img != null ? img : darabongbaImages.get("default");
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
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    if (pressedPauseSave && pauseSaveBounds.contains(p)) {
                        handleSave();
                    } else if (pressedPauseLoad && pauseLoadBounds.contains(p)) {
                        handleLoad();
                    } else if (pressedPauseSettings && pauseSettingsBounds.contains(p)) {
                        handleSettings();
                    } else if (pressedPauseHistory && pauseHistoryBounds.contains(p)) {
                        handleHistory();
                    } else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                        int choice = JOptionPane.showConfirmDialog(ForestEntranceScene.this,
                            "Are you sure you want to quit?",
                            "Quit to Title",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                        if (choice == JOptionPane.YES_OPTION && onQuitToTitle != null) {
                            pauseMenuVisible = false;
                            onQuitToTitle.run();
                        }
                    } else if (!pauseSaveBounds.contains(p) && !pauseLoadBounds.contains(p) && !pauseSettingsBounds.contains(p) && !pauseHistoryBounds.contains(p) && !pauseQuitBounds.contains(p)) {
                        pauseMenuVisible = false;
                    }
                    pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                    repaint();
                    return;
                }
                if (phase == PHASE_ENTRANCE_DIALOGUE || phase == PHASE_MAP_DIALOGUE) {
                    performAdvanceOrReveal();
                    return;
                }
                if (phase == PHASE_MAP_CHOICE) {
                    if (wrongDestinationMessage != null) {
                        wrongDestinationMessage = null;
                        repaint();
                        return;
                    }
                    for (int i = 0; i < landmarkBounds.length; i++) {
                        if (landmarkBounds[i].contains(p)) {
                            String id = ForestMapData.CHOICE_LANDMARK_IDS[i];
                            if (!ForestMapData.FIRST_DESTINATION_LANDMARK_ID.equals(id)) {
                                wrongDestinationMessage = "This isn't our destination. The map says Troll Cave. I know reading is hard.";
                                repaint();
                                return;
                            }
                            chosenLandmarkId = id;
                            phase = PHASE_BLACK_SCREEN;
                            if ("troll_cave".equals(id)) {
                                trollCaveFadeStartTime = System.currentTimeMillis();
                                javax.swing.Timer t = new javax.swing.Timer(TROLL_CAVE_FADE_MS, ev -> {
                                    ((javax.swing.Timer)ev.getSource()).stop();
                                    if (onLandmarkChosen != null && "troll_cave".equals(chosenLandmarkId))
                                        onLandmarkChosen.accept(chosenLandmarkId);
                                });
                                t.setRepeats(false);
                                t.start();
                            }
                            repaint();
                            return;
                        }
                    }
                    return;
                }
                if (phase == PHASE_BLACK_SCREEN && continueBounds.contains(p)) {
                    if (onLandmarkChosen != null && chosenLandmarkId != null) {
                        onLandmarkChosen.accept(chosenLandmarkId);
                    } else if (onEnterForest != null) {
                        onEnterForest.run();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    hoverPauseSave = pauseSaveBounds.contains(p);
                    hoverPauseLoad = pauseLoadBounds.contains(p);
                    hoverPauseSettings = pauseSettingsBounds.contains(p);
                    hoverPauseHistory = pauseHistoryBounds.contains(p);
                    hoverPauseQuit = pauseQuitBounds.contains(p);
                } else {
                    hoverContinue = phase == PHASE_BLACK_SCREEN && continueBounds.contains(p);
                    hoverLandmarkIndex = -1;
                    if (phase == PHASE_MAP_CHOICE) {
                        for (int i = 0; i < landmarkBounds.length; i++) {
                            if (landmarkBounds[i].contains(p)) {
                                hoverLandmarkIndex = i;
                                break;
                            }
                        }
                    }
                }
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (phase == PHASE_BLACK_SCREEN) {
            if ("troll_cave".equals(chosenLandmarkId)) {
                if (bgMap != null) {
                    int iw = bgMap.getWidth(this);
                    int ih = bgMap.getHeight(this);
                    if (iw > 0 && ih > 0) {
                        double scale = Math.max((double) w / iw, (double) h / ih);
                        int sw = (int) Math.ceil(iw * scale);
                        int sh = (int) Math.ceil(ih * scale);
                        int x = (w - sw) / 2;
                        int y = (h - sh) / 2;
                        g2.drawImage(bgMap, x, y, sw, sh, this);
                    }
                } else {
                    g2.setColor(new Color(25, 50, 25));
                    g2.fillRect(0, 0, w, h);
                }
                long elapsed = System.currentTimeMillis() - trollCaveFadeStartTime;
                float alpha = Math.min(1f, (float) elapsed / TROLL_CAVE_FADE_MS);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                if (alpha < 1f) repaint(30);
                g2.dispose();
                return;
            }
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            int btnW = 200;
            int btnH = 44;
            int bx = (w - btnW) / 2;
            int by = h - btnH - 80;
            continueBounds.setBounds(bx, by, btnW, btnH);
            g2.setColor(hoverContinue ? new Color(60, 100, 60) : new Color(40, 80, 40));
            g2.fillRoundRect(bx, by, btnW, btnH, 12, 12);
            g2.setColor(new Color(200, 220, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, btnW, btnH, 12, 12);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
            FontMetrics fm = g2.getFontMetrics();
            String text = "Continue";
            g2.drawString(text, bx + (btnW - fm.stringWidth(text)) / 2, by + (btnH + fm.getAscent()) / 2 - 2);
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            fm = g2.getFontMetrics();
            String sub = "Destination: " + (chosenLandmarkId != null ? chosenLandmarkId : "—") + " (scene not implemented yet)";
            g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, by + btnH + 24);
            g2.dispose();
            return;
        }

        boolean useMapBg = (phase == PHASE_MAP_DIALOGUE || phase == PHASE_MAP_CHOICE);
        Image bg = useMapBg ? bgMap : bgEntrance;
        if (bg != null) {
            int iw = bg.getWidth(this);
            int ih = bg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(bg, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(useMapBg ? new Color(25, 50, 25) : new Color(30, 60, 30));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(140, 160, 140));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
            FontMetrics fm = g2.getFontMetrics();
            String msg = useMapBg ? "Map (map1forest.jpg)" : "Forest entrance (forest1.jpg)";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 - 10);
        }

        if (phase == PHASE_MAP_CHOICE) {
            drawMapChoiceOverlay(g2, w, h);
            if (wrongDestinationMessage != null) {
                drawWrongDestinationDialogue(g2, w, h);
            }
            g2.dispose();
            return;
        }

        String[][] lines = (phase == PHASE_ENTRANCE_DIALOGUE) ? ForestEntranceData.LINES : ForestMapData.LINES;
        int lineIndex = phase == PHASE_ENTRANCE_DIALOGUE ? entranceLineIndex : mapLineIndex;
        if (lineIndex >= lines.length) {
            g2.dispose();
            return;
        }

        String[] line = lines[lineIndex];
        String speaker = line[0];
        String text = line[1];
        String princeExpr = line[2];
        String darabongbaExpr = line[3];

        boolean princeSpeaking = "Prince".equals(speaker);
        boolean darabongbaSpeaking = "Darabongba".equals(speaker);
        boolean onlyDarabongbaSpeaks = darabongbaSpeaking && !princeSpeaking && !speaker.isEmpty();

        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars)
            repaint(TEXT_ANIM_DELAY_MS);
        else
            recordCurrentLineToHistory();

        int boxH = h * DIALOGUE_BOX_HEIGHT_RATIO / 100;
        int boxY = h - boxH;
        int charH = (int) (h * CHARACTER_HEIGHT_RATIO);
        int charY = boxY - (int) (charH * CHARACTER_KNEE_ALIGN_RATIO);

        int charW = (int) (w * 0.38);
        int leftX = (int) (w * 0.06);
        int rightX = (int) (w * 0.52);

        if (!onlyDarabongbaSpeaks) {
            Image princeImg = getPrinceImage(princeExpr);
            if (princeImg != null) {
                float alpha = princeSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, princeImg, leftX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        } else {
            Image princeImg = getPrinceImage(princeExpr);
            if (princeImg != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DIM_ALPHA));
                drawCharacter(g2, princeImg, leftX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }
        Image daraImg = getDarabongbaImage(darabongbaExpr);
        if (daraImg != null) {
            float alpha = darabongbaSpeaking ? 1f : DIM_ALPHA;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            drawCharacter(g2, daraImg, rightX, charY, charW, charH, false);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
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
        String nameLabel = speaker.isEmpty() ? "" : speaker;
        if (!nameLabel.isEmpty()) {
            g2.drawString(nameLabel, pad, y + g2.getFontMetrics().getAscent());
        }
        y += 26;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.setColor(new Color(240, 240, 235));
        String visibleText = text.substring(0, Math.min(visibleChars, text.length()));
        drawWrappedText(g2, visibleText, pad, y, w - pad * 2, 22);

        g2.setColor(new Color(120, 120, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click / Space | Hold Space to skip | ESC menu", w - 340, h - 12);

        if (pauseMenuVisible)
            paintPauseMenu(g2, w, h);
        g2.dispose();
    }

    private void paintPauseMenu(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        int panelW = (int) (w * 0.5);
        int panelH = (int) (h * 0.55);
        int px = (w - panelW) / 2;
        int py = (h - panelH) / 2;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(40, 40, 40, 230));
        g2.fillRoundRect(px, py, panelW, panelH, 18, 18);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(px + 1, py + 1, panelW - 2, panelH - 2, 18, 18);
        int btnW = (int) (panelW * 0.7);
        int btnH = (int) (panelH * 0.12);
        int spacing = (panelH - btnH * 5) / 6;
        int x = w / 2 - btnW / 2;
        int y = py + spacing;
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
        int cw = (int) (r.width * scale);
        int ch = (int) (r.height * scale);
        int cx = r.x + (r.width - cw) / 2;
        int cy = r.y + (r.height - ch) / 2;
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
        state.setCurrentScene("forest_entrance");
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
                    JOptionPane.showMessageDialog(ForestEntranceScene.this, "Saved to slot " + slot);
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ForestEntranceScene.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                        JOptionPane.showMessageDialog(ForestEntranceScene.this, "Empty slot " + slot);
                        return;
                    }
                    int savedCh = loaded.getSavedChapter();
                    if (savedCh == 2 || savedCh == 3) {
                        JOptionPane.showMessageDialog(ForestEntranceScene.this, "Chapter " + savedCh + " is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    GameState.setState(loaded);
                    dialog.dispose();
                    pauseMenuVisible = false;
                    String sceneKey = loaded.getCurrentScene();
                    if ("troll_cave".equals(sceneKey)) {
                        if (onLandmarkChosen != null) onLandmarkChosen.accept("troll_cave");
                        else if (onEnterForest != null) onEnterForest.run();
                    } else if ("forest_overworld_map".equals(sceneKey)) {
                        if (onEnterForest != null) onEnterForest.run();
                    } else if ("forest_entrance".equals(sceneKey)) {
                        phase = PHASE_ENTRANCE_DIALOGUE;
                        entranceLineIndex = 0;
                        mapLineIndex = 0;
                        lineStartTime = System.currentTimeMillis();
                        repaint();
                    } else {
                        if (onLoadSwitchToChapterOne != null) onLoadSwitchToChapterOne.run();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ForestEntranceScene.this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        slider.setForeground(new Color(240, 240, 240));
        slider.setBackground(new Color(60, 60, 60));
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!slider.isEnabled()) return;
                int w = slider.getWidth();
                int x = e.getX();
                int min = slider.getMinimum();
                int max = slider.getMaximum();
                int value = min + (int) Math.round((max - min) * (double) x / w);
                value = Math.max(min, Math.min(max, value));
                slider.setValue(value);
            }
        });
        slider.addChangeListener((ChangeListener) e -> {
            if (!slider.getValueIsAdjusting())
                StartScene.playVolumeTestDingAt(slider.getValue() / 100f);
        });
        JCheckBox muteMusicCheck = new JCheckBox("Mute all music", StartScene.getMuteAllMusic());
        muteMusicCheck.setForeground(new Color(245, 245, 240));
        muteMusicCheck.setBackground(new Color(40, 40, 50));
        JCheckBox muteSfxCheck = new JCheckBox("Mute all sound effects", StartScene.getMuteAllSoundEffects());
        muteSfxCheck.setForeground(new Color(245, 245, 240));
        muteSfxCheck.setBackground(new Color(40, 40, 50));
        JPanel checkPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        checkPanel.setOpaque(false);
        checkPanel.add(muteMusicCheck);
        checkPanel.add(muteSfxCheck);
        JButton applyBtn = new JButton("Apply");
        applyBtn.setBackground(new Color(30, 144, 255));
        applyBtn.setForeground(Color.BLACK);
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(ev -> {
            StartScene.setMasterVolume(slider.getValue() / 100f);
            StartScene.setMuteAllMusic(muteMusicCheck.isSelected());
            StartScene.setMuteAllSoundEffects(muteSfxCheck.isSelected());
            StartScene.applyVolumeToClipForSceneNoFloor(forestMusicClip, true);
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
        for (DialogueRecord r : forestDialogueHistory) {
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
        area.setFont(area.getFont().deriveFont(14f));
        area.setForeground(new Color(240, 238, 230));
        area.setBackground(new Color(45, 42, 50));
        area.setCaretColor(area.getForeground());
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(area.getBackground());
        wrap.add(scroll, BorderLayout.CENTER);
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(212, 175, 55));
        closeBtn.setForeground(new Color(30, 28, 35));
        closeBtn.setFocusPainted(false);
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

    /** Draw inside maxW x maxH with feet at the bottom of the slot (aligned to the dialogue top). */
    private void drawCharacter(Graphics2D g2, Image img, int x, int y, int maxW, int maxH, boolean mirror) {
        int iw = img.getWidth(this);
        int ih = img.getHeight(this);
        if (iw <= 0 || ih <= 0) return;
        double scale = Math.min((double) maxW / iw, (double) maxH / ih);
        int sw = (int) (iw * scale);
        int sh = (int) (ih * scale);
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
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, currentY + fm.getAscent());
        }
    }

    private void drawMapChoiceOverlay(Graphics2D g2, int w, int h) {
        double sx = w / 800.0;
        double sy = h / 600.0;
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, w, 70);
        g2.setColor(new Color(255, 255, 220));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
        FontMetrics fm = g2.getFontMetrics();
        String hint = "Our first stop is Troll Cave. Click it.";
        g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, 42);

        for (int i = 0; i < ForestMapData.CHOICE_BOUNDS_800x600.length; i++) {
            int[] b = ForestMapData.CHOICE_BOUNDS_800x600[i];
            int rx = (int) (b[0] * sx);
            int ry = (int) (b[1] * sy);
            int rw = (int) (b[2] * sx);
            int rh = (int) (b[3] * sy);
            landmarkBounds[i].setBounds(rx, ry, rw, rh);
            boolean hover = (hoverLandmarkIndex == i);
            g2.setColor(hover ? new Color(255, 220, 100, 200) : new Color(255, 255, 200, 140));
            g2.fillRoundRect(rx, ry, rw, rh, 10, 10);
            g2.setColor(hover ? new Color(220, 180, 40) : new Color(180, 160, 60));
            g2.setStroke(new BasicStroke(hover ? 3f : 2f));
            g2.drawRoundRect(rx, ry, rw, rh, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            fm = g2.getFontMetrics();
            String name = ForestMapData.CHOICE_LANDMARK_NAMES[i];
            if (fm.stringWidth(name) > rw - 6) {
                while (name.length() > 1 && fm.stringWidth(name + "…") > rw - 6) name = name.substring(0, name.length() - 1);
                name = name + "…";
            }
            g2.drawString(name, rx + (rw - fm.stringWidth(name)) / 2, ry + (rh + fm.getAscent()) / 2 - 2);
        }
    }

    @Override
    public void onEnter() {
        GameState.getState().setCurrentScene("forest_entrance");
        lineStartTime = System.currentTimeMillis();
        if (forestMusicClip != null) {
            try { if (forestMusicClip.isRunning()) forestMusicClip.stop(); forestMusicClip.close(); } catch (Exception ignore) {}
            forestMusicClip = null;
        }
        forestMusicClip = StartScene.loadMusicFromMusicDir("beginforest.wav");
            if (forestMusicClip == null) forestMusicClip = StartScene.loadMusicFromMusicDir("beginforest.mp3");
        if (forestMusicClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(forestMusicClip, true);
            forestMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            forestMusicClip.start();
        }
        requestFocusInWindow();
    }

    /** Wrong landmark on the map picker: knight sprite + bottom box; click anywhere to dismiss. */
    private void drawWrongDestinationDialogue(Graphics2D g2, int w, int h) {
        int boxH = h * DIALOGUE_BOX_HEIGHT_RATIO / 100;
        int boxY = h - boxH;
        int charH = (int) (h * CHARACTER_HEIGHT_RATIO);
        int charY = boxY - (int) (charH * CHARACTER_KNEE_ALIGN_RATIO);
        int rightX = (int) (w * 0.52);
        int charW = (int) (w * 0.38);
        Image daraImg = getDarabongbaImage(ForestEntranceData.EXPR_RESENTMENT);
        if (daraImg != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            drawCharacter(g2, daraImg, rightX, charY, charW, charH, false);
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
        drawWrappedText(g2, wrongDestinationMessage, pad, y, w - pad * 2, 22);
        g2.setColor(new Color(120, 120, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click to close", w - 100, h - 12);
    }

    @Override
    public void onExit() {
        if (forestMusicClip != null) {
            try { if (forestMusicClip.isRunning()) forestMusicClip.stop(); forestMusicClip.close(); } catch (Exception ignore) {}
            forestMusicClip = null;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

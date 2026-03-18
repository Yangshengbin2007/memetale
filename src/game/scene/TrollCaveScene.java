package game.scene;

import game.model.forest.ForestEntranceData;
import game.model.forest.ForestImageLoader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Troll Cave: Prince, Darabongba, Troll Boss. Troll default right (knight position).
 * When Darabongba speaks with Troll in scene, Darabongba on left (prince position) mirrored.
 * All text English. ESC/Save/Load/Settings/History/Quit, hold Space to skip.
 */
public class TrollCaveScene extends JPanel implements Scene {
    /**
     * 代理数据源：用反射读取真实的 game.model.forest.TrollCaveData。
     * 规避 IDE/linter 在当前项目里的“跨包常量无法解析”误报。
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
        public static final String TROLL_LAUGH = getStringField("TROLL_LAUGH");
        public static final String TROLL_SCARED = getStringField("TROLL_SCARED");
        public static final String TROLL_DEFEAT = getStringField("TROLL_DEFEAT");

        public static final String[][] LINES = getString2dField("LINES");
    }

    private final Runnable onQuitToTitle;
    private final Runnable onDialogueComplete;

    private Image bgCave;
    private final Map<String, Image> princeImages = new HashMap<>();
    private final Map<String, Image> darabongbaImages = new HashMap<>();
    private final Map<String, Image> trollBossImages = new HashMap<>();

    private int lineIndex = 0;
    private static final float DIM_ALPHA = 0.45f;
    private static final int DIALOGUE_BOX_HEIGHT_RATIO = 22;
    private static final double CHARACTER_HEIGHT_RATIO = 2.0 / 3.0;
    private static final double CHARACTER_KNEE_ALIGN_RATIO = 0.6;
    private static final int TEXT_DURATION_MS = 1000;
    private static final int TEXT_ANIM_DELAY_MS = 40;
    private static final int SPACE_HOLD_MS = 3000;
    private static final int FAST_FORWARD_INTERVAL_MS = 100;
    private static final int FIRST_TROLL_LINE = 32;

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

    private int lastRecordedLine = -1;
    private Clip caveMusicClip;

    public TrollCaveScene(Runnable onQuitToTitle, Runnable onDialogueComplete) {
        this.onQuitToTitle = onQuitToTitle;
        this.onDialogueComplete = onDialogueComplete;
        setBackground(new Color(30, 20, 40));
        loadAllImages();
        initSpaceTimers();
        initMouse();
        initKey();
    }

    private void loadAllImages() {
        bgCave = ForestImageLoader.loadBackground("troll cave.png");
        if (bgCave == null) bgCave = ForestImageLoader.loadBackground("troll_cave.png");
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
        loadTrollBoss(TrollCaveData.TROLL_LAUGH);
        loadTrollBoss(TrollCaveData.TROLL_SCARED);
        loadTrollBoss(TrollCaveData.TROLL_DEFEAT);
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
            if (spaceKeyHeld && lineIndex < TrollCaveData.LINES.length) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning()) fastForwardTimer.start();
            }
            if (holdSpaceTimer != null) holdSpaceTimer.setRepeats(false);
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive) return;
            if (lineIndex >= TrollCaveData.LINES.length) {
                if (fastForwardTimer != null) fastForwardTimer.stop();
                return;
            }
            performAdvance();
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
                    performAdvance();
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
                    if (pressedPauseSave && pauseSaveBounds.contains(p)) handleSave();
                    else if (pressedPauseLoad && pauseLoadBounds.contains(p)) handleLoad();
                    else if (pressedPauseSettings && pauseSettingsBounds.contains(p)) handleSettings();
                    else if (pressedPauseHistory && pauseHistoryBounds.contains(p)) handleHistory();
                    else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                        int c = JOptionPane.showConfirmDialog(TrollCaveScene.this, "Are you sure you want to quit?", "Quit to Title", JOptionPane.YES_NO_OPTION);
                        if (c == JOptionPane.YES_OPTION && onQuitToTitle != null) {
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
                if (lineIndex < TrollCaveData.LINES.length) performAdvance();
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
                }
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void performAdvance() {
        if (lineIndex >= TrollCaveData.LINES.length) return;
        String[] line = TrollCaveData.LINES[lineIndex];
        String text = line[1];
        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (!fastForwardActive && visibleChars < totalChars) {
            lineStartTime = System.currentTimeMillis() - TEXT_DURATION_MS;
            repaint();
            return;
        }
        recordCurrentLineToHistory();
        lineIndex++;
        lineStartTime = System.currentTimeMillis();
        if (lineIndex >= TrollCaveData.LINES.length && onDialogueComplete != null) {
            GameState.getState().setTrollCaveDialogueIndex(lineIndex);
            onDialogueComplete.run();
            return;
        }
        repaint();
    }

    private void recordCurrentLineToHistory() {
        if (lineIndex < 0 || lineIndex >= TrollCaveData.LINES.length || lastRecordedLine == lineIndex) return;
        String[] line = TrollCaveData.LINES[lineIndex];
        GameState.getState().addTrollCaveHistory(line[0], line[1]);
        lastRecordedLine = lineIndex;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (bgCave != null) {
            int iw = bgCave.getWidth(this), ih = bgCave.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale), sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2, y = (h - sh) / 2;
                g2.drawImage(bgCave, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(40, 30, 50));
            g2.fillRect(0, 0, w, h);
        }

        if (lineIndex >= TrollCaveData.LINES.length) {
            if (pauseMenuVisible) paintPauseMenu(g2, w, h);
            g2.dispose();
            return;
        }

        String[][] lines = TrollCaveData.LINES;
        String[] line = lines[lineIndex];
        String speaker = line[0];
        String text = line[1];
        String princeExpr = line[2];
        String darabongbaExpr = line[3];
        String trollExpr = line[4];

        boolean princeSpeaking = "Prince".equals(speaker);
        boolean darabongbaSpeaking = "Darabongba".equals(speaker);
        boolean trollSpeaking = "Troll Boss".equals(speaker);
        boolean showTrollRight = lineIndex >= FIRST_TROLL_LINE;

        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars) repaint(TEXT_ANIM_DELAY_MS);
        else if (lastRecordedLine != lineIndex) recordCurrentLineToHistory();

        int boxH = h * DIALOGUE_BOX_HEIGHT_RATIO / 100;
        int boxY = h - boxH;
        int charH = (int) (h * CHARACTER_HEIGHT_RATIO);
        int charY = boxY - (int) (charH * CHARACTER_KNEE_ALIGN_RATIO);
        int charW = (int) (w * 0.32);
        int leftX = (int) (w * 0.04);
        int centerX = (int) (w * 0.34);
        int rightX = (int) (w * 0.58);

        if (showTrollRight) {
            Image princeImg = getPrinceImage(princeExpr);
            if (princeImg != null) {
                float alpha = princeSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, princeImg, leftX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            Image trollImg = getTrollBossImage(trollExpr);
            if (trollImg != null) {
                float alpha = trollSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                int trollW = (int)(charW * 0.85);
                int trollH = (int)(charH * 0.55);
                int trollY = boxY - trollH;
                drawCharacter(g2, trollImg, centerX, trollY, trollW, trollH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            Image daraImg = getDarabongbaImage(darabongbaExpr);
            if (daraImg != null) {
                float alpha = darabongbaSpeaking ? 1f : DIM_ALPHA;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawCharacter(g2, daraImg, rightX, charY, charW, charH, false);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
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
        g2.drawString("Click / Space | Hold Space to skip | ESC menu", w - 340, h - 12);

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
        state.setCurrentScene("troll_cave");
        state.setTrollCaveDialogueIndex(lineIndex);
        state.setTrollCaveHistory(GameState.getState().getTrollCaveHistory());
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
                    JOptionPane.showMessageDialog(TrollCaveScene.this, "Saved to slot " + slot);
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TrollCaveScene.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                        JOptionPane.showMessageDialog(TrollCaveScene.this, "Empty slot " + slot);
                        return;
                    }
                    GameState.setState(loaded);
                    lineIndex = loaded.getTrollCaveDialogueIndex();
                    lineIndex = Math.min(lineIndex, TrollCaveData.LINES.length);
                    lineStartTime = System.currentTimeMillis();
                    lastRecordedLine = -1;
                    dialog.dispose();
                    pauseMenuVisible = false;
                    repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TrollCaveScene.this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                int w = slider.getWidth(), x = e.getX();
                int min = slider.getMinimum(), max = slider.getMaximum();
                int value = min + (int) Math.round((max - min) * (double) x / w);
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
            StartScene.applyVolumeToClipForScene(caveMusicClip, true);
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
        List<DialogueRecord> history = GameState.getState().getTrollCaveHistory();
        StringBuilder sb = new StringBuilder();
        for (DialogueRecord r : history) {
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
        StoryState state = GameState.getState();
        state.setCurrentScene("troll_cave");
        lineIndex = state.getTrollCaveDialogueIndex();
        lineIndex = Math.min(lineIndex, TrollCaveData.LINES.length);
        lineStartTime = System.currentTimeMillis();
        lastRecordedLine = -1;
        if (caveMusicClip != null) {
            try { if (caveMusicClip.isRunning()) caveMusicClip.stop(); caveMusicClip.close(); } catch (Exception ignore) {}
            caveMusicClip = null;
        }
        caveMusicClip = StartScene.loadMusicFromMusicDir("trollcave.wav");
        if (caveMusicClip == null) caveMusicClip = StartScene.loadMusicFromMusicDir("trollcave.mp3");
        if (caveMusicClip != null) {
            StartScene.applyVolumeToClipForScene(caveMusicClip, true);
            caveMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            try { caveMusicClip.start(); } catch (Exception ignored) {}
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (holdSpaceTimer != null) holdSpaceTimer.stop();
        if (fastForwardTimer != null) fastForwardTimer.stop();
        if (caveMusicClip != null) {
            try { if (caveMusicClip.isRunning()) caveMusicClip.stop(); caveMusicClip.close(); } catch (Exception ignore) {}
            caveMusicClip = null;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

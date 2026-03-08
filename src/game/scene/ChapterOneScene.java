package game.scene;

import game.model.*;
import game.io.SaveLoad;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

/**
 * 游戏第一章场景。底部对话点击推进，CG 随剧情切换，结尾淡入淡出 "Chapter 1 Meme Forest"。
 * 存档含位置与 History，读档从同一位置继续。ESC 暂停：存档/读档/设置/历史/返回主菜单。
 */
public class ChapterOneScene extends JPanel implements Scene {
    private final Runnable onQuitToTitle;
    private Image chapterBg;
    /** 当前显示的 CG 编号 1/2/3，用于避免重复 load */
    private int currentCgIndex = 0;

    private boolean pauseMenuVisible = false;
    private boolean hoverPauseSave, hoverPauseLoad, hoverPauseSettings, hoverPauseHistory, hoverPauseQuit;
    private boolean pressedPauseSave, pressedPauseLoad, pressedPauseSettings, pressedPauseHistory, pressedPauseQuit;
    private final Rectangle pauseSaveBounds = new Rectangle();
    private final Rectangle pauseLoadBounds = new Rectangle();
    private final Rectangle pauseSettingsBounds = new Rectangle();
    private final Rectangle pauseHistoryBounds = new Rectangle();
    private final Rectangle pauseQuitBounds = new Rectangle();

    /** 当前使用的存档槽位（与 StoryState 同步，存/读同一槽） */
    private int lastUsedSaveSlot = 1;

    /** 章节标题 "Chapter 1 Meme Forest" 淡入淡出：0=未开始 1=淡入 2=停留 3=淡出 4=结束 */
    private int chapterTitlePhase = 0;
    private long chapterTitleStartTime;
    private static final int TITLE_FADE_MS = 1000;
    private static final int TITLE_HOLD_MS = 1500;
    private float chapterTitleAlpha = 0f;
    private javax.swing.Timer titleTimer;

    /** 进入场景：先黑屏+引言，再淡入显示背景。*/
    private float enterFadeAlpha = 1f;
    private float quoteTextAlpha = 0f;
    private long enterFadeStartTime;
    private static final int QUOTE_FADE_IN_MS = 500;
    private static final int QUOTE_HOLD_MS = 3000;
    private static final int QUOTE_FADEOUT_MS = 500;
    /** 引言结束后黑屏淡出显示背景的时长（调大=更慢） */
    private static final int ENTER_FADE_MS = 950;
    private static final int ENTER_FADE_TIMER_DELAY = 40;
    private static final String QUOTE_TEXT = "When you can make a choice, don't let yourself regret it.";
    private javax.swing.Timer enterFadeTimer;

    /** 当前行开始显示的时间（用于 5 秒打字机动画）；点击未完成时立刻显现 */
    private long lineStartTime = 0L;
    private static final int TEXT_DURATION_MS = 3000;
    /** 打字机动画 repaint 驱动 */
    private static final int TEXT_ANIM_DELAY_MS = 40;

    /** CG+文本框 淡入：0→1，进入或切换 CG 时用 */
    private float sceneFadeAlpha = 0f;
    private long sceneFadeStartTime = 0L;
    private static final int SCENE_FADE_MS = 400;
    private javax.swing.Timer sceneFadeTimer;

    /** 空格：按下=点击推进；长按 3 秒=快速过对话，松开取消 */
    private boolean spaceKeyHeld = false;
    private boolean fastForwardActive = false;
    private static final int SPACE_HOLD_MS = 3000;
    private static final int FAST_FORWARD_INTERVAL_MS = 100;
    private javax.swing.Timer holdSpaceTimer;
    private javax.swing.Timer fastForwardTimer;

    /** 下次 onEnter 时跳过开场白（主菜单 Continue 读档后设为 true） */
    private static boolean skipQuoteNextEnter = false;
    public static void setSkipQuoteNextEnter(boolean skip) { skipQuoteNextEnter = skip; }

    /** 第一章字幕播完后黑屏，预留点击交互地图（之后实现） */
    private boolean chapterEndBlackScreen = false;

    public ChapterOneScene(Runnable onQuitToTitle) {
        this.onQuitToTitle = onQuitToTitle;
        setBackground(Color.BLACK);
        loadBackgroundForDialogueIndex(0);
        initEnterFadeTimer();
        initTitleTimer();
        initSceneFadeTimer();
        initSpaceTimers();
        initKey();
        initMouse();
    }

    private void initSceneFadeTimer() {
        sceneFadeTimer = new javax.swing.Timer(40, e -> {
            long elapsed = System.currentTimeMillis() - sceneFadeStartTime;
            sceneFadeAlpha = Math.min(1f, elapsed / (float) SCENE_FADE_MS);
            if (sceneFadeAlpha >= 1f && sceneFadeTimer != null)
                sceneFadeTimer.stop();
            repaint();
        });
    }

    private void startSceneFade() {
        sceneFadeAlpha = 0f;
        sceneFadeStartTime = System.currentTimeMillis();
        if (sceneFadeTimer != null && !sceneFadeTimer.isRunning())
            sceneFadeTimer.start();
    }

    private void initSpaceTimers() {
        holdSpaceTimer = new javax.swing.Timer(SPACE_HOLD_MS, e -> {
            if (spaceKeyHeld) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning())
                    fastForwardTimer.start();
            }
            if (holdSpaceTimer != null) holdSpaceTimer.setRepeats(false);
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive) return;
            int idx = GameState.getState().getChapterOneDialogueIndex();
            if (idx >= ChapterOneData.LINES.length || chapterTitlePhase != 0) {
                if (fastForwardTimer != null) fastForwardTimer.stop();
                return;
            }
            advanceDialogue();
        });
    }

    private void initTitleTimer() {
        titleTimer = new javax.swing.Timer(40, e -> {
            long elapsed = System.currentTimeMillis() - chapterTitleStartTime;
            if (chapterTitlePhase == 1) {
                chapterTitleAlpha = Math.min(1f, elapsed / (float) TITLE_FADE_MS);
                if (chapterTitleAlpha >= 1f) {
                    chapterTitlePhase = 2;
                    chapterTitleStartTime = System.currentTimeMillis();
                }
            } else if (chapterTitlePhase == 2) {
                if (elapsed >= TITLE_HOLD_MS) {
                    chapterTitlePhase = 3;
                    chapterTitleStartTime = System.currentTimeMillis();
                }
            } else if (chapterTitlePhase == 3) {
                chapterTitleAlpha = Math.max(0f, 1f - elapsed / (float) TITLE_FADE_MS);
                if (chapterTitleAlpha <= 0f) {
                    chapterTitlePhase = 4;
                    chapterEndBlackScreen = true;
                    if (titleTimer != null) titleTimer.stop();
                }
            }
            repaint();
        });
    }

    private void initEnterFadeTimer() {
        enterFadeTimer = new javax.swing.Timer(ENTER_FADE_TIMER_DELAY, e -> {
            long elapsed = System.currentTimeMillis() - enterFadeStartTime;
            int quoteTotal = QUOTE_FADE_IN_MS + QUOTE_HOLD_MS + QUOTE_FADEOUT_MS;
            if (elapsed < QUOTE_FADE_IN_MS) {
                quoteTextAlpha = elapsed / (float) QUOTE_FADE_IN_MS;
                enterFadeAlpha = 1f;
            } else if (elapsed < QUOTE_FADE_IN_MS + QUOTE_HOLD_MS) {
                quoteTextAlpha = 1f;
                enterFadeAlpha = 1f;
            } else if (elapsed < quoteTotal) {
                quoteTextAlpha = 1f - (elapsed - QUOTE_FADE_IN_MS - QUOTE_HOLD_MS) / (float) QUOTE_FADEOUT_MS;
                enterFadeAlpha = 1f;
            } else {
                long sceneFadeElapsed = elapsed - quoteTotal;
                enterFadeAlpha = Math.max(0f, 1f - sceneFadeElapsed / (float) ENTER_FADE_MS);
                quoteTextAlpha = 0f;
                if (enterFadeAlpha <= 0f) {
                    if (enterFadeTimer != null) enterFadeTimer.stop();
                    startSceneFade();
                    lineStartTime = System.currentTimeMillis();
                }
            }
            repaint();
        });
    }

    private void loadBackgroundForDialogueIndex(int dialogueIndex) {
        int cg = ChapterOneData.cgIndexForLine(dialogueIndex);
        if (cg == currentCgIndex && chapterBg != null) return;
        currentCgIndex = cg;
        setChapterBackgroundFromFile(ChapterOneData.bgFileForCg(cg));
        if (enterFadeAlpha <= 0f)
            startSceneFade();
    }

    /** 从文件名加载并设为当前章节背景（图片1等，之后可换成别的图） */
    public void setChapterBackgroundFromFile(String filename) {
        Image img = loadImageCandidatesRaw(filename);
        if (img != null)
            chapterBg = img;
        else
            System.err.println("Chapter background " + filename + " not found.");
    }

    /** 直接设置章节背景图（便于后续换成别的图片） */
    public void setChapterBackground(Image image) {
        if (image != null)
            chapterBg = image;
    }

    private Image loadImageCandidatesRaw(String filename) {
        String[] classpathCandidates = { "/image/" + filename, "/image/chapter one/" + filename, "/image/chapter%20one/" + filename };
        String[] fileCandidates = { "image/" + filename, "image/chapter one/" + filename };
        for (String p : classpathCandidates) {
            URL url = getClass().getResource(p);
            if (url != null)
                return new ImageIcon(url).getImage();
        }
        for (String p : fileCandidates) {
            File f = new File(p);
            if (f.exists())
                return new ImageIcon(f.getAbsolutePath()).getImage();
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        int idx = GameState.getState().getChapterOneDialogueIndex();
        loadBackgroundForDialogueIndex(idx);

        boolean inDialogue = idx < ChapterOneData.LINES.length && chapterTitlePhase == 0;
        if (sceneFadeAlpha < 1f)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sceneFadeAlpha));

        if (chapterBg != null) {
            int iw = chapterBg.getWidth(this);
            int ih = chapterBg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(chapterBg, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(20, 20, 40));
            g2.fillRect(0, 0, w, h);
        }

        if (inDialogue) {
            long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
            String lineText = ChapterOneData.LINES[idx][1];
            int totalChars = lineText.length();
            int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
            paintDialogueBox(g2, w, h, idx, visibleChars);
            if (visibleChars < totalChars)
                repaint(TEXT_ANIM_DELAY_MS);
        }
        if (sceneFadeAlpha < 1f)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 章节标题淡入淡出
        if (chapterTitlePhase > 0 && chapterTitlePhase < 4 && chapterTitleAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, chapterTitleAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.WHITE);
            Font f = g2.getFont().deriveFont(Font.BOLD, 36f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String title = ChapterOneData.CHAPTER_TITLE;
            g2.drawString(title, (w - fm.stringWidth(title)) / 2, h / 2 + fm.getAscent() / 2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        if (pauseMenuVisible)
            paintPauseMenu(g2, w, h);
        if (chapterEndBlackScreen) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            // TODO: 之后在此处做点击交互地图
        }
        // 进入游戏：黑屏遮罩 + 中央引言（读档时跳过）
        if (enterFadeAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, enterFadeAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
        }
        if (quoteTextAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, quoteTextAlpha));
            g2.setColor(Color.WHITE);
            Font font = g2.getFont().deriveFont(Font.ITALIC, 28f);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(QUOTE_TEXT);
            int th = fm.getAscent();
            g2.drawString(QUOTE_TEXT, (w - tw) / 2, h / 2 + th / 2);
        }
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

        paintBtn(g2, pauseSaveBounds, "Save", hoverPauseSave, pressedPauseSave);
        paintBtn(g2, pauseLoadBounds, "Load", hoverPauseLoad, pressedPauseLoad);
        paintBtn(g2, pauseSettingsBounds, "Settings", hoverPauseSettings, pressedPauseSettings);
        paintBtn(g2, pauseHistoryBounds, "History", hoverPauseHistory, pressedPauseHistory);
        paintBtn(g2, pauseQuitBounds, "Quit to Title", hoverPauseQuit, pressedPauseQuit);
    }

    /** 底部对话栏：说话人 + 正文（visibleChars 为打字机可见字数，-1 或 ≥length 表示全部） */
    private void paintDialogueBox(Graphics2D g2, int w, int h, int lineIndex, int visibleChars) {
        if (lineIndex < 0 || lineIndex >= ChapterOneData.LINES.length) return;
        String[] line = ChapterOneData.LINES[lineIndex];
        String speaker = line[0];
        String text = line[1];
        if (visibleChars < 0 || visibleChars > text.length())
            visibleChars = text.length();
        String visibleText = text.substring(0, visibleChars);

        int boxH = (int) (h * 0.22);
        int margin = (int) (w * 0.04);
        int boxY = h - boxH - margin;
        int boxW = w - margin * 2;
        int boxX = margin;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        int pad = 14;
        int y = boxY + pad;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(220, 180, 80));
        if (!speaker.isEmpty())
            g2.drawString(speaker, boxX + pad, y + g2.getFontMetrics().getAscent());
        y += 28;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.setColor(new Color(240, 240, 235));
        drawWrappedText(g2, visibleText, boxX + pad, y, boxW - pad * 2, 20);
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxW, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.isEmpty() ? new String[0] : text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            String tryLine = line.length() == 0 ? word : line.toString() + " " + word;
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
        if (line.length() > 0)
            g2.drawString(line.toString(), x, currentY + fm.getAscent());
    }

    private void paintBtn(Graphics2D g2, Rectangle r, String text, boolean hover, boolean pressed) {
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

    private void initKey() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (enterFadeAlpha > 0f || quoteTextAlpha > 0f) return;
                    pauseMenuVisible = !pauseMenuVisible;
                    clearPauseHover();
                    repaint();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !pauseMenuVisible) {
                    e.consume();
                    if (enterFadeAlpha > 0f || quoteTextAlpha > 0f) return;
                    spaceKeyHeld = true;
                    performAdvanceOrReveal();
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
                    if (fastForwardTimer != null && fastForwardTimer.isRunning())
                        fastForwardTimer.stop();
                }
            }
        });
    }

    private void clearPauseHover() {
        hoverPauseSave = hoverPauseLoad = hoverPauseSettings = hoverPauseHistory = hoverPauseQuit = false;
        pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
    }

    private void initMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!pauseMenuVisible) return;
                Point p = e.getPoint();
                hoverPauseSave = pauseSaveBounds.contains(p);
                hoverPauseLoad = pauseLoadBounds.contains(p);
                hoverPauseSettings = pauseSettingsBounds.contains(p);
                hoverPauseHistory = pauseHistoryBounds.contains(p);
                hoverPauseQuit = pauseQuitBounds.contains(p);
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!pauseMenuVisible) return;
                Point p = e.getPoint();
                pressedPauseSave = pauseSaveBounds.contains(p);
                pressedPauseLoad = pauseLoadBounds.contains(p);
                pressedPauseSettings = pauseSettingsBounds.contains(p);
                pressedPauseHistory = pauseHistoryBounds.contains(p);
                pressedPauseQuit = pauseQuitBounds.contains(p);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pauseMenuVisible) {
                    Point p = e.getPoint();
                    if (pressedPauseSave && pauseSaveBounds.contains(p)) {
                        pressedPauseSave = false;
                        handleSave();
                    } else if (pressedPauseLoad && pauseLoadBounds.contains(p)) {
                        pressedPauseLoad = false;
                        handleLoad();
                    } else if (pressedPauseSettings && pauseSettingsBounds.contains(p)) {
                        pressedPauseSettings = false;
                        handleSettings();
                    } else if (pressedPauseHistory && pauseHistoryBounds.contains(p)) {
                        pressedPauseHistory = false;
                        handleHistory();
                    } else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                        pressedPauseQuit = false;
                        int quitChoice = JOptionPane.showConfirmDialog(ChapterOneScene.this,
                                "Are you sure you want to quit?",
                                "Quit to Title",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (quitChoice == JOptionPane.YES_OPTION && onQuitToTitle != null)
                            onQuitToTitle.run();
                    } else {
                        pauseMenuVisible = false;
                    }
                    clearPauseHover();
                    repaint();
                    return;
                }
                // 点击推进对话（无暂停、无进入黑屏/引言时）
                if (enterFadeAlpha <= 0f && quoteTextAlpha <= 0f) {
                    int idx = GameState.getState().getChapterOneDialogueIndex();
                    if (chapterTitlePhase == 1 || chapterTitlePhase == 2 || chapterTitlePhase == 3) {
                        if (chapterTitlePhase == 2) {
                            chapterTitlePhase = 3;
                            chapterTitleStartTime = System.currentTimeMillis();
                        }
                    } else if (idx < ChapterOneData.LINES.length) {
                        performAdvanceOrReveal();
                    }
                }
                pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                clearPauseHover();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (pauseMenuVisible) {
                    clearPauseHover();
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void handleSave() {
        StoryState state = GameState.getState();
        state.setSavedChapter(1);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Save", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                state.setLastUsedSaveSlot(slot);
                try {
                    SaveLoad.save(state, "saves/slot" + slot + ".dat");
                    JOptionPane.showMessageDialog(ChapterOneScene.this, "Saved to slot " + slot);
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ChapterOneScene.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            grid.add(b);
        }
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.add(grid, BorderLayout.CENTER);
        JPanel chapterBtns = new JPanel(new FlowLayout());
        JButton ch1 = new JButton("Chapter One");
        JButton ch2 = new JButton("Chapter Two");
        JButton ch3 = new JButton("Chapter Three");
        ch1.addActionListener(ev -> {
            int c = JOptionPane.showConfirmDialog(dialog, "Are you sure? This may affect your game experience.", "Chapter One", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                GameState.getState().setChapterOneDialogueIndex(ChapterOneData.LINES.length);
                GameState.getState().setSavedChapter(1);
                chapterTitlePhase = 4;
                chapterEndBlackScreen = true;
                loadBackgroundForDialogueIndex(ChapterOneData.LINES.length);
                dialog.dispose();
                pauseMenuVisible = false;
                repaint();
            }
        });
        ch2.addActionListener(ev -> JOptionPane.showMessageDialog(dialog, "Chapter 2 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE));
        ch3.addActionListener(ev -> JOptionPane.showMessageDialog(dialog, "Chapter 3 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE));
        chapterBtns.add(ch1);
        chapterBtns.add(ch2);
        chapterBtns.add(ch3);
        chapterBtns.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        main.add(chapterBtns, BorderLayout.SOUTH);
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
                        JOptionPane.showMessageDialog(ChapterOneScene.this, "Empty slot " + slot);
                        return;
                    }
                    int savedCh = loaded.getSavedChapter();
                    if (savedCh == 2) {
                        JOptionPane.showMessageDialog(ChapterOneScene.this, "Chapter 2 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (savedCh == 3) {
                        JOptionPane.showMessageDialog(ChapterOneScene.this, "Chapter 3 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    GameState.setState(loaded);
                    lastUsedSaveSlot = loaded.getLastUsedSaveSlot();
                    int idx = loaded.getChapterOneDialogueIndex();
                    loadBackgroundForDialogueIndex(idx);
                    if (idx >= ChapterOneData.LINES.length) {
                        chapterTitlePhase = 4;
                    } else {
                        lineStartTime = System.currentTimeMillis();
                    }
                    dialog.dispose();
                    pauseMenuVisible = false;
                    repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ChapterOneScene.this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            grid.add(b);
        }
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.add(grid, BorderLayout.CENTER);
        JPanel chapterBtns = new JPanel(new FlowLayout());
        JButton ch1 = new JButton("Chapter One");
        JButton ch2 = new JButton("Chapter Two");
        JButton ch3 = new JButton("Chapter Three");
        ch1.addActionListener(ev -> {
            int c = JOptionPane.showConfirmDialog(dialog, "Are you sure? This may affect your game experience.", "Chapter One", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                GameState.getState().setChapterOneDialogueIndex(ChapterOneData.LINES.length);
                GameState.getState().setSavedChapter(1);
                chapterTitlePhase = 4;
                chapterEndBlackScreen = true;
                loadBackgroundForDialogueIndex(ChapterOneData.LINES.length);
                dialog.dispose();
                pauseMenuVisible = false;
                repaint();
            }
        });
        ch2.addActionListener(ev -> JOptionPane.showMessageDialog(dialog, "Chapter 2 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE));
        ch3.addActionListener(ev -> JOptionPane.showMessageDialog(dialog, "Chapter 3 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE));
        chapterBtns.add(ch1);
        chapterBtns.add(ch2);
        chapterBtns.add(ch3);
        chapterBtns.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        main.add(chapterBtns, BorderLayout.SOUTH);
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(main);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** 点击/空格：若当前行未播完则立刻显现，否则推进下一条 */
    private void performAdvanceOrReveal() {
        int idx = GameState.getState().getChapterOneDialogueIndex();
        if (idx >= ChapterOneData.LINES.length || chapterTitlePhase != 0) return;
        String text = ChapterOneData.LINES[idx][1];
        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : 0;
        int totalChars = text.length();
        int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
        if (visibleChars < totalChars) {
            lineStartTime = System.currentTimeMillis() - TEXT_DURATION_MS;
            repaint();
        } else {
            advanceDialogue();
        }
    }

    /** 推进一条对话并记入 History；若到达结尾则启动章节标题淡入淡出 */
    private void advanceDialogue() {
        StoryState state = GameState.getState();
        int idx = state.getChapterOneDialogueIndex();
        if (idx >= ChapterOneData.LINES.length) return;
        String[] line = ChapterOneData.LINES[idx];
        state.addChapterOneHistory(line[0], line[1]);
        state.setChapterOneDialogueIndex(idx + 1);
        lineStartTime = System.currentTimeMillis();
        if (idx + 1 >= ChapterOneData.LINES.length) {
            chapterTitlePhase = 1;
            chapterTitleAlpha = 0f;
            chapterTitleStartTime = System.currentTimeMillis();
            if (titleTimer != null && !titleTimer.isRunning()) titleTimer.start();
        } else {
            loadBackgroundForDialogueIndex(idx + 1);
        }
        repaint();
    }

    private void handleHistory() {
        java.util.List<DialogueRecord> history = GameState.getState().getChapterOneHistory();
        StringBuilder sb = new StringBuilder();
        for (DialogueRecord r : history) {
            if (sb.length() > 0) sb.append("\n");
            String s = r.getSpeaker();
            if (s.isEmpty()) sb.append(r.getText());
            else sb.append(s).append(": ").append(r.getText());
        }
        JOptionPane.showMessageDialog(this,
                sb.length() > 0 ? sb.toString() : "No dialogue history yet.",
                "History",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleSettings() {
        float vol = StartScene.getMasterVolume();
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new BorderLayout(10, 10));
        JSlider slider = new JSlider(0, 100, (int) (vol * 100));
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(25);
        // 设置内仅 ding 随滑块变，主音量不变；关闭时再写入主音量
        slider.addChangeListener((ChangeListener) e -> {
            if (!slider.getValueIsAdjusting())
                StartScene.playVolumeTestDingAt(slider.getValue() / 100f);
        });
        JButton close = new JButton("Close");
        close.addActionListener(ev -> {
            StartScene.setMasterVolume(slider.getValue() / 100f);
            d.dispose();
        });
        p.add(new JLabel("Sound Volume"), BorderLayout.NORTH);
        p.add(slider, BorderLayout.CENTER);
        p.add(close, BorderLayout.SOUTH);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    @Override
    public void onEnter() {
        StoryState state = GameState.getState();
        lastUsedSaveSlot = state.getLastUsedSaveSlot();
        int idx = state.getChapterOneDialogueIndex();
        loadBackgroundForDialogueIndex(idx);
        if (idx >= ChapterOneData.LINES.length) {
            chapterTitlePhase = 4;
            chapterEndBlackScreen = true;
        } else {
            chapterTitlePhase = 0;
            chapterEndBlackScreen = false;
        }
        lineStartTime = System.currentTimeMillis();
        if (skipQuoteNextEnter) {
            skipQuoteNextEnter = false;
            enterFadeAlpha = 0f;
            quoteTextAlpha = 0f;
            if (idx < ChapterOneData.LINES.length)
                startSceneFade();
            else
                sceneFadeAlpha = 1f;
        } else {
            enterFadeAlpha = 1f;
            quoteTextAlpha = 0f;
            enterFadeStartTime = System.currentTimeMillis();
            if (enterFadeTimer != null && !enterFadeTimer.isRunning())
                enterFadeTimer.start();
        }
        if (titleTimer != null && titleTimer.isRunning())
            titleTimer.stop();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (enterFadeTimer != null && enterFadeTimer.isRunning())
            enterFadeTimer.stop();
        if (titleTimer != null && titleTimer.isRunning())
            titleTimer.stop();
        if (sceneFadeTimer != null && sceneFadeTimer.isRunning())
            sceneFadeTimer.stop();
        if (fastForwardTimer != null && fastForwardTimer.isRunning())
            fastForwardTimer.stop();
        if (holdSpaceTimer != null && holdSpaceTimer.isRunning())
            holdSpaceTimer.stop();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

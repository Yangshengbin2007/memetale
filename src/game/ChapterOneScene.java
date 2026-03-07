package game;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏第一章场景。点击主菜单「开始游戏」后切换到此场景，主页面完全消失。
 * ESC 打开/关闭暂停菜单（存档/读档/设置/历史/返回主菜单）。
 */
public class ChapterOneScene extends JPanel implements Scene {
    private final Runnable onQuitToTitle;
    private Image chapterBg;
    private boolean pauseMenuVisible = false;
    private boolean hoverPauseSave, hoverPauseLoad, hoverPauseSettings, hoverPauseHistory, hoverPauseQuit;
    private boolean pressedPauseSave, pressedPauseLoad, pressedPauseSettings, pressedPauseHistory, pressedPauseQuit;
    private final Rectangle pauseSaveBounds = new Rectangle();
    private final Rectangle pauseLoadBounds = new Rectangle();
    private final Rectangle pauseSettingsBounds = new Rectangle();
    private final Rectangle pauseHistoryBounds = new Rectangle();
    private final Rectangle pauseQuitBounds = new Rectangle();

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

    public ChapterOneScene(Runnable onQuitToTitle) {
        this.onQuitToTitle = onQuitToTitle;
        setBackground(Color.BLACK);
        loadBackground();
        initEnterFadeTimer();
        initKey();
        initMouse();
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
                if (enterFadeAlpha <= 0f && enterFadeTimer != null) {
                    enterFadeTimer.stop();
                }
            }
            repaint();
        });
    }

    private void loadBackground() {
        setChapterBackgroundFromFile("gamecg1.jpg");
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
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24f));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Chapter 1";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
        }

        if (pauseMenuVisible)
            paintPauseMenu(g2, w, h);
        // 进入游戏：黑屏遮罩 + 中央引言
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
                    pauseMenuVisible = !pauseMenuVisible;
                    clearPauseHover();
                    repaint();
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
                if (!pauseMenuVisible) return;
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
                    JOptionPane.showMessageDialog(ChapterOneScene.this, "History feature will be implemented later.");
                } else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                    pressedPauseQuit = false;
                    if (onQuitToTitle != null) onQuitToTitle.run();
                }
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

    private static final int DIALOG_FADE_MS = 220;
    private static final int DIALOG_FADE_STEP_MS = 25;

    private void showDialogWithFade(JDialog dialog) {
        dialog.setOpacity(0f);
        javax.swing.Timer fadeIn = new javax.swing.Timer(DIALOG_FADE_STEP_MS, null);
        fadeIn.addActionListener(e -> {
            float o = dialog.getOpacity() + (float) DIALOG_FADE_STEP_MS / DIALOG_FADE_MS;
            if (o >= 1f) {
                dialog.setOpacity(1f);
                fadeIn.stop();
            } else {
                dialog.setOpacity(o);
            }
        });
        fadeIn.start();
        dialog.setVisible(true);
    }

    private void fadeOutAndDispose(JDialog dialog, Runnable afterDispose) {
        javax.swing.Timer fadeOut = new javax.swing.Timer(DIALOG_FADE_STEP_MS, null);
        fadeOut.addActionListener(e -> {
            float o = dialog.getOpacity() - (float) DIALOG_FADE_STEP_MS / DIALOG_FADE_MS;
            if (o <= 0f) {
                dialog.setOpacity(0f);
                fadeOut.stop();
                dialog.dispose();
                if (afterDispose != null) afterDispose.run();
            } else {
                dialog.setOpacity(o);
            }
        });
        fadeOut.start();
    }

    private void handleSave() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Save", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                try {
                    SaveLoad.save(GameState.getState(), "saves/slot" + slot + ".dat");
                    JOptionPane.showMessageDialog(this, "Saved to slot " + slot);
                    fadeOutAndDispose(dialog, null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(b);
        }
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fadeOutAndDispose(dialog, null);
            }
        });
        showDialogWithFade(dialog);
    }

    private void handleLoad() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Load", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                try {
                    StoryState loaded = SaveLoad.load("saves/slot" + slot + ".dat");
                    if (loaded == null)
                        JOptionPane.showMessageDialog(this, "Empty slot " + slot);
                    else {
                        GameState.setState(loaded);
                        fadeOutAndDispose(dialog, null);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(b);
        }
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fadeOutAndDispose(dialog, null);
            }
        });
        showDialogWithFade(dialog);
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
        enterFadeAlpha = 1f;
        quoteTextAlpha = 0f;
        enterFadeStartTime = System.currentTimeMillis();
        if (enterFadeTimer != null && !enterFadeTimer.isRunning())
            enterFadeTimer.start();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (enterFadeTimer != null && enterFadeTimer.isRunning())
            enterFadeTimer.stop();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

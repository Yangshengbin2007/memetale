package main.java.game;

// Explicit imports for Swing and AWT (required for desktop Java SE projects)
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.io.File;
import java.awt.GridLayout;

public class StartScene extends JPanel implements Scene {
    // Durations (ms)
    private final int FADE_MS = 1000;
    private final int HOLD_MS = 3000;
    private final int TIMER_DELAY = 40; // ~25 FPS

    // images
    private Image jerryImg;
    private Image startImg;
    private Image gameCgImg;

    // alpha values
    private float alphaJerry = 0f;
    private float alphaStart = 0f;
    private float alphaCg = 0f;

    private javax.swing.Timer timer;

    // sound for jerry logo
    private Clip jerryClip;
    // 建议使用 WAV 而不是 MP3（Java 标准库对 MP3 不支持）
    private final String JERRY_SOUND_NAME = "beginsound.wav"; // 放在 sound/ 目录下
    // volume test ding sound
    private final String DING_SOUND_NAME = "ding.wav"; // 放在 sound/ 目录下

    // 背景 BGM（开始页面的循环音乐）
    private Clip bgmClip;
    private final String BGM_SOUND_NAME = "beginmusic.wav"; // 放在 music/ 目录下
    // BGM 最长播放时长（毫秒），例如只播放前 8 秒
    private final int BGM_MAX_PLAY_MS = 8000;
    private long bgmStartTime = -1L;

    // 全局音量（0.0f ~ 1.0f），影响所有声音
    private float masterVolume = 1.0f;

    // 主界面按钮交互状态
    private boolean hoverContinue = false;
    private boolean hoverSetting = false;
    private boolean hoverStartGames = false;
    private boolean hoverQuit = false;
    private boolean pressedContinue = false;
    private boolean pressedSetting = false;
    private boolean pressedStartGames = false;
    private boolean pressedQuit = false;
    private Rectangle continueBounds = new Rectangle();
    private Rectangle settingBounds = new Rectangle();
    private Rectangle startGamesBounds = new Rectangle();
    private Rectangle quitBounds = new Rectangle();

    // 暂停菜单（ESC）相关
    private boolean pauseMenuVisible = false;
    private boolean hoverPauseSave = false;
    private boolean hoverPauseLoad = false;
    private boolean hoverPauseSettings = false;
    private boolean hoverPauseHistory = false;
    private boolean hoverPauseQuit = false;
    private boolean pressedPauseSave = false;
    private boolean pressedPauseLoad = false;
    private boolean pressedPauseSettings = false;
    private boolean pressedPauseHistory = false;
    private boolean pressedPauseQuit = false;
    private Rectangle pauseSaveBounds = new Rectangle();
    private Rectangle pauseLoadBounds = new Rectangle();
    private Rectangle pauseSettingsBounds = new Rectangle();
    private Rectangle pauseHistoryBounds = new Rectangle();
    private Rectangle pauseQuitBounds = new Rectangle();

    // 读档淡入淡出效果
    private boolean loadFadeActive = false;
    private long loadFadeStartTime = 0L;
    private final int LOAD_FADE_TOTAL_MS = 800;
    private float loadFadeAlpha = 0f;

    // settings slider
    private JSlider slider;

    private enum State {
        JERRY_FADE_IN,
        JERRY_HOLD,
        JERRY_FADE_OUT,
        START_FADE_IN,
        DONE,
        START_TRANSITION_TO_CG,
        CG_FADE_IN,
        CG_DISPLAY
    }

    private State state = State.JERRY_FADE_IN;
    private long stateStart;

    public StartScene() {
        setBackground(Color.BLACK);
        loadImages();
        initTimer();
        initMouse();
        initKey();
    }

    private void loadImages() {
        // keep jerry scaled to a reasonable size (use ImageIcon to get real dimensions)
        // 优化一下 Jerry 图标大小，让整体更协调
        jerryImg = loadAndScaleCandidates("jerry.png", 220, 220);
        // load start background raw; we'll scale-to-cover in paintComponent
        startImg = loadImageCandidatesRaw("start.jpg");
        // we no longer use separate images for the buttons; they are drawn procedurally
        // preload game cg (optional) but we can load upon transition
        gameCgImg = null;
    }

    // 使用 ImageIcon 来获取图片尺寸并做缩放，避免 Image.getWidth(this) 在未显示时返回 -1
    private Image loadAndScaleCandidates(String filename, int maxW, int maxH) {
        String[] classpathCandidates = new String[] {
                "/image/" + filename,
                "/image/chapter one/" + filename,
                "/image/chapter%20one/" + filename
        };
        String[] fileCandidates = new String[] {
                "image/" + filename,
                "image/chapter one/" + filename
        };

        ImageIcon icon = null;
        for (String p : classpathCandidates) {
            icon = loadIconFromClasspath(p);
            if (icon != null) {
                System.out.println("Loaded image icon from classpath: " + p);
                break;
            }
        }
        if (icon == null) {
            for (String p : fileCandidates) {
                icon = loadIconFromFile(p);
                if (icon != null) {
                    System.out.println("Loaded image icon from file: " + p);
                    break;
                }
            }
        }
        if (icon == null) {
            System.err.println("Image not found for filename: " + filename + " (tried multiple locations)");
            return null;
        }

        int iw = icon.getIconWidth();
        int ih = icon.getIconHeight();
        if (iw <= 0 || ih <= 0)
            return icon.getImage();
        double scale = Math.min(1.0, Math.min((double) maxW / iw, (double) maxH / ih));
        if (scale < 1.0) {
            int nw = (int) (iw * scale);
            int nh = (int) (ih * scale);
            Image scaled = icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            return scaled;
        }
        return icon.getImage();
    }

    // raw load (no scaling) but using ImageIcon helpers
    private Image loadImageCandidatesRaw(String filename) {
        String[] classpathCandidates = new String[] {
                "/image/" + filename,
                "/image/chapter one/" + filename,
                "/image/chapter%20one/" + filename
        };
        String[] fileCandidates = new String[] {
                "image/" + filename,
                "image/chapter one/" + filename
        };

        ImageIcon icon = null;
        for (String p : classpathCandidates) {
            icon = loadIconFromClasspath(p);
            if (icon != null) {
                System.out.println("Loaded image icon from classpath: " + p);
                break;
            }
        }
        if (icon == null) {
            for (String p : fileCandidates) {
                icon = loadIconFromFile(p);
                if (icon != null) {
                    System.out.println("Loaded image icon from file: " + p);
                    break;
                }
            }
        }
        if (icon == null) {
            System.err.println("Image not found for filename: " + filename + " (tried multiple locations)");
            return null;
        }
        return icon.getImage();
    }

    private ImageIcon loadIconFromClasspath(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url != null)
                return new ImageIcon(url);
        } catch (Exception e) {
            // ignore fallback
        }
        return null;
    }

    private ImageIcon loadIconFromFile(String path) {
        try {
            File f = new File(path);
            if (f.exists())
                return new ImageIcon(f.getAbsolutePath());
        } catch (Exception e) {
            // ignore fallback
        }
        return null;
    }

    private void initMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    boolean oldSave = hoverPauseSave;
                    boolean oldLoad = hoverPauseLoad;
                    boolean oldSet = hoverPauseSettings;
                    boolean oldHist = hoverPauseHistory;
                    boolean oldQuit = hoverPauseQuit;

                    hoverPauseSave = pauseSaveBounds.contains(p);
                    hoverPauseLoad = pauseLoadBounds.contains(p);
                    hoverPauseSettings = pauseSettingsBounds.contains(p);
                    hoverPauseHistory = pauseHistoryBounds.contains(p);
                    hoverPauseQuit = pauseQuitBounds.contains(p);

                    if (oldSave != hoverPauseSave || oldLoad != hoverPauseLoad || oldSet != hoverPauseSettings
                            || oldHist != hoverPauseHistory || oldQuit != hoverPauseQuit) {
                        repaint();
                    }
                } else {
                    boolean oldHoverContinue = hoverContinue;
                    boolean oldHoverSetting = hoverSetting;
                    boolean oldHoverStart = hoverStartGames;
                    boolean oldHoverQuit = hoverQuit;
                    hoverContinue = continueBounds.contains(p);
                    hoverSetting = settingBounds.contains(p);
                    hoverStartGames = startGamesBounds.contains(p);
                    hoverQuit = quitBounds.contains(p);
                    if (hoverContinue != oldHoverContinue || hoverSetting != oldHoverSetting || hoverStartGames != oldHoverStart
                            || hoverQuit != oldHoverQuit)
                        repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    if (pauseSaveBounds.contains(p)) {
                        pressedPauseSave = true;
                    } else if (pauseLoadBounds.contains(p)) {
                        pressedPauseLoad = true;
                    } else if (pauseSettingsBounds.contains(p)) {
                        pressedPauseSettings = true;
                    } else if (pauseHistoryBounds.contains(p)) {
                        pressedPauseHistory = true;
                    } else if (pauseQuitBounds.contains(p)) {
                        pressedPauseQuit = true;
                    }
                    repaint();
                } else {
                    if (continueBounds.contains(p)) {
                        pressedContinue = true;
                        repaint();
                    } else if (settingBounds.contains(p)) {
                        pressedSetting = true;
                        repaint();
                    } else if (startGamesBounds.contains(p)) {
                        pressedStartGames = true;
                        repaint();
                    } else if (quitBounds.contains(p)) {
                        pressedQuit = true;
                        repaint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    if (pressedPauseSave && pauseSaveBounds.contains(p)) {
                        pressedPauseSave = false;
                        handlePauseSaveClicked();
                    } else if (pressedPauseLoad && pauseLoadBounds.contains(p)) {
                        pressedPauseLoad = false;
                        handlePauseLoadClicked();
                    } else if (pressedPauseSettings && pauseSettingsBounds.contains(p)) {
                        pressedPauseSettings = false;
                        handlePauseSettingsClicked();
                    } else if (pressedPauseHistory && pauseHistoryBounds.contains(p)) {
                        pressedPauseHistory = false;
                        handlePauseHistoryClicked();
                    } else if (pressedPauseQuit && pauseQuitBounds.contains(p)) {
                        pressedPauseQuit = false;
                        handlePauseQuitClicked();
                    } else {
                        pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                    }
                    repaint();
                } else {
                    if (pressedContinue && continueBounds.contains(p)) {
                        pressedContinue = false;
                        onContinueClicked();
                    } else if (pressedSetting && settingBounds.contains(p)) {
                        pressedSetting = false;
                        onSettingClicked();
                    } else if (pressedStartGames && startGamesBounds.contains(p)) {
                        pressedStartGames = false;
                        onStartGamesClicked();
                    } else if (pressedQuit && quitBounds.contains(p)) {
                        pressedQuit = false;
                        onQuitClicked();
                    } else {
                        pressedContinue = false;
                        pressedSetting = false;
                        pressedStartGames = false;
                        pressedQuit = false;
                    }
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (pauseMenuVisible) {
                    hoverPauseSave = hoverPauseLoad = hoverPauseSettings = hoverPauseHistory = hoverPauseQuit = false;
                    pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                    repaint();
                } else {
                    hoverContinue = hoverSetting = hoverStartGames = hoverQuit = false;
                    pressedContinue = pressedSetting = pressedStartGames = pressedQuit = false;
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void onSettingClicked() {
        // show a styled dialog that matches start screen aesthetic (no external image)
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Settings",
                    Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);

            JPanel bg = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    // 背景稍微提亮一点，避免整体太暗
                    g2.setColor(new Color(30, 30, 30, 220));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            bg.setLayout(new GridBagLayout());
            bg.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JPanel card = new JPanel(new BorderLayout(10, 10)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    // subtle gold border + dark center to match theme
                    // 再提升亮度，让设置内容更加清晰
                    g2.setColor(new Color(110, 110, 110, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setStroke(new BasicStroke(3f));
                    g2.setColor(new Color(200, 160, 60)); // gold
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
                    g2.dispose();
                }
            };

            JPanel center = new JPanel(new BorderLayout(8, 8));
            center.setOpaque(false);
            JLabel lbl = new JLabel("Sound Volume");
            lbl.setForeground(new Color(250, 250, 240));

            slider = new JSlider(0, 100, (int) (masterVolume * 100));
            slider.setPaintTicks(true);
            // 提升对比度，让刻度和数字更清晰
            slider.setForeground(new Color(240, 240, 240));
            slider.setBackground(new Color(60, 60, 60));
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintLabels(true);

            // 松开滑块时应用音量并播放一次 ding.wav 作为预览
            slider.addChangeListener((ChangeListener) e -> {
                if (!slider.getValueIsAdjusting()) {
                    int vol = slider.getValue();
                    masterVolume = Math.max(0f, Math.min(1f, vol / 100f));
                    updateAllVolumes();
                    playVolumeTestDing();
                }
            });

            center.add(lbl, BorderLayout.NORTH);
            center.add(slider, BorderLayout.CENTER);
            card.add(center, BorderLayout.CENTER);

            // btns panel: replace Save/Load/Close with Apply (settings only) and Close
            JPanel btns = new JPanel();
            btns.setOpaque(false);
            JButton apply = new JButton("Apply");
            JButton close = new JButton("Close");
            // styled buttons
            Color btnBg = new Color(30, 144, 255); // DodgerBlue-like
            apply.setBackground(btnBg);
            apply.setForeground(Color.BLACK); // 改为黑字，提高清晰度
            close.setBackground(new Color(200, 200, 200));
            close.setForeground(Color.BLACK); // 底部两个按钮的文字都使用黑色
            apply.setFocusPainted(false);
            close.setFocusPainted(false);

            // Apply action: 应用全局音量设置
            apply.addActionListener(ev -> {
                // 音量已经在松开滑块时实时应用，这里只关闭窗口
                System.out.println("Master volume confirmed: " + masterVolume);
                dialog.dispose();
            });

            close.addActionListener(ev -> dialog.dispose());
            btns.add(apply);
            btns.add(close);
            card.add(btns, BorderLayout.SOUTH);

            bg.add(card);
            dialog.setContentPane(bg);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
    }

    private void onStartGamesClicked() {
        // start transition: fade out current start image then fade in gameCgImg
        if (state == State.START_FADE_IN || state == State.DONE) {
            state = State.START_TRANSITION_TO_CG;
            stateStart = System.currentTimeMillis();
            // 点击开始游戏立即停止 BGM
            stopBackgroundMusic();
            // 确保在 DONE 状态下也能继续驱动淡入淡出动画
            if (timer != null && !timer.isRunning()) {
                timer.start();
            }
            // ensure gameCg loaded
            if (gameCgImg == null) {
                gameCgImg = loadImageCandidatesRaw("gamecg1.jpg");
                if (gameCgImg == null) {
                    System.err.println("gamecg1.jpg not found; transition will still fade out start.");
                }
            }
        }
    }

    private void onContinueClicked() {
        // 主界面上的 Continue：直接弹出读档窗口
        showLoadSlotDialog();
    }

    private void onQuitClicked() {
        // 主界面上的退出按钮：直接退出程序
        System.exit(0);
    }

    private void initTimer() {
        stateStart = System.currentTimeMillis();
        timer = new javax.swing.Timer(TIMER_DELAY, e -> {
            long elapsed = System.currentTimeMillis() - stateStart;
            switch (state) {
                case JERRY_FADE_IN:
                    alphaJerry = Math.min(1f, elapsed / (float) FADE_MS);
                    if (alphaJerry >= 1f) {
                        state = State.JERRY_HOLD;
                        stateStart = System.currentTimeMillis();
                    }
                    break;
                case JERRY_HOLD:
                    alphaJerry = 1f;
                    if (elapsed >= HOLD_MS) {
                        state = State.JERRY_FADE_OUT;
                        stateStart = System.currentTimeMillis();
                    }
                    break;
                case JERRY_FADE_OUT:
                    alphaJerry = Math.max(0f, 1f - elapsed / (float) FADE_MS);
                    if (alphaJerry <= 0f) {
                        // Jerry 图标完全消失时，立即停止音效
                        stopJerrySound();
                        state = State.START_FADE_IN;
                        stateStart = System.currentTimeMillis();
                        alphaStart = 0f;
                    }
                    break;
                case START_FADE_IN:
                    alphaStart = Math.min(1f, elapsed / (float) FADE_MS);
                    if (alphaStart >= 1f) {
                        state = State.DONE;
                        stateStart = System.currentTimeMillis();
                        // 主页面完全显示后再开始播放 BGM
                        startBackgroundMusicLoop();
                        timer.stop(); // stop idle animation; we'll restart on enter
                    }
                    break;
                case START_TRANSITION_TO_CG:
                    // fade out start image
                    alphaStart = Math.max(0f, 1f - elapsed / (float) FADE_MS);
                    if (alphaStart <= 0f) {
                        // begin cg fade-in
                        state = State.CG_FADE_IN;
                        stateStart = System.currentTimeMillis();
                        alphaCg = 0f;
                    }
                    break;
                case CG_FADE_IN:
                    alphaCg = Math.min(1f, elapsed / (float) FADE_MS);
                    if (alphaCg >= 1f) {
                        state = State.CG_DISPLAY;
                        stateStart = System.currentTimeMillis();
                    }
                    break;
                case CG_DISPLAY:
                    // remain showing game cg
                    break;
                case DONE:
                default:
                    break;
            }
            // 控制 BGM 的最大播放时长（例如只播放前 BGM_MAX_PLAY_MS 毫秒）
            if (bgmClip != null && bgmStartTime > 0) {
                long bgmElapsed = System.currentTimeMillis() - bgmStartTime;
                if (bgmElapsed >= BGM_MAX_PLAY_MS) {
                    stopBackgroundMusic();
                }
            }
            // 读档淡入淡出效果更新
            if (loadFadeActive) {
                long t = System.currentTimeMillis() - loadFadeStartTime;
                float progress = Math.min(1f, t / (float) LOAD_FADE_TOTAL_MS);
                if (progress < 0.5f) {
                    loadFadeAlpha = progress * 2f; // 0 -> 1
                } else {
                    loadFadeAlpha = (1f - progress) * 2f; // 1 -> 0
                }
                if (progress >= 1f) {
                    loadFadeActive = false;
                    loadFadeAlpha = 0f;
                }
            }
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        // draw centered jerry (existing behavior)
        if ((state == State.JERRY_FADE_IN || state == State.JERRY_HOLD || state == State.JERRY_FADE_OUT)
                && jerryImg != null) {
            int iw = jerryImg.getWidth(this);
            int ih = jerryImg.getHeight(this);
            int x = (w - iw) / 2;
            int y = (h - ih) / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaJerry));
            g2.drawImage(jerryImg, x, y, this);
        }

        // draw start background filling the whole panel (cover) when visible
        if ((state == State.START_FADE_IN || state == State.DONE || state == State.START_TRANSITION_TO_CG)
                && startImg != null) {
            int iw = startImg.getWidth(this);
            int ih = startImg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih); // cover
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int imgX = (w - sw) / 2;
                int imgY = (h - sh) / 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaStart));
                g2.drawImage(startImg, imgX, imgY, sw, sh, this);

                // centered stacked button positions (Continue / Start Game / Settings / Quit)
                int btnW = (int) (sw * 0.4); // 按钮宽度稍微收窄
                int btnH = (int) (sh * 0.08); // 按钮高度也减小
                int spacing = (int) (sh * 0.025); // 垂直间距

                int centerX = imgX + sw / 2;
                int firstBtnY = imgY + (int) (sh * 0.50);

                int continueBtnY = firstBtnY;
                int startBtnY = continueBtnY + btnH + spacing;
                int settingBtnY = startBtnY + btnH + spacing;
                int quitBtnY = settingBtnY + btnH + spacing;

                int bx = centerX - btnW / 2;
                continueBounds.setBounds(bx, continueBtnY, btnW, btnH); // Continue
                startGamesBounds.setBounds(bx, startBtnY, btnW, btnH); // Start Game
                settingBounds.setBounds(bx, settingBtnY, btnW, btnH); // Settings
                quitBounds.setBounds(bx, quitBtnY, btnW, btnH); // Quit

                // draw CONTINUE button
                paintStyledButton(g2, continueBounds, "Continue", hoverContinue, pressedContinue);

                // draw START GAME button - stylized (blue + gold frame)
                paintStyledButton(g2, startGamesBounds, "Start Game", hoverStartGames, pressedStartGames);

                // draw SETTINGS button
                paintStyledButton(g2, settingBounds, "Settings", hoverSetting, pressedSetting);

                // draw QUIT button
                paintStyledButton(g2, quitBounds, "Quit", hoverQuit, pressedQuit);

                // hover/pressed overlay handled inside paintStyledButton
            }
        }

        // draw game cg if transitioning/shown
        if ((state == State.CG_FADE_IN || state == State.CG_DISPLAY) && gameCgImg != null) {
            int iw = gameCgImg.getWidth(this);
            int ih = gameCgImg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih); // cover
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaCg));
                g2.drawImage(gameCgImg, x, y, sw, sh, this);
            }
        }

        // 暂停菜单（ESC）覆盖在最上层
        if (pauseMenuVisible && (state == State.CG_FADE_IN || state == State.CG_DISPLAY)) {
            paintPauseMenu(g2, w, h);
        }

        // 读档淡入淡出整体遮罩
        if (loadFadeActive && loadFadeAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, loadFadeAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
        }

        g2.dispose();
    }

    // helper to draw stylized rounded button with hover/pressed animations
    private void paintStyledButton(Graphics2D g2, Rectangle r, String text, boolean hover, boolean pressed) {
        // compute slight scale when pressed for click animation
        double scale = pressed ? 0.96 : 1.0;
        int cw = (int) Math.round(r.width * scale);
        int ch = (int) Math.round(r.height * scale);
        int cx = r.x + (r.width - cw) / 2;
        int cy = r.y + (r.height - ch) / 2;

        // background gradient (deep blue) and inner darker area
        GradientPaint gp = new GradientPaint(cx, cy, new Color(10, 60, 110), cx, cy + ch, new Color(20, 100, 180));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setPaint(gp);
        g2.fillRoundRect(cx, cy, cw, ch, 16, 16);

        // inner shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(cx + 4, cy + (int) (ch * 0.55), cw - 8, ch / 6, 12, 12);

        // gold border
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(200, 160, 60));
        g2.drawRoundRect(cx, cy, cw, ch, 16, 16);

        // text
        Font orig = g2.getFont();
        Font f = orig.deriveFont(Font.BOLD, Math.max(14f, cw * 0.08f));
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics(f);
        int tx = cx + (cw - fm.stringWidth(text)) / 2;
        int ty = cy + (ch + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(245, 245, 240));
        g2.drawString(text, tx, ty);

        // hover / pressed overlay (semi-transparent blue)
        if (hover || pressed) {
            float a = pressed ? 0.40f : 0.20f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setColor(new Color(0x1E90FF));
            g2.fillRoundRect(cx, cy, cw, ch, 16, 16);
        }

        // restore composite/font
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setFont(orig);
    }

    // 绘制 ESC 暂停菜单
    private void paintPauseMenu(Graphics2D g2, int w, int h) {
        // 半透明黑色遮罩
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        int panelW = (int) (w * 0.5);
        int panelH = (int) (h * 0.55);
        int px = (w - panelW) / 2;
        int py = (h - panelH) / 2;

        Graphics2D gPanel = (Graphics2D) g2.create();
        // 卡片背景
        gPanel.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        gPanel.setColor(new Color(40, 40, 40, 230));
        gPanel.fillRoundRect(px, py, panelW, panelH, 18, 18);
        gPanel.setStroke(new BasicStroke(3f));
        gPanel.setColor(new Color(200, 160, 60));
        gPanel.drawRoundRect(px + 1, py + 1, panelW - 2, panelH - 2, 18, 18);

        int btnW = (int) (panelW * 0.7);
        int btnH = (int) (panelH * 0.12);
        int spacing = (panelH - btnH * 5) / 6;
        int centerX = w / 2;
        int x = centerX - btnW / 2;
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

        paintStyledButton(gPanel, pauseSaveBounds, "Save", hoverPauseSave, pressedPauseSave);
        paintStyledButton(gPanel, pauseLoadBounds, "Load", hoverPauseLoad, pressedPauseLoad);
        paintStyledButton(gPanel, pauseSettingsBounds, "Settings", hoverPauseSettings, pressedPauseSettings);
        paintStyledButton(gPanel, pauseHistoryBounds, "History", hoverPauseHistory, pressedPauseHistory);
        paintStyledButton(gPanel, pauseQuitBounds, "Quit to Title", hoverPauseQuit, pressedPauseQuit);

        gPanel.dispose();
    }

    @Override
    public void onEnter() {
        // reset state and start animation
        state = State.JERRY_FADE_IN;
        stateStart = System.currentTimeMillis();
        // 进入 Jerry 阶段时播放开场音效
        playJerrySoundOnce();
        alphaJerry = 0f;
        alphaStart = 0f;
        alphaCg = 0f;
        pauseMenuVisible = false;
        loadFadeActive = false;
        if (timer != null && !timer.isRunning())
            timer.start();
        // 确保键盘事件可用（ESC）
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (timer != null && timer.isRunning())
            timer.stop();
        // 离开场景时确保音效停止并释放资源
        stopJerrySound();
        stopBackgroundMusic();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    private void initKey() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    onEscapePressed();
                }
            }
        });
    }

    private void onEscapePressed() {
        // 只在真正“游戏中”（CG 阶段）允许弹出暂停菜单
        if (state == State.CG_FADE_IN || state == State.CG_DISPLAY) {
            pauseMenuVisible = !pauseMenuVisible;
            // 重置暂停按钮状态
            hoverPauseSave = hoverPauseLoad = hoverPauseSettings = hoverPauseHistory = hoverPauseQuit = false;
            pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit =
                    false;
            repaint();
        }
    }

    /**
     * 播放 Jerry 出现时的音效，只播放一次。
     */
    private void playJerrySoundOnce() {
        // 如有正在播放的旧 Clip，先停掉
        stopJerrySound();
        try {
            Clip clip = loadSoundClip(JERRY_SOUND_NAME);
            if (clip != null) {
                jerryClip = clip;
                applyVolumeToClip(jerryClip);
                jerryClip.start();
            } else {
                System.err.println("Could not load jerry sound: " + JERRY_SOUND_NAME);
            }
        } catch (Exception ex) {
            System.err.println("Error playing jerry sound: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 停止当前 Jerry 音效并释放资源。
     */
    private void stopJerrySound() {
        if (jerryClip != null) {
            try {
                if (jerryClip.isRunning()) {
                    jerryClip.stop();
                }
                jerryClip.close();
            } catch (Exception ignore) {
            } finally {
                jerryClip = null;
            }
        }
    }

    /**
     * 尝试从 classpath (/sound/) 或项目目录 sound/ 中加载指定文件名的音效。
     */
    private Clip loadSoundClip(String fileName) {
        AudioInputStream ais = null;
        try {
            // 1. 先尝试从 classpath 中读取（例如打包到 JAR 的 /sound/ 目录）
            URL url = getClass().getResource("/sound/" + fileName);
            if (url != null) {
                ais = AudioSystem.getAudioInputStream(url);
            } else {
                // 2. 回退到文件系统相对路径 sound/
                File f = new File("sound/" + fileName);
                if (!f.exists()) {
                    System.err.println("Sound file not found: " + f.getAbsolutePath());
                    return null;
                }
                ais = AudioSystem.getAudioInputStream(f);
            }
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception ex) {
            System.err.println("Failed to load sound: " + fileName + " - " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            // 注意：Clip 打开后会持有数据，这里不能提前关闭 ais；交给 Clip 管理
        }
    }

    /**
     * 开始在开始页面播放 BGM（循环播放），但总时长不超过 BGM_MAX_PLAY_MS。
     */
    private void startBackgroundMusicLoop() {
        stopBackgroundMusic();
        try {
            Clip clip = loadMusicClip(BGM_SOUND_NAME);
            if (clip != null) {
                bgmClip = clip;
                applyVolumeToClip(bgmClip);
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmStartTime = System.currentTimeMillis();
            } else {
                System.err.println("Could not load bgm sound: " + BGM_SOUND_NAME);
            }
        } catch (Exception ex) {
            System.err.println("Error playing bgm sound: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 停止开始页面 BGM 并释放资源。
     */
    private void stopBackgroundMusic() {
        bgmStartTime = -1L;
        if (bgmClip != null) {
            try {
                if (bgmClip.isRunning()) {
                    bgmClip.stop();
                }
                bgmClip.close();
            } catch (Exception ignore) {
            } finally {
                bgmClip = null;
            }
        }
    }

    /**
     * 尝试从 classpath (/music/) 或项目目录 music/ 中加载开始页面 BGM。
     */
    private Clip loadMusicClip(String fileName) {
        AudioInputStream ais = null;
        try {
            // 1. 先尝试从 classpath 中读取（例如打包到 JAR 的 /music/ 目录）
            URL url = getClass().getResource("/music/" + fileName);
            if (url != null) {
                ais = AudioSystem.getAudioInputStream(url);
            } else {
                // 2. 回退到文件系统相对路径 music/
                File f = new File("music/" + fileName);
                if (!f.exists()) {
                    System.err.println("Music file not found: " + f.getAbsolutePath());
                    return null;
                }
                ais = AudioSystem.getAudioInputStream(f);
            }
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception ex) {
            System.err.println("Failed to load music: " + fileName + " - " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            // 注意：Clip 打开后会持有数据，这里不能提前关闭 ais；交给 Clip 管理
        }
    }

    /**
     * 将当前 masterVolume 应用到指定 Clip。
     */
    private void applyVolumeToClip(Clip clip) {
        if (clip == null)
            return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float volume = Math.max(0f, Math.min(1f, masterVolume));
                if (volume == 0f) {
                    gain.setValue(gain.getMinimum());
                } else {
                    float dB = (float) (20.0 * Math.log10(volume));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 将当前音量设置应用到所有已存在的声音资源。
     */
    private void updateAllVolumes() {
        applyVolumeToClip(jerryClip);
        applyVolumeToClip(bgmClip);
    }

    // ========= 暂停菜单：保存/读取/设置/历史/退出 =========

    // 暂停菜单按钮行为（内部处理函数，从鼠标事件中调用）
    private void handlePauseSaveClicked() {
        showSaveSlotDialog();
    }

    private void handlePauseLoadClicked() {
        showLoadSlotDialog();
    }

    private void handlePauseSettingsClicked() {
        onSettingClicked();
    }

    private void handlePauseHistoryClicked() {
        // 目前仅占位，后续可以加入对话复述功能
        JOptionPane.showMessageDialog(this, "History feature will be implemented later.");
    }

    private void handlePauseQuitClicked() {
        // 游戏内 Quit：返回主页面（start 背景和按钮），不要重新播放 Jerry 开场
        pauseMenuVisible = false;
        loadFadeActive = false;
        // 直接切换到主菜单状态
        state = State.START_FADE_IN;
        stateStart = System.currentTimeMillis() - FADE_MS; // 让下一个 tick 立刻完成淡入并进入 DONE
        alphaJerry = 0f;
        alphaStart = 1f;
        alphaCg = 0f;
        stopJerrySound();
        stopBackgroundMusic();
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    private static final int SAVE_SLOT_COUNT = 8;

    private void showSaveSlotDialog() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Save",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= SAVE_SLOT_COUNT; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                performSave(slot);
                dialog.dispose();
            });
            panel.add(b);
        }
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showLoadSlotDialog() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Load",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= SAVE_SLOT_COUNT; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                String[] options = { "Load", "Delete", "Cancel" };
                int choice = JOptionPane.showOptionDialog(dialog,
                        "Choose action for slot " + slot,
                        "Slot " + slot,
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                if (choice == 0) { // Load
                    performLoad(slot);
                    dialog.dispose();
                } else if (choice == 1) { // Delete
                    deleteSaveSlot(slot);
                }
            });
            panel.add(b);
        }
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void performSave(int slot) {
        try {
            String path = "saves/slot" + slot + ".dat";
            SaveLoad.save(GameState.getState(), path);
            JOptionPane.showMessageDialog(this, "Saved to slot " + slot);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void performLoad(int slot) {
        try {
            String path = "saves/slot" + slot + ".dat";
            StoryState loaded = SaveLoad.load(path);
            if (loaded == null) {
                JOptionPane.showMessageDialog(this, "Empty slot " + slot, "Load",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            GameState.setState(loaded);
            // 触发淡出->淡入效果
            loadFadeActive = true;
            loadFadeStartTime = System.currentTimeMillis();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void deleteSaveSlot(int slot) {
        String path = "saves/slot" + slot + ".dat";
        File f = new File(path);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "Slot " + slot + " is already empty.", "Delete",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (f.delete()) {
            JOptionPane.showMessageDialog(this, "Deleted slot " + slot, "Delete",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete slot " + slot, "Delete",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 播放一次 ding.wav 作为音量测试（使用当前 masterVolume）。
     */
    private void playVolumeTestDing() {
        try {
            Clip clip = loadSoundClip(DING_SOUND_NAME);
            if (clip == null) {
                System.err.println("ding.wav not found for volume test.");
                return;
            }
            applyVolumeToClip(clip);
            // 播放完自动关闭资源
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception ex) {
            System.err.println("Failed to play ding.wav: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}

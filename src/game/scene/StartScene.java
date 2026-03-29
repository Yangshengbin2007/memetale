package game.scene;

import game.model.*;
import game.io.SaveLoad;
// Explicit imports for Swing and AWT (required for desktop Java SE projects)
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
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
import java.net.URL;
import java.io.File;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class StartScene extends JPanel implements Scene {
    // Durations (ms)
    private final int FADE_MS = 1000;
    private final int HOLD_MS = 3000;
    private final int TIMER_DELAY = 40; // ~25 FPS

    // images
    private Image jerryImg;
    private Image startImg;

    // alpha values
    private float alphaJerry = 0f;
    private float alphaStart = 0f;

    private javax.swing.Timer timer;

    // sound for jerry logo
    private Clip jerryClip;
    // Prefer WAV; stock Java does not decode MP3 without extra SPIs.
    private final String JERRY_SOUND_NAME = "beginsound.wav"; // under sound/
    // volume test ding sound
    private final String DING_SOUND_NAME = "ding.wav"; // under sound/
    private final String CLICK_SOUND_NAME = "click.wav"; // UI click; falls back to ding.wav

    // Title screen looping BGM
    private Clip bgmClip;
    private final String BGM_SOUND_NAME = "beginmusic.wav"; // under music/
    // Cap BGM length in ms (e.g. only the first eight seconds)
    private final int BGM_MAX_PLAY_MS = 8000;
    private long bgmStartTime = -1L;

    // Master volume 0..1 shared by the title menu and in-game settings
    private static float masterVolumeStatic = 1.0f;
    /** Mute all music (BGM). Applied on Apply click. */
    private static boolean muteAllMusicStatic = false;
    /** Mute all sound effects. Applied on Apply click. */
    private static boolean muteAllSoundEffectsStatic = false;

    public static float getMasterVolume() {
        return masterVolumeStatic;
    }

    public static void setMasterVolume(float v) {
        masterVolumeStatic = Math.max(0f, Math.min(1f, v));
    }

    public static boolean getMuteAllMusic() { return muteAllMusicStatic; }
    public static void setMuteAllMusic(boolean v) { muteAllMusicStatic = v; }
    public static boolean getMuteAllSoundEffects() { return muteAllSoundEffectsStatic; }
    public static void setMuteAllSoundEffects(boolean v) { muteAllSoundEffectsStatic = v; }

    // Title button hover/press state
    private boolean hoverContinue = false;
    private boolean hoverSetting = false;
    private boolean hoverStartGames = false;
    private boolean hoverMiniGames = false;
    private boolean hoverQuit = false;
    private boolean pressedContinue = false;
    private boolean pressedSetting = false;
    private boolean pressedStartGames = false;
    private boolean pressedMiniGames = false;
    private boolean pressedQuit = false;
    private final Rectangle continueBounds = new Rectangle();
    private final Rectangle settingBounds = new Rectangle();
    private final Rectangle startGamesBounds = new Rectangle();
    private final Rectangle miniGamesBounds = new Rectangle();
    private final Rectangle quitBounds = new Rectangle();

    /** Fired when Start Game is pressed; Main wires this to chapter one (or resume). */
    private Runnable onStartGame;
    /** Fired when Mini Games is pressed; Main wires the hub scene. */
    private Runnable onMiniGames;

    /** Next onEnter skips the Jerry intro (set when quitting to title from gameplay). */
    private boolean skipJerryNextEnter = false;

    // Load-game fade overlay
    private boolean loadFadeActive = false;
    private long loadFadeStartTime = 0L;
    private final int LOAD_FADE_TOTAL_MS = 800;
    private static final int START_GAME_FADEOUT_MS = 450; // fade before leaving title
    private float loadFadeAlpha = 0f;

    // settings slider
    private JSlider slider;

    private enum State {
        JERRY_FADE_IN,
        JERRY_HOLD,
        JERRY_FADE_OUT,
        START_FADE_IN,
        DONE,
        START_GAME_FADEOUT  // fade title out, then switch scene
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
        // Scale the Jerry sticker for layout balance
        jerryImg = loadAndScaleCandidates("jerry.png", 220, 220);
        // load start background raw; we'll scale-to-cover in paintComponent
        startImg = loadImageCandidatesRaw("start.jpg");
        // we no longer use separate images for the buttons; they are drawn procedurally
    }

    /** Main injects the Start Game handler. */
    public void setOnStartGame(Runnable r) {
        onStartGame = r;
    }

    /** Main injects the Mini Games handler. */
    public void setOnMiniGames(Runnable r) {
        onMiniGames = r;
    }

    public void setSkipJerryNextEnter(boolean skip) {
        skipJerryNextEnter = skip;
    }

        // ImageIcon gives reliable dimensions before the component is shown
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
                boolean oldHoverContinue = hoverContinue;
                boolean oldHoverSetting = hoverSetting;
                boolean oldHoverStart = hoverStartGames;
                boolean oldHoverMini = hoverMiniGames;
                boolean oldHoverQuit = hoverQuit;
                hoverContinue = continueBounds.contains(p);
                hoverSetting = settingBounds.contains(p);
                hoverStartGames = startGamesBounds.contains(p);
                hoverMiniGames = miniGamesBounds.contains(p);
                hoverQuit = quitBounds.contains(p);
                if (hoverContinue != oldHoverContinue || hoverSetting != oldHoverSetting || hoverStartGames != oldHoverStart
                        || hoverMiniGames != oldHoverMini || hoverQuit != oldHoverQuit)
                    repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (continueBounds.contains(p)) {
                    pressedContinue = true;
                    repaint();
                } else if (settingBounds.contains(p)) {
                    pressedSetting = true;
                    repaint();
                } else if (startGamesBounds.contains(p)) {
                    pressedStartGames = true;
                    repaint();
                } else if (miniGamesBounds.contains(p)) {
                    pressedMiniGames = true;
                    repaint();
                } else if (quitBounds.contains(p)) {
                    pressedQuit = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                if (state != State.DONE && state != State.START_FADE_IN) {
                    pressedContinue = pressedSetting = pressedStartGames = pressedMiniGames = pressedQuit = false;
                    repaint();
                    return;
                }
                if (pressedContinue && continueBounds.contains(p)) {
                    pressedContinue = false;
                    onContinueClicked();
                } else if (pressedSetting && settingBounds.contains(p)) {
                    pressedSetting = false;
                    onSettingClicked();
                } else if (pressedStartGames && startGamesBounds.contains(p)) {
                    pressedStartGames = false;
                    onStartGamesClicked();
                } else if (pressedMiniGames && miniGamesBounds.contains(p)) {
                    pressedMiniGames = false;
                    onMiniGamesClicked();
                } else if (pressedQuit && quitBounds.contains(p)) {
                    pressedQuit = false;
                    onQuitClicked();
                } else {
                    pressedContinue = false;
                    pressedSetting = false;
                    pressedStartGames = false;
                    pressedMiniGames = false;
                    pressedQuit = false;
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverContinue = hoverSetting = hoverStartGames = hoverMiniGames = hoverQuit = false;
                pressedContinue = pressedSetting = pressedStartGames = pressedMiniGames = pressedQuit = false;
                repaint();
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
                    // Slightly lift background brightness
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
                    // Brighter still so settings text reads clearly
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

            slider = new JSlider(0, 100, (int) (masterVolumeStatic * 100));
            slider.setPaintTicks(true);
            slider.setForeground(new Color(240, 240, 240));
            slider.setBackground(new Color(60, 60, 60));
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintLabels(true);
            slider.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
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
                    playVolumeTestDingAt(slider.getValue() / 100f);
            });

            JCheckBox muteMusicCheck = new JCheckBox("Mute all music", muteAllMusicStatic);
            muteMusicCheck.setForeground(new Color(250, 250, 240));
            muteMusicCheck.setOpaque(false);
            JCheckBox muteSfxCheck = new JCheckBox("Mute all sound effects", muteAllSoundEffectsStatic);
            muteSfxCheck.setForeground(new Color(250, 250, 240));
            muteSfxCheck.setOpaque(false);

            JPanel checkPanel = new JPanel(new GridLayout(2, 1, 4, 4));
            checkPanel.setOpaque(false);
            checkPanel.add(muteMusicCheck);
            checkPanel.add(muteSfxCheck);

            center.add(lbl, BorderLayout.NORTH);
            center.add(slider, BorderLayout.CENTER);
            center.add(checkPanel, BorderLayout.SOUTH);
            card.add(center, BorderLayout.CENTER);

            // btns panel: Apply (confirm volume + mute), Save (open save slots), Close
            JPanel btns = new JPanel();
            btns.setOpaque(false);
            JButton apply = new JButton("Apply");
            JButton saveGame = new JButton("Save");
            JButton close = new JButton("Close");
            Color btnBg = new Color(30, 144, 255);
            apply.setBackground(btnBg);
            apply.setForeground(Color.BLACK);
            saveGame.setBackground(btnBg);
            saveGame.setForeground(Color.BLACK);
            close.setBackground(new Color(200, 200, 200));
            close.setForeground(Color.BLACK);
            apply.setFocusPainted(false);
            saveGame.setFocusPainted(false);
            close.setFocusPainted(false);

            apply.addActionListener(ev -> {
                masterVolumeStatic = Math.max(0f, Math.min(1f, slider.getValue() / 100f));
                muteAllMusicStatic = muteMusicCheck.isSelected();
                muteAllSoundEffectsStatic = muteSfxCheck.isSelected();
                updateAllVolumes();
                dialog.dispose();
            });
            saveGame.addActionListener(ev -> {
                dialog.dispose();
                showSaveSlotDialog();
            });
            close.addActionListener(ev -> dialog.dispose());
            btns.add(apply);
            btns.add(saveGame);
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
        if (state == State.START_FADE_IN || state == State.DONE) {
            stopBackgroundMusic();
            playClickSound();
            StoryState st = GameState.getState();
            st.setChapterOneDialogueIndex(0);
            st.setChapterOneHistory(new java.util.ArrayList<>());
            st.setSavedChapter(1);
            st.setCurrentScene("chapter_one");
            state = State.START_GAME_FADEOUT;
            stateStart = System.currentTimeMillis();
            alphaStart = 1f;
            if (timer != null && !timer.isRunning())
                timer.start();
        }
    }

    private void onContinueClicked() {
        showLoadSlotDialog();
    }

    private void onMiniGamesClicked() {
        if (state == State.START_FADE_IN || state == State.DONE) {
            stopBackgroundMusic();
            playClickSound();
            if (onMiniGames != null)
                onMiniGames.run();
        }
    }

    private void onQuitClicked() {
        int choice = JOptionPane.showOptionDialog(this,
                "Are you sure you want to quit?",
                "Quit",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[] { "Quit", "Cancel" },
                "Cancel");
        if (choice == 0) // Quit
            System.exit(0);
    }

    private void initTimer() {
        stateStart = System.currentTimeMillis();
        timer = new javax.swing.Timer(TIMER_DELAY, e -> {
            long elapsed = System.currentTimeMillis() - stateStart;
            switch (state) {
                case JERRY_FADE_IN -> {
                    alphaJerry = Math.min(1f, elapsed / (float) FADE_MS);
                    if (alphaJerry >= 1f) {
                        state = State.JERRY_HOLD;
                        stateStart = System.currentTimeMillis();
                    }
                }
                case JERRY_HOLD -> {
                    alphaJerry = 1f;
                    if (elapsed >= HOLD_MS) {
                        state = State.JERRY_FADE_OUT;
                        stateStart = System.currentTimeMillis();
                    }
                }
                case JERRY_FADE_OUT -> {
                    alphaJerry = Math.max(0f, 1f - elapsed / (float) FADE_MS);
                    if (alphaJerry <= 0f) {
                        stopJerrySound();
                        state = State.START_FADE_IN;
                        stateStart = System.currentTimeMillis();
                        alphaStart = 0f;
                    }
                }
                case START_FADE_IN -> {
                    alphaStart = Math.min(1f, elapsed / (float) FADE_MS);
                    if (alphaStart >= 1f) {
                        state = State.DONE;
                        stateStart = System.currentTimeMillis();
                        startBackgroundMusicLoop();
                        timer.stop();
                    }
                }
                case DONE -> { }
                case START_GAME_FADEOUT -> {
                    alphaStart = Math.max(0f, 1f - elapsed / (float) START_GAME_FADEOUT_MS);
                    if (alphaStart <= 0f && onStartGame != null) {
                        onStartGame.run();
                    }
                }
                default -> { }
            }
            // Enforce BGM_MAX_PLAY_MS cap on the title BGM
            if (bgmClip != null && bgmStartTime > 0) {
                long bgmElapsed = System.currentTimeMillis() - bgmStartTime;
                if (bgmElapsed >= BGM_MAX_PLAY_MS) {
                    stopBackgroundMusic();
                }
            }
            // Update load-game fade overlay
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
        if ((state == State.START_FADE_IN || state == State.DONE || state == State.START_GAME_FADEOUT)
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

                // centered stacked button positions (Continue / Start Game / Mini Games / Settings / Quit)
                int btnW = (int) (sw * 0.4);
                int btnH = (int) (sh * 0.08);
                int spacing = (int) (sh * 0.025);

                int centerX = imgX + sw / 2;
                int firstBtnY = imgY + (int) (sh * 0.48);

                int continueBtnY = firstBtnY;
                int startBtnY = continueBtnY + btnH + spacing;
                int miniGamesBtnY = startBtnY + btnH + spacing;
                int settingBtnY = miniGamesBtnY + btnH + spacing;
                int quitBtnY = settingBtnY + btnH + spacing;

                int bx = centerX - btnW / 2;
                continueBounds.setBounds(bx, continueBtnY, btnW, btnH);
                startGamesBounds.setBounds(bx, startBtnY, btnW, btnH);
                miniGamesBounds.setBounds(bx, miniGamesBtnY, btnW, btnH);
                settingBounds.setBounds(bx, settingBtnY, btnW, btnH);
                quitBounds.setBounds(bx, quitBtnY, btnW, btnH);

                // draw CONTINUE button
                paintStyledButton(g2, continueBounds, "Continue", hoverContinue, pressedContinue);

                // draw START GAME button
                paintStyledButton(g2, startGamesBounds, "Start Game", hoverStartGames, pressedStartGames);

                // draw MINI GAMES button
                paintStyledButton(g2, miniGamesBounds, "Mini Games", hoverMiniGames, pressedMiniGames);

                // draw SETTINGS button
                paintStyledButton(g2, settingBounds, "Settings", hoverSetting, pressedSetting);

                // draw QUIT button
                paintStyledButton(g2, quitBounds, "Quit", hoverQuit, pressedQuit);

                // hover/pressed overlay handled inside paintStyledButton
            }
        }

        // Full-screen load fade veil
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

    @Override
    public void onEnter() {
        if (skipJerryNextEnter) {
            skipJerryNextEnter = false;
            state = State.DONE;
            stateStart = System.currentTimeMillis();
            alphaJerry = 0f;
            alphaStart = 1f;
            loadFadeActive = false;
            startBackgroundMusicLoop();
            if (timer != null && timer.isRunning())
                timer.stop();
        } else {
            state = State.JERRY_FADE_IN;
            stateStart = System.currentTimeMillis();
            playJerrySoundOnce();
            alphaJerry = 0f;
            alphaStart = 0f;
            loadFadeActive = false;
            if (timer != null && !timer.isRunning())
                timer.start();
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (timer != null && timer.isRunning())
            timer.stop();
        // Stop and release clips when leaving the title
        stopJerrySound();
        stopBackgroundMusic();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    private void initKey() {
        setFocusable(true);
    }

    /**
     * Plays the Jerry entrance sting once.
     */
    private void playJerrySoundOnce() {
        // Stop any previous Jerry clip before reopening
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
        }
    }

    /**
     * Stops the Jerry sting and releases its clip.
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
     * Loads SFX from classpath /sound/ or the sound/ working directory.
     */
    private Clip loadSoundClip(String fileName) {
        try {
            // 1) Classpath (e.g. /sound/ inside a jar)
            URL url = getClass().getResource("/sound/" + fileName);
            AudioInputStream ais;
            if (url != null) {
                ais = AudioSystem.getAudioInputStream(url);
            } else {
                // 2) Filesystem ./sound/
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
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
            System.err.println("Failed to load sound: " + fileName + " - " + ex.getMessage());
            return null;
        } finally {
            // Do not close ais here; the clip owns the stream
        }
    }

    /**
     * Starts looping title BGM, capped at BGM_MAX_PLAY_MS.
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
        }
    }

    /**
     * Stops title BGM and releases the clip.
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
     * Loads title BGM from classpath /music/ or ./music/.
     */
    private Clip loadMusicClip(String fileName) {
        try {
            // 1) Classpath /music/
            URL url = getClass().getResource("/music/" + fileName);
            AudioInputStream ais;
            if (url != null) {
                ais = AudioSystem.getAudioInputStream(url);
            } else {
                // 2) Filesystem ./music/
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
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
            System.err.println("Failed to load music: " + fileName + " - " + ex.getMessage());
            return null;
        } finally {
            // Do not close ais here; the clip owns the stream
        }
    }

    /**
     * Apply current master volume and mute flags to a Clip. isMusic: true = music (BGM), false = sound effect.
     */
    private void applyVolumeToClip(Clip clip, boolean isMusic) {
        if (clip == null) return;
        float effective = masterVolumeStatic * (isMusic ? (muteAllMusicStatic ? 0f : 1f) : (muteAllSoundEffectsStatic ? 0f : 1f));
        applyVolumeToClip(clip, effective);
    }

    /**
     * Applies a one-off volume to a clip (does not touch masterVolumeStatic); used for ding preview.
     */
    private void applyVolumeToClip(Clip clip, float volume) {
        if (clip == null)
            return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float v = Math.max(0f, Math.min(1f, volume));
                if (v == 0f) {
                    gain.setValue(gain.getMinimum());
                } else {
                    float dB = (float) (20.0 * Math.log10(v));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Applies current mute/volume rules to an existing clip (defaults to SFX if isMusic is false).
     */
    private void applyVolumeToClip(Clip clip) {
        applyVolumeToClip(clip, false);
    }

    /**
     * Apply current master volume and mute to a clip from any scene (Settings/menu volume).
     * Call after loading and when user applies Settings.
     */
    public static void applyVolumeToClipForScene(Clip clip, boolean isMusic) {
        applyVolumeToClipForScene(clip, isMusic, 1f);
    }

    /** Apply scene volume without the low-volume floor cutoff. */
    public static void applyVolumeToClipForSceneNoFloor(Clip clip, boolean isMusic) {
        if (clip == null) return;
        float effective = masterVolumeStatic * (isMusic ? (muteAllMusicStatic ? 0f : 1f) : (muteAllSoundEffectsStatic ? 0f : 1f));
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float v = Math.max(0f, Math.min(1f, effective));
                if (v == 0f) gain.setValue(gain.getMinimum());
                else {
                    float dB = (float) (20.0 * Math.log10(v));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) {}
    }

    /**
     * Same as above with a volume scale (0..1]. When scale < 1 the clip is quieter.
     * When master volume is below 0.5 (50%), effective volume is forced to 0 (for subtle ambience that disappears at low volume).
     */
    public static void applyVolumeToClipForScene(Clip clip, boolean isMusic, float volumeScale) {
        if (clip == null) return;
        float base = masterVolumeStatic * (isMusic ? (muteAllMusicStatic ? 0f : 1f) : (muteAllSoundEffectsStatic ? 0f : 1f));
        float effective = (base < 0.5f) ? 0f : (base * Math.max(0f, Math.min(1f, volumeScale)));
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float v = Math.max(0f, Math.min(1f, effective));
                if (v == 0f) gain.setValue(gain.getMinimum());
                else {
                    float dB = (float) (20.0 * Math.log10(v));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) {}
    }

    /**
     * Load a music clip from music/ (classpath /music/ or file music/). Tries fileName as-is then .wav.
     * For use by other scenes. Caller must loop and start; apply volume with applyVolumeToClipForScene.
     */
    public static Clip loadMusicFromMusicDir(String fileName) {
        String[] names = fileName.contains(".") ? new String[]{fileName} : new String[]{fileName + ".mp3", fileName + ".wav"};
        for (String name : names) {
            try {
                URL url = StartScene.class.getResource("/music/" + name);
                AudioInputStream ais = null;
                if (url != null) ais = AudioSystem.getAudioInputStream(url);
                if (ais == null) {
                    File f = new File("music/" + name);
                    if (f.exists()) ais = AudioSystem.getAudioInputStream(f);
                }
                if (ais != null) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    return clip;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Reapplies mute/volume to every clip owned by this scene.
     */
    private void updateAllVolumes() {
        applyVolumeToClip(jerryClip, false);  // sound effect
        applyVolumeToClip(bgmClip, true);     // music
    }

    /** Static helper for {@link #playVolumeTestDingAt(float)} gain staging. */
    private static void applyVolumeToClipStatic(Clip clip, float volume) {
        if (clip == null) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float v = Math.max(0f, Math.min(1f, volume));
                if (v == 0f) gain.setValue(gain.getMinimum());
                else {
                    float dB = (float) (20.0 * Math.log10(v));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) { }
    }

    /**
     * Plays ding.wav once at the given volume without mutating masterVolume (settings preview).
     */
    public static void playVolumeTestDingAt(float volume) {
        try {
            URL url = StartScene.class.getResource("/sound/ding.wav");
            AudioInputStream ais = null;
            if (url != null)
                ais = AudioSystem.getAudioInputStream(url);
            else {
                File f = new File("sound/ding.wav");
                if (f.exists())
                    ais = AudioSystem.getAudioInputStream(f);
            }
            if (ais == null) return;
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            applyVolumeToClipStatic(clip, volume);
            Clip c = clip;
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP)
                    c.close();
            });
            clip.start();
        } catch (Exception ex) {
            System.err.println("Failed to play ding: " + ex.getMessage());
        }
    }

    private static final int SAVE_SLOT_COUNT = 8;

    private void showSaveSlotDialog() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Save",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= SAVE_SLOT_COUNT; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                performSave(slot);
                dialog.dispose();
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
                StoryState st = GameState.getState();
                // Skip opening quote + in-chapter CG dialogue; jump to title sticker / post-chapter flow (same as end of Ch.1).
                st.setChapterOneDialogueIndex(ChapterOneData.LINES.length);
                st.setChapterOneHistory(new java.util.ArrayList<>());
                st.setSavedChapter(1);
                st.setCurrentScene("chapter_one");
                dialog.dispose();
                if (onStartGame != null) onStartGame.run();
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

    private void showLoadSlotDialog() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Load",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= SAVE_SLOT_COUNT; i++) {
            int slot = i;
            int ch = SaveLoad.getSavedChapter("saves/slot" + slot + ".dat");
            String label = ch == 0 ? "Slot " + slot : "Slot " + slot + " (Ch." + ch + ")";
            JButton b = new JButton(label);
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
                if (choice == 0) {
                    dialog.dispose();
                    performLoad(slot);
                } else if (choice == 1) {
                    deleteSaveSlot(slot);
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
                StoryState st = GameState.getState();
                // Continue → Chapter One: skip quote + all Ch.1 dialogue; start at title.png / post-chapter sequence.
                st.setChapterOneDialogueIndex(ChapterOneData.LINES.length);
                st.setChapterOneHistory(new java.util.ArrayList<>());
                st.setSavedChapter(1);
                st.setCurrentScene("chapter_one");
                dialog.dispose();
                if (onStartGame != null) onStartGame.run();
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

    private void performSave(int slot) {
        try {
            StoryState state = GameState.getState();
            state.setSavedChapter(1);
            String path = "saves/slot" + slot + ".dat";
            SaveLoad.save(state, path);
            JOptionPane.showMessageDialog(this, "Saved to slot " + slot);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
            int ch = loaded.getSavedChapter();
            if (ch == 2) {
                JOptionPane.showMessageDialog(this, "Chapter 2 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ch == 3) {
                JOptionPane.showMessageDialog(this, "Chapter 3 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                return;
            }
            GameState.setState(loaded);
            loadFadeActive = true;
            loadFadeStartTime = System.currentTimeMillis();
            String sceneKey = loaded.getCurrentScene();
            if (sceneKey == null || sceneKey.isEmpty()) {
                if (loaded.getTrollCaveDialogueIndex() > 0 || !loaded.getTrollCaveHistory().isEmpty()) {
                    sceneKey = "troll_cave";
                } else if (loaded.hasCompletedTrollCaveAndChoseDoge()) {
                    sceneKey = "forest_overworld_map";
                } else if (loaded.getChapterOneDialogueIndex() >= ChapterOneData.LINES.length) {
                    // Old saves without sceneKey: chapter one finished usually means forest start area.
                    sceneKey = "forest_entrance";
                } else {
                    sceneKey = "chapter_one";
                }
                loaded.setCurrentScene(sceneKey);
            }
            if (sceneKey == null || sceneKey.isEmpty() || "chapter_one".equals(sceneKey)) {
                ChapterOneScene.setSkipQuoteNextEnter(true);
            }
            if (onStartGame != null)
                onStartGame.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
     * Volume test uses {@link #playVolumeTestDingAt(float)} with {@link #masterVolumeStatic}; no instance helper.
     */

    /** UI click SFX: try click.wav, else ding.wav */
    private void playClickSound() {
        Clip clip = loadSoundClip(CLICK_SOUND_NAME);
        if (clip == null)
            clip = loadSoundClip(DING_SOUND_NAME);
        if (clip == null) return;
        applyVolumeToClip(clip);
        final Clip toClose = clip;
        clip.addLineListener(event -> {
            if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                toClose.close();
            }
        });
        clip.start();
    }
}


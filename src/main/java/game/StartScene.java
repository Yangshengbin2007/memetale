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

    // button interaction state
    private boolean hoverSetting = false;
    private boolean hoverStartGames = false;
    private boolean pressedSetting = false;
    private boolean pressedStartGames = false;
    private Rectangle settingBounds = new Rectangle();
    private Rectangle startGamesBounds = new Rectangle();

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
    }

    private void loadImages() {
        // keep jerry scaled to a reasonable size (use ImageIcon to get real dimensions)
        jerryImg = loadAndScaleCandidates("jerry.png", 300, 300);
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
                boolean oldHoverSetting = hoverSetting;
                boolean oldHoverStart = hoverStartGames;
                hoverSetting = settingBounds.contains(p);
                hoverStartGames = startGamesBounds.contains(p);
                if (hoverSetting != oldHoverSetting || hoverStartGames != oldHoverStart)
                    repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (settingBounds.contains(p)) {
                    pressedSetting = true;
                    repaint();
                } else if (startGamesBounds.contains(p)) {
                    pressedStartGames = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                if (pressedSetting && settingBounds.contains(p)) {
                    pressedSetting = false;
                    onSettingClicked();
                } else if (pressedStartGames && startGamesBounds.contains(p)) {
                    pressedStartGames = false;
                    onStartGamesClicked();
                } else {
                    pressedSetting = false;
                    pressedStartGames = false;
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverSetting = hoverStartGames = false;
                pressedSetting = pressedStartGames = false;
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
                    // darkened translucent background to keep style consistent
                    g2.setColor(new Color(10, 10, 10, 220));
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
                    g2.setColor(new Color(30, 30, 30, 240));
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
            lbl.setForeground(new Color(230, 230, 210));

            slider = new JSlider(0, 100, 50);
            slider.setPaintTicks(true);
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintLabels(true);

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
            apply.setForeground(Color.WHITE);
            close.setBackground(new Color(80, 80, 80));
            close.setForeground(Color.WHITE);
            apply.setFocusPainted(false);
            close.setFocusPainted(false);

            // Apply action: apply settings (e.g. volume) but DO NOT save game state here
            apply.addActionListener(ev -> {
                int vol = slider.getValue();
                System.out.println("Applied volume: " + vol);

                JOptionPane.showMessageDialog(this, "Settings applied (temporary)");
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
            // ensure gameCg loaded
            if (gameCgImg == null) {
                gameCgImg = loadImageCandidatesRaw("gamecg1.jpg");
                if (gameCgImg == null) {
                    System.err.println("gamecg1.jpg not found; transition will still fade out start.");
                }
            }
        }
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

                // centered stacked button positions (Start Game on top, Settings below)
                int btnW = (int) (sw * 0.5); // button width ~50% of image width
                int btnH = (int) (sh * 0.11); // button height ~11% of image height
                int spacing = (int) (sh * 0.035); // vertical spacing between buttons

                int centerX = imgX + sw / 2;
                // move buttons slightly down on the start image (was 0.48)
                int topBtnY = imgY + (int) (sh * 0.55);
                int startBtnY = topBtnY - btnH / 2;
                int settingBtnY = startBtnY + btnH + spacing;

                int bx = centerX - btnW / 2;
                startGamesBounds.setBounds(bx, startBtnY, btnW, btnH); // Start Game (upper)
                settingBounds.setBounds(bx, settingBtnY, btnW, btnH); // Settings (lower)

                // draw START GAME button - stylized (blue + gold frame)
                paintStyledButton(g2, startGamesBounds, "Start Game", hoverStartGames, pressedStartGames);

                // draw SETTINGS button - stylized (blue + gold frame but darker)
                paintStyledButton(g2, settingBounds, "Settings", hoverSetting, pressedSetting);

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
        // reset state and start animation
        state = State.JERRY_FADE_IN;
        stateStart = System.currentTimeMillis();
        alphaJerry = 0f;
        alphaStart = 0f;
        if (timer != null && !timer.isRunning())
            timer.start();
    }

    @Override
    public void onExit() {
        if (timer != null && timer.isRunning())
            timer.stop();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

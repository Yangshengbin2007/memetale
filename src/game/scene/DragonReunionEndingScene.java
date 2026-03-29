package game.scene;

import javax.swing.*;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

/**
 * Chapter 3 epilogue: English dialogue on {@code goodending} art, {@code cg2.wav}, then credits with {@code rickroll.wav},
 * then optional extra lines with no music, then return to title. Space advances; hold Space to fast-skip.
 */
public class DragonReunionEndingScene extends JPanel implements Scene {
    private static final int PHASE_MAIN = 0;
    private static final int PHASE_CREDITS = 1;
    private static final int PHASE_EXTRA = 2;
    private static final int PHASE_FINAL = 3;

    private static final int SPACE_HOLD_MS = 2800;
    private static final int FAST_SKIP_MS = 90;

    private final Runnable onCompleteToTitle;

    private Image backgroundImage;
    private Clip mainBgmClip;
    private Clip creditsBgmClip;

    private int phase = PHASE_MAIN;
    private int mainLineIndex;
    private int creditPageIndex;
    private int extraLineIndex;

    private boolean spaceDown;
    private boolean fastSkipFromHold;
    private javax.swing.Timer holdSpaceTimer;
    private javax.swing.Timer fastSkipTimer;

    public DragonReunionEndingScene(Runnable onCompleteToTitle) {
        this.onCompleteToTitle = onCompleteToTitle;
        setBackground(Color.BLACK);
        setFocusable(true);
        loadBackground();
        initTimers();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !spaceDown) {
                    spaceDown = true;
                    fastSkipFromHold = false;
                    holdSpaceTimer.restart();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
                holdSpaceTimer.stop();
                fastSkipTimer.stop();
                if (!fastSkipFromHold) advanceOneStep();
                fastSkipFromHold = false;
                spaceDown = false;
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                advanceOneStep();
            }
        });
    }

    private void initTimers() {
        holdSpaceTimer = new javax.swing.Timer(SPACE_HOLD_MS, e -> {
            if (spaceDown) {
                fastSkipFromHold = true;
                fastSkipTimer.start();
            }
        });
        holdSpaceTimer.setRepeats(false);
        fastSkipTimer = new javax.swing.Timer(FAST_SKIP_MS, e -> advanceOneStep());
    }

    private void loadBackground() {
        String[] names = {"goodending.cg", "goodending.jpg", "goodending.png", "goodending.jpeg"};
        for (String n : names) {
            Image img = tryLoadImage("/image/Chapter one/" + n, "image/Chapter one/" + n);
            if (img != null) {
                backgroundImage = img;
                return;
            }
        }
        backgroundImage = null;
    }

    private static Image tryLoadImage(String classpath, String filePath) {
        URL url = DragonReunionEndingScene.class.getResource(classpath);
        if (url != null) return new ImageIcon(url).getImage();
        File f = new File(filePath);
        if (f.exists()) return new ImageIcon(f.getAbsolutePath()).getImage();
        return null;
    }

    private void advanceOneStep() {
        if (phase == PHASE_MAIN) {
            if (mainLineIndex + 1 < DragonReunionEndingData.MAIN_LINES.length) {
                mainLineIndex++;
            } else {
                startCreditsPhase();
            }
        } else if (phase == PHASE_CREDITS) {
            if (creditPageIndex + 1 < DragonReunionEndingData.CREDIT_PAGES.length) {
                creditPageIndex++;
            } else {
                startExtraPhase();
            }
        } else if (phase == PHASE_EXTRA) {
            if (extraLineIndex + 1 < DragonReunionEndingData.EXTRA_LINES.length) {
                extraLineIndex++;
            } else {
                phase = PHASE_FINAL;
            }
        } else if (phase == PHASE_FINAL) {
            finishToTitle();
        }
        repaint();
    }

    private void startCreditsPhase() {
        stopMainBgm();
        phase = PHASE_CREDITS;
        creditPageIndex = 0;
        creditsBgmClip = StartScene.loadMusicFromMusicDir("rickroll.wav");
        if (creditsBgmClip == null) creditsBgmClip = StartScene.loadMusicFromMusicDir("rickroll.mp3");
        if (creditsBgmClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(creditsBgmClip, true);
            creditsBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            creditsBgmClip.start();
        }
    }

    private void startExtraPhase() {
        stopCreditsBgm();
        phase = PHASE_EXTRA;
        extraLineIndex = 0;
    }

    private void stopMainBgm() {
        if (mainBgmClip != null) {
            try {
                if (mainBgmClip.isRunning()) mainBgmClip.stop();
                mainBgmClip.close();
            } catch (Exception ignore) {}
            mainBgmClip = null;
        }
    }

    private void stopCreditsBgm() {
        if (creditsBgmClip != null) {
            try {
                if (creditsBgmClip.isRunning()) creditsBgmClip.stop();
                creditsBgmClip.close();
            } catch (Exception ignore) {}
            creditsBgmClip = null;
        }
    }

    private void finishToTitle() {
        stopMainBgm();
        stopCreditsBgm();
        if (onCompleteToTitle != null) onCompleteToTitle.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        if (backgroundImage != null) {
            int iw = backgroundImage.getWidth(this);
            int ih = backgroundImage.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(backgroundImage, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(25, 35, 55));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(180, 190, 220));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            String hint = "Place goodending.jpg / goodending.png (or goodending.cg) in image/Chapter one/";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, h / 2);
        }

        if (phase == PHASE_MAIN) {
            drawMainDialogue(g2, w, h);
        } else if (phase == PHASE_CREDITS) {
            drawCredits(g2, w, h);
        } else if (phase == PHASE_EXTRA) {
            drawExtra(g2, w, h);
        } else {
            drawFinalPrompt(g2, w, h);
        }

        g2.dispose();
    }

    private void drawMainDialogue(Graphics2D g2, int w, int h) {
        if (mainLineIndex >= DragonReunionEndingData.MAIN_LINES.length) return;
        String[] row = DragonReunionEndingData.MAIN_LINES[mainLineIndex];
        String who = row[0];
        String text = row[1];

        int boxH = (int) (h * 0.28);
        int boxY = h - boxH;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        g2.setColor(new Color(12, 14, 22));
        g2.fillRoundRect(12, boxY - 8, w - 24, boxH + 8, 14, 14);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 170, 90));
        g2.drawRoundRect(12, boxY - 8, w - 24, boxH + 8, 14, 14);

        int pad = 18;
        int y = boxY + pad;
        if (!who.isEmpty()) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 17f));
            g2.setColor(new Color(230, 200, 120));
            g2.drawString(who, pad + 8, y + g2.getFontMetrics().getAscent());
            y += 26;
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.setColor(new Color(238, 238, 235));
        drawWrapped(g2, text, pad + 8, y, w - 2 * pad - 16, 22);

        g2.setColor(new Color(130, 130, 130));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Space / Click — advance   ·   Hold Space — fast skip", pad + 8, h - 14);
    }

    private void drawCredits(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.94f));
        g2.setColor(new Color(8, 8, 12));
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        String[] page = DragonReunionEndingData.CREDIT_PAGES[creditPageIndex];
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15f));
        int y = 48;
        for (String line : page) {
            if (line.isEmpty()) {
                y += 12;
                continue;
            }
            g2.setColor(line.startsWith("•") || line.startsWith("Gameplay") || line.startsWith("Meme")
                || line.startsWith("Music") || line.startsWith("Art") || line.startsWith("Development")
                || line.startsWith("Legal") || line.startsWith("References")
                ? new Color(220, 190, 120) : new Color(230, 230, 228));
            if (line.startsWith("Chapter Complete") || line.startsWith("But the journey"))
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            else
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15f));
            y = drawWrapped(g2, line, 32, y, w - 64, 20) + 6;
        }
        g2.setColor(new Color(140, 140, 150));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Space / Click — next   ·   Rickroll.wav", 24, h - 16);
    }

    private void drawExtra(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        if (extraLineIndex >= DragonReunionEndingData.EXTRA_LINES.length) return;
        String[] row = DragonReunionEndingData.EXTRA_LINES[extraLineIndex];
        int boxY = h / 2 - 40;
        g2.setColor(new Color(240, 240, 235));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.drawString(row[0], 40, boxY);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 17f));
        drawWrapped(g2, row[1], 40, boxY + 28, w - 80, 24);
        g2.setColor(new Color(120, 120, 130));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("No music — Space / Click", 40, h - 24);
    }

    private void drawFinalPrompt(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(220, 220, 230));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        String a = "Chapter complete.";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(a, (w - fm.stringWidth(a)) / 2, h / 2 - 20);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        String b = "Press Space or click to return to the title.";
        fm = g2.getFontMetrics();
        g2.drawString(b, (w - fm.stringWidth(b)) / 2, h / 2 + 16);
    }

    /** @return next y below wrapped block */
    private static int drawWrapped(Graphics2D g2, String text, int x, int y, int maxW, int lineH) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : words) {
            String trial = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(trial) > maxW && line.length() > 0) {
                g2.drawString(line.toString(), x, cy + fm.getAscent());
                cy += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, cy + fm.getAscent());
            cy += lineH;
        }
        return cy;
    }

    @Override
    public void onEnter() {
        phase = PHASE_MAIN;
        mainLineIndex = 0;
        creditPageIndex = 0;
        extraLineIndex = 0;
        spaceDown = false;
        fastSkipFromHold = false;
        mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.wav");
        if (mainBgmClip == null) mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.mp3");
        if (mainBgmClip != null) {
            StartScene.applyVolumeToClipForSceneNoFloor(mainBgmClip, true);
            mainBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            mainBgmClip.start();
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        holdSpaceTimer.stop();
        fastSkipTimer.stop();
        stopMainBgm();
        stopCreditsBgm();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

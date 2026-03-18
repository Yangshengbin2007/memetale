package game.scene;

import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Undertale-style bullet-hell: battle box, red heart, arrow keys.
 * Theme: Internet / Cancel culture. Bullets: "CANCELLED", glitch squares.
 * Normal mode: 3 phases (Cancel Warm-up, Public Pressure, Final Judgment).
 * Hell mode: same structure, harder (more bullets, delays, fake attacks).
 * All text in English.
 */
public class TrollBattleScene extends JPanel implements Scene {
    private final boolean hellMode;
    private final Runnable onVictory;
    private final Runnable onQuitToTitle;

    private static final int BOX_MARGIN_X = 80;
    private static final int BOX_MARGIN_TOP = 60;
    private static final int BOX_MARGIN_BOTTOM = 120;
    private static final int HEART_SIZE = 14;
    private static final int MAX_HP = 20;
    private static final int BULLET_SIZE = 24;
    private static final double NORMAL_BOX_SCALE = 1.0;
    private static final double SHRINK_BOX_SCALE = 0.7;
    private static final int SHAKE_DURATION_MS = 200;
    private static final int GOLDEN_ITEM_SIZE = 20;
    private static final String[] TAUNTS = {
        "Skill issue.", "Cope.", "Ratio.", "Touch grass.", "Your take is bad.",
        "Cancelled.", "Uninstall.", "Cringe.", "Nobody asked."
    };

    private int boxX, boxY, boxW, boxH;
    private double currentBoxScale = 1.0;
    private int phase = 1;
    private int waveInPhase = 0;
    private long phaseStartTime;
    private long battleStartTime;
    private final BattlePlayer player = new BattlePlayer();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<GoldenItem> goldenItems = new ArrayList<>();
    private int hp = MAX_HP;
    private long shakeUntil;
    private int shakeOffsetX, shakeOffsetY;
    private String dialogueText = "";
    private long dialogueUntil;
    private String tauntText = "";
    private long tauntUntil;
    private boolean victory;
    private boolean goldenItemSpawned;
    private boolean goldenItemCaught;
    private final Random rnd = new Random();
    private javax.swing.Timer gameTimer;
    private boolean up, down, left, right;
    private static final int TICK_MS = 16;
    private Clip battleMusicClip;
    private int lastMusicPhase = 0;
    /** Hell mode only: very quiet looping ambience ('.mp3), volume below 50% = silent. */
    private Clip hellAmbienceClip;

    public TrollBattleScene(boolean hellMode, Runnable onVictory, Runnable onQuitToTitle) {
        this.hellMode = hellMode;
        this.onVictory = onVictory;
        this.onQuitToTitle = onQuitToTitle;
        setBackground(new Color(15, 15, 25));
        setFocusable(true);
        initKey();
    }

    private void initKey() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    up = true; e.consume(); break;
                    case KeyEvent.VK_DOWN:  down = true; e.consume(); break;
                    case KeyEvent.VK_LEFT:  left = true; e.consume(); break;
                    case KeyEvent.VK_RIGHT: right = true; e.consume(); break;
                    case KeyEvent.VK_ESCAPE:
                        if (onQuitToTitle != null) {
                            int c = JOptionPane.showConfirmDialog(TrollBattleScene.this, "Quit to title?", "Quit", JOptionPane.YES_NO_OPTION);
                            if (c == JOptionPane.YES_OPTION) onQuitToTitle.run();
                        }
                        break;
                    default: break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    up = false; e.consume(); break;
                    case KeyEvent.VK_DOWN:  down = false; e.consume(); break;
                    case KeyEvent.VK_LEFT:  left = false; e.consume(); break;
                    case KeyEvent.VK_RIGHT: right = false; e.consume(); break;
                    default: break;
                }
            }
        });
    }

    private void updateBoxBounds() {
        int w = getWidth(), h = getHeight();
        int innerW = w - BOX_MARGIN_X * 2;
        int innerH = h - BOX_MARGIN_TOP - BOX_MARGIN_BOTTOM;
        boxW = (int) (innerW * currentBoxScale);
        boxH = (int) (innerH * currentBoxScale);
        boxX = (w - boxW) / 2;
        boxY = BOX_MARGIN_TOP + (innerH - boxH) / 2;
    }

    @Override
    public void onEnter() {
        updateBoxBounds();
        player.x = boxX + boxW / 2 - HEART_SIZE / 2;
        player.y = boxY + boxH / 2 - HEART_SIZE / 2;
        phase = 1;
        waveInPhase = 0;
        hp = MAX_HP;
        bullets.clear();
        goldenItems.clear();
        victory = false;
        goldenItemSpawned = false;
        goldenItemCaught = false;
        currentBoxScale = NORMAL_BOX_SCALE;
        battleStartTime = System.currentTimeMillis();
        phaseStartTime = battleStartTime;
        showDialogue(hellMode ? "Let's start easy." : "Let's start easy.");
        showTaunt();
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        gameTimer.start();
        startBattleMusicForPhase(1);
        lastMusicPhase = 1;
        if (hellMode) {
            if (hellAmbienceClip != null) {
                try { if (hellAmbienceClip.isRunning()) hellAmbienceClip.stop(); hellAmbienceClip.close(); } catch (Exception ignore) {}
                hellAmbienceClip = null;
            }
            hellAmbienceClip = StartScene.loadMusicFromMusicDir("'.mp3");
            if (hellAmbienceClip == null) hellAmbienceClip = StartScene.loadMusicFromMusicDir("hell_ambience.mp3");
            if (hellAmbienceClip != null) {
                StartScene.applyVolumeToClipForScene(hellAmbienceClip, true, 0.18f);
                hellAmbienceClip.loop(Clip.LOOP_CONTINUOUSLY);
                hellAmbienceClip.start();
            }
        }
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }
        if (battleMusicClip != null) {
            try { if (battleMusicClip.isRunning()) battleMusicClip.stop(); battleMusicClip.close(); } catch (Exception ignore) {}
            battleMusicClip = null;
        }
        if (hellAmbienceClip != null) {
            try { if (hellAmbienceClip.isRunning()) hellAmbienceClip.stop(); hellAmbienceClip.close(); } catch (Exception ignore) {}
            hellAmbienceClip = null;
        }
        lastMusicPhase = 0;
    }

    /** Normal: phase1=1x fighttroll.mp3, phase2=1.2x fighttroll_12.mp3, phase3=1.5x fighttroll_15.mp3. Hell: phase2=0.75x fighttroll_075.mp3, phase3=0.5x fighttroll_05.mp3. */
    private void startBattleMusicForPhase(int p) {
        if (p == lastMusicPhase) return;
        String file = "fighttroll.mp3";
        if (p == 1) file = "fighttroll.mp3";
        else if (p == 2) file = hellMode ? "fighttroll_075.mp3" : "fighttroll_12.mp3";
        else if (p == 3) file = hellMode ? "fighttroll_05.mp3" : "fighttroll_15.mp3";
        Clip next = StartScene.loadMusicFromMusicDir(file);
        if (next == null && p != 1) next = StartScene.loadMusicFromMusicDir("fighttroll.mp3");
        if (next != null) {
            if (battleMusicClip != null) {
                try { if (battleMusicClip.isRunning()) battleMusicClip.stop(); battleMusicClip.close(); } catch (Exception ignore) {}
                battleMusicClip = null;
            }
            battleMusicClip = next;
            StartScene.applyVolumeToClipForScene(battleMusicClip, true);
            battleMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            battleMusicClip.start();
            lastMusicPhase = p;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    private void showDialogue(String text) {
        dialogueText = text;
        dialogueUntil = System.currentTimeMillis() + 2500;
    }

    private void showTaunt() {
        tauntText = TAUNTS[rnd.nextInt(TAUNTS.length)];
        tauntUntil = System.currentTimeMillis() + 1500;
    }

    private void tick() {
        if (victory) return;
        long now = System.currentTimeMillis();
        updateBoxBounds();

        if (up) player.y = Math.max(boxY, player.y - 4);
        if (down) player.y = Math.min(boxY + boxH - HEART_SIZE, player.y + 4);
        if (left) player.x = Math.max(boxX, player.x - 4);
        if (right) player.x = Math.min(boxX + boxW - HEART_SIZE, player.x + 4);

        spawnBullets(now);
        if (phase != lastMusicPhase) startBattleMusicForPhase(phase);
        if (phase == 3 && !goldenItemSpawned && waveInPhase >= 2) {
            spawnGoldenItem();
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.x += b.vx;
            b.y += b.vy;
            if (b.x + b.w < boxX || b.x > boxX + boxW || b.y + b.h < boxY || b.y > boxY + boxH) {
                if (b.bounce) {
                    if (b.x + b.w < boxX) { b.x = boxX; b.vx = -b.vx; }
                    if (b.x > boxX + boxW) { b.x = boxX + boxW - b.w; b.vx = -b.vx; }
                    if (b.y + b.h < boxY) { b.y = boxY; b.vy = -b.vy; }
                    if (b.y > boxY + boxH) { b.y = boxY + boxH - b.h; b.vy = -b.vy; }
                } else {
                    bullets.remove(i);
                }
                continue;
            }
            if (intersects(player.x, player.y, HEART_SIZE, HEART_SIZE, (int)b.x, (int)b.y, b.w, b.h)) {
                hp--;
                bullets.remove(i);
                shakeUntil = now + SHAKE_DURATION_MS;
                if (hp <= 0) {
                    hp = 0;
                    victory = false;
                    if (onQuitToTitle != null) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "You got cancelled. Try again.");
                            onQuitToTitle.run();
                        });
                    }
                    if (gameTimer != null) gameTimer.stop();
                    return;
                }
                showDialogue("Ow. That was a bad take.");
            }
        }

        for (int i = goldenItems.size() - 1; i >= 0; i--) {
            GoldenItem g = goldenItems.get(i);
            g.x += g.vx;
            g.y += g.vy;
            if (intersects(player.x, player.y, HEART_SIZE, HEART_SIZE, (int)g.x, (int)g.y, GOLDEN_ITEM_SIZE, GOLDEN_ITEM_SIZE)) {
                goldenItemCaught = true;
                goldenItems.remove(i);
            } else if (g.x < boxX - 50 || g.x > boxX + boxW + 50) {
                goldenItems.remove(i);
            }
        }

        advancePhaseAndWaves(now);
        repaint();
    }

    private boolean intersects(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    private void spawnGoldenItem() {
        goldenItemSpawned = true;
        GoldenItem g = new GoldenItem();
        g.x = boxX + (rnd.nextBoolean() ? -20 : boxW + 20);
        g.y = boxY + boxH / 2 - GOLDEN_ITEM_SIZE / 2;
        g.vx = (g.x < boxX + boxW / 2) ? 2.5 : -2.5;
        g.vy = (rnd.nextBoolean() ? 1 : -1) * 1.2;
        goldenItems.add(g);
    }

    private void spawnBullets(long now) {
        long elapsed = (now - phaseStartTime);
        long totalBattle = (now - battleStartTime);

        if (phase == 1) {
            if (waveInPhase == 0 && elapsed > 500) {
                waveInPhase = 1;
                showDialogue(hellMode ? "Let's start easy." : "Let's start easy.");
            }
            if (waveInPhase == 1) {
                if ((int)(elapsed / 800) > (int)((elapsed - TICK_MS) / 800)) {
                    for (int i = 0; i < (hellMode ? 5 : 3); i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.vx = 0;
                        b.vy = hellMode ? 1.8f : 1.2f;
                        b.text = "CANCEL";
                        bullets.add(b);
                    }
                }
                if (elapsed > 5000) { waveInPhase = 2; showDialogue("Oh great, now it's opinions from both sides."); }
            }
            if (waveInPhase == 2) {
                if ((int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000) && elapsed > 5500) {
                    boolean leftToRight = rnd.nextBoolean();
                    int row = boxY + 20 + rnd.nextInt(Math.max(1, boxH - 80));
                    for (int col = 0; col < boxW; col += (hellMode ? 35 : 45)) {
                        Bullet b = new Bullet();
                        b.w = 18;
                        b.h = 8;
                        b.y = row;
                        b.vy = 0;
                        if (leftToRight) { b.x = boxX - 20; b.vx = hellMode ? 3f : 2.2f; }
                        else { b.x = boxX + boxW + 20; b.vx = hellMode ? -3f : -2.2f; }
                        b.text = "";
                        bullets.add(b);
                    }
                }
                if (elapsed > 10000) { waveInPhase = 3; showDialogue("Okay this is getting harder."); }
            }
            if (waveInPhase == 3) {
                if ((int)(elapsed / 700) > (int)((elapsed - TICK_MS) / 700) && elapsed > 10500) {
                    int corner = rnd.nextInt(4);
                    double angle = corner * Math.PI / 2 + 0.3 + rnd.nextDouble() * 0.4;
                    double spd = hellMode ? 2.2 : 1.6;
                    Bullet b = new Bullet();
                    if (corner == 0) { b.x = boxX; b.y = boxY; }
                    else if (corner == 1) { b.x = boxX + boxW - BULLET_SIZE; b.y = boxY; }
                    else if (corner == 2) { b.x = boxX + boxW - BULLET_SIZE; b.y = boxY + boxH - BULLET_SIZE; }
                    else { b.x = boxX; b.y = boxY + boxH - BULLET_SIZE; }
                    b.w = BULLET_SIZE;
                    b.h = BULLET_SIZE;
                    b.vx = (float)(Math.cos(angle) * spd);
                    b.vy = (float)(Math.sin(angle) * spd);
                    b.text = "C";
                    bullets.add(b);
                }
                if (elapsed > 15000) {
                    phase = 2;
                    phaseStartTime = now;
                    waveInPhase = 0;
                    showDialogue("You survived… the tutorial.");
                }
            }
        } else if (phase == 2) {
            if (waveInPhase == 0 && elapsed > 400) { waveInPhase = 1; showDialogue("Let's see how you handle pressure."); }
            if (waveInPhase == 1) {
                if ((int)(elapsed / 900) > (int)((elapsed - TICK_MS) / 900)) {
                    for (int i = 0; i < (hellMode ? 4 : 2); i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.vx = 0;
                        b.vy = hellMode ? 2f : 1.4f;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.text = "CANCEL";
                        bullets.add(b);
                    }
                    Bullet b2 = new Bullet();
                    b2.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                    b2.y = boxY + boxH;
                    b2.vx = 0;
                    b2.vy = hellMode ? -2f : -1.4f;
                    b2.w = BULLET_SIZE;
                    b2.h = BULLET_SIZE;
                    b2.text = "CANCEL";
                    bullets.add(b2);
                }
                if (elapsed > 5000) { waveInPhase = 2; showDialogue("It warned me!"); }
            }
            if (waveInPhase == 2) {
                if ((int)(elapsed / 1200) > (int)((elapsed - TICK_MS) / 1200) && elapsed > 5500) {
                    boolean leftToRight = rnd.nextBoolean();
                    int row = boxY + 30 + rnd.nextInt(Math.max(1, boxH - 60));
                    for (int col = 0; col < boxW; col += (hellMode ? 30 : 40)) {
                        Bullet b = new Bullet();
                        b.w = 20;
                        b.h = 10;
                        b.y = row;
                        b.vy = 0;
                        if (leftToRight) { b.x = boxX - 25; b.vx = hellMode ? 3.2f : 2.5f; }
                        else { b.x = boxX + boxW + 25; b.vx = hellMode ? -3.2f : -2.5f; }
                        b.text = "";
                        bullets.add(b);
                    }
                }
                if (elapsed > 12000) { waveInPhase = 3; showDialogue("No safe opinions."); }
            }
            if (waveInPhase == 3) {
                if ((int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000) && elapsed > 12500) {
                    int cx = boxX + boxW / 2 - 10;
                    int cy = boxY + boxH / 2 - 10;
                    for (int i = 0; i < 4; i++) {
                        Bullet b = new Bullet();
                        b.w = 20;
                        b.h = 20;
                        b.x = cx;
                        b.y = cy;
                        double a = i * Math.PI / 2 + (rnd.nextDouble() * 0.3);
                        double spd = hellMode ? 2.5 : 2;
                        b.vx = (float)(Math.cos(a) * spd);
                        b.vy = (float)(Math.sin(a) * spd);
                        b.text = "";
                        bullets.add(b);
                    }
                }
                if (elapsed > 18000) { waveInPhase = 4; showDialogue("This is chaos!"); }
            }
            if (waveInPhase == 4) {
                if ((int)(elapsed / 850) > (int)((elapsed - TICK_MS) / 850) && elapsed > 18500) {
                    int n = hellMode ? 4 : 2;
                    for (int i = 0; i < n; i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.vx = (rnd.nextFloat() - 0.5f) * 1.5f;
                        b.vy = 1.5f + rnd.nextFloat() * 0.8f;
                        b.text = "C";
                        bullets.add(b);
                    }
                }
                if (elapsed > 22000) {
                    phase = 3;
                    phaseStartTime = now;
                    waveInPhase = 0;
                    showDialogue("Still standing?");
                }
            }
        } else if (phase == 3) {
            if (waveInPhase == 0) {
                currentBoxScale = NORMAL_BOX_SCALE - (float)((elapsed / 2000.0) * (NORMAL_BOX_SCALE - SHRINK_BOX_SCALE));
                if (currentBoxScale < SHRINK_BOX_SCALE) currentBoxScale = SHRINK_BOX_SCALE;
                if (elapsed > 500 && (int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000)) {
                    Bullet b = new Bullet();
                    b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                    b.y = boxY - BULLET_SIZE;
                    b.w = BULLET_SIZE;
                    b.h = BULLET_SIZE;
                    b.vx = 0;
                    b.vy = 1.8f;
                    b.text = "CANCEL";
                    bullets.add(b);
                }
                if (elapsed > 2500) { waveInPhase = 1; showDialogue("Let's make this… tighter."); }
            }
            if (waveInPhase == 1) {
                if ((int)(elapsed / 1100) > (int)((elapsed - TICK_MS) / 1100) && elapsed > 3500) {
                    int cx = boxX + boxW / 2 - BULLET_SIZE / 2;
                    int cy = boxY + boxH / 2 - BULLET_SIZE / 2;
                    for (int i = 0; i < 12; i++) {
                        Bullet b = new Bullet();
                        double a = i * Math.PI * 2 / 12;
                        b.x = cx;
                        b.y = cy;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        double spd = 2.2;
                        b.vx = (float)(Math.cos(a) * spd);
                        b.vy = (float)(Math.sin(a) * spd);
                        b.bounce = true;
                        b.text = "";
                        bullets.add(b);
                    }
                }
                if (elapsed > 6500) { waveInPhase = 2; showDialogue("Final verdict."); }
            }
            if (waveInPhase == 2) {
                if ((int)(elapsed / 550) > (int)((elapsed - TICK_MS) / 550) && elapsed > 7000) {
                    boolean leftToRight = rnd.nextBoolean();
                    int row = boxY + 25 + rnd.nextInt(Math.max(1, boxH - 50));
                    int gap = hellMode ? 55 : 70;
                    for (int col = 0; col < boxW; col += gap) {
                        if (rnd.nextInt(3) == 0) continue;
                        Bullet b = new Bullet();
                        b.w = 22;
                        b.h = 10;
                        b.y = row;
                        b.vy = 0;
                        if (leftToRight) { b.x = boxX - 30; b.vx = 3.2f; }
                        else { b.x = boxX + boxW + 30; b.vx = -3.2f; }
                        b.text = "";
                        bullets.add(b);
                    }
                }
                if (elapsed > 9500) {
                    victory = true;
                    if (gameTimer != null) gameTimer.stop();
                    showDialogue(hellMode ? "You survived the judgment." : "You survived.");
                    SwingUtilities.invokeLater(() -> {
                        try { Thread.sleep(2200); } catch (InterruptedException ignored) {}
                        if (onVictory != null) onVictory.run();
                    });
                }
            }
        }
    }

    private void advancePhaseAndWaves(long now) {
        if (rnd.nextInt(80) == 0 && now > tauntUntil) showTaunt();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        long now = System.currentTimeMillis();

        int sx = 0, sy = 0;
        if (now < shakeUntil) {
            sx = (rnd.nextInt(7)) - 3;
            sy = (rnd.nextInt(7)) - 3;
        }
        g2.translate(sx, sy);

        updateBoxBounds();
        g2.setColor(Color.BLACK);
        g2.fillRect(boxX, boxY, boxW, boxH);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(Color.WHITE);
        g2.drawRect(boxX, boxY, boxW, boxH);

        for (Bullet b : bullets) {
            g2.setColor(new Color(180, 80, 80));
            g2.fillRect((int)b.x, (int)b.y, b.w, b.h);
            if (b.text != null && !b.text.isEmpty()) {
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(b.text, (int)b.x + (b.w - fm.stringWidth(b.text)) / 2, (int)b.y + b.h / 2 + fm.getAscent() / 2 - 2);
            }
        }

        for (GoldenItem gi : goldenItems) {
            g2.setColor(new Color(255, 215, 0));
            g2.fillOval((int)gi.x, (int)gi.y, GOLDEN_ITEM_SIZE, GOLDEN_ITEM_SIZE);
            g2.setColor(new Color(255, 255, 200));
            g2.drawOval((int)gi.x, (int)gi.y, GOLDEN_ITEM_SIZE, GOLDEN_ITEM_SIZE);
        }

        g2.setColor(Color.RED);
        g2.fillOval(player.x, player.y, HEART_SIZE, HEART_SIZE);
        g2.setColor(new Color(200, 50, 50));
        g2.drawOval(player.x, player.y, HEART_SIZE, HEART_SIZE);

        g2.setColor(new Color(220, 200, 100));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.drawString("HP: " + hp + " / " + MAX_HP, boxX, boxY - 12);

        if (now < dialogueUntil && dialogueText != null && !dialogueText.isEmpty()) {
            g2.setColor(new Color(240, 240, 230));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            int tw = g2.getFontMetrics().stringWidth(dialogueText);
            g2.drawString(dialogueText, boxX + (boxW - tw) / 2, boxY + boxH + 28);
        }
        if (now < tauntUntil && tauntText != null && !tauntText.isEmpty()) {
            g2.setColor(new Color(200, 150, 150));
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 12f));
            int tw = g2.getFontMetrics().stringWidth(tauntText);
            g2.drawString(tauntText, boxX + (boxW - tw) / 2, boxY - 28);
        }

        g2.setColor(new Color(120, 120, 110));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Arrow keys to move | Stay inside the box", w / 2 - 140, h - 25);
        g2.dispose();
    }

    private static class BattlePlayer {
        int x, y;
    }

    private static class Bullet {
        float x, y;
        int w = 24, h = 24;
        float vx, vy;
        boolean bounce;
        String text;
    }

    private static class GoldenItem {
        double x, y, vx, vy;
    }
}

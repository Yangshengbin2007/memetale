package game.scene;

import javax.sound.sampled.Clip;
import javax.swing.*;
import game.model.forest.ForestImageLoader;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
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
    /** true = main story (cave boss), can spawn golden item; false = minigame/hell, no golden item. */
    private final boolean allowGoldenItem;
    private final Runnable onVictory;
    private final Runnable onQuitToTitle;

    private static final int BOX_MARGIN_X = 80;
    private static final int BOX_MARGIN_TOP = 130;
    private static final int BOX_MARGIN_BOTTOM = 100;
    private static final int HEART_SIZE = 14;
    private static final int MAX_HP_NORMAL = 30;
    private static final int MAX_HP_HELL = 20;
    private static final int BULLET_SIZE = 24;
    private static final double NORMAL_BOX_SCALE = 1.0;
    private static final double SHRINK_BOX_SCALE = 0.7;
    private static final int SHAKE_DURATION_MS = 200;
    private static final int GOLDEN_ITEM_SIZE = 20;
    private static final String[] TAUNTS = {
        "Cute dodge. Do it again.",
        "You call that movement?",
        "You're one opinion away from disaster.",
        "I have screenshots of this performance.",
        "This is your final form?",
        "I expected more from a protagonist.",
        "You're dodging like a loading bar.",
        "Try harder. I'm barely trying.",
        "This timeline rejects your gameplay.",
        "You look nervous. Good."
    };
    private static final String[] HIT_LINES = {
        "Got you!", "Heh. Cope.", "Too slow!", "That one counted.", "Ouch? No, you."
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
    private int hp;
    private long shakeUntil;
    private int shakeOffsetX, shakeOffsetY;
    private String dialogueText = "";
    private long dialogueUntil;
    private String tauntText = "";
    private long tauntUntil;
    private boolean victory;
    private boolean victoryAskReplay = false;
    private boolean goldenItemSpawned;
    private boolean goldenItemCaught;
    private final Random rnd = new Random();
    private javax.swing.Timer gameTimer;
    private boolean up, down, left, right;
    private static final int TICK_MS = 16;
    private Clip battleMusicClip;
    private int lastMusicPhase = 0;
    private Clip hellAmbienceClip;
    /** Troll face: default usually, laugh rarely on hit, scared when player doing well, defeat only after victory. */
    private String topTrollState = "default";
    private long lastHitTime = 0;
    private long lastScaredTime = 0;
    private long lastAngryTime = 0;
    private static final long SCARED_AFTER_NO_HIT_MS = 7000;
    private static final long SCARED_DURATION_MS = 4000;
    private static final long ANGRY_DURATION_MS = 2200;
    private boolean wavesComplete = false;
    private long defeatReactionStart = -1;
    private static final int DEFEAT_REACTION_MS = 2200;
    private static final int ANGRY_REACTION_MS = 1500;
    private static final int PHASE3_SPAWN_MS = 20000;
    private boolean phase3SpawnStopped = false;
    private Image topTrollDefault, topTrollLaugh, topTrollScared, topTrollDefeat, topTrollAngry;
    private final List<Image> bulletTrollImages = new ArrayList<>();

    // Golden item rules (main story only, allowGoldenItem=true):
    // - it spawns at a random phase, once per attempt
    // - once caught, we mark it as "acquired" for the rest of this scene (until leaving/re-entering),
    //   so it won't spawn again on subsequent retries/difficulty decreases.
    private boolean goldenItemAcquired = false;
    private int goldenSpawnPhase = -1;
    /** On death: show knight dialogue. 0 = ask lower difficulty, 1 = show taunt then skip. */
    private boolean dead;
    private int deathPhase;
    private int difficultyReductions;
    private static final int MAX_DIFFICULTY_REDUCTIONS = 10;
    private Image knightImage;
    private final Rectangle yesBounds = new Rectangle();
    private final Rectangle noBounds = new Rectangle();
    private long tauntShowUntil;
    private static final String[] LOWER_DIFFICULTY_TAUNTS = {
        "You're really bad at this game.", "Skill issue. We're skipping this.", "Game over. For you. Literally."
    };
    private long lastPeriodicDialogueTime;

    public TrollBattleScene(boolean hellMode, boolean allowGoldenItem, Runnable onVictory, Runnable onQuitToTitle) {
        this.hellMode = hellMode;
        this.allowGoldenItem = allowGoldenItem;
        this.onVictory = onVictory;
        this.onQuitToTitle = onQuitToTitle;
        setBackground(new Color(15, 15, 25));
        setFocusable(true);
        initKey();
        initDeathMouse();
    }

    private void initDeathMouse() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (dead) {
                    if (deathPhase != 0) return;
                } else if (victoryAskReplay) {
                    // fallthrough to handle replay choice
                } else {
                    return;
                }
                Point p = e.getPoint();
                if (victoryAskReplay && !dead) {
                    if (yesBounds.contains(p)) {
                        victoryAskReplay = false;
                        restartBattle(false);
                    } else if (noBounds.contains(p)) {
                        victoryAskReplay = false;
                        if (onQuitToTitle != null) onQuitToTitle.run();
                        else if (onVictory != null) onVictory.run();
                    }
                    return;
                }

                if (hellMode) {
                    if (yesBounds.contains(p)) {
                        restartBattle(false);
                    } else if (noBounds.contains(p)) {
                        if (onQuitToTitle != null) onQuitToTitle.run();
                    }
                    return;
                }

                boolean isMainStory = allowGoldenItem;

                if (yesBounds.contains(p)) {
                    if (isMainStory) {
                        difficultyReductions++;
                        if (difficultyReductions >= MAX_DIFFICULTY_REDUCTIONS) {
                            deathPhase = 1;
                            String taunt = LOWER_DIFFICULTY_TAUNTS[rnd.nextInt(LOWER_DIFFICULTY_TAUNTS.length)];
                            dialogueText = "Darabongba: " + taunt;
                            dialogueUntil = System.currentTimeMillis() + 3500;
                            tauntShowUntil = System.currentTimeMillis() + 3200;
                            repaint();
                        } else {
                            restartBattle(true);
                        }
                    } else {
                        // Minigame normal: no difficulty decrease; only retry.
                        restartBattle(false);
                    }
                } else if (noBounds.contains(p)) {
                    if (isMainStory) {
                        restartBattle(false);
                    } else {
                        if (onQuitToTitle != null) onQuitToTitle.run();
                    }
                }
            }
        });
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
        dead = false;
        deathPhase = 0;
        difficultyReductions = 0;
        phase3SpawnStopped = false;
        updateBoxBounds();
        player.x = boxX + boxW / 2 - HEART_SIZE / 2;
        player.y = boxY + boxH / 2 - HEART_SIZE / 2;
        phase = 1;
        waveInPhase = 0;
        hp = hellMode ? MAX_HP_HELL : MAX_HP_NORMAL;
        bullets.clear();
        goldenItems.clear();
        victory = false;
        victoryAskReplay = false;
        phase3SpawnStopped = false;
        goldenItemAcquired = false;
        goldenSpawnPhase = allowGoldenItem && !hellMode ? (1 + rnd.nextInt(3)) : -1;
        goldenItemSpawned = false;
        goldenItemCaught = false;
        wavesComplete = false;
        defeatReactionStart = -1;
        topTrollState = "default";
        lastHitTime = 0;
        lastPeriodicDialogueTime = System.currentTimeMillis();
        currentBoxScale = NORMAL_BOX_SCALE;
        battleStartTime = System.currentTimeMillis();
        phaseStartTime = battleStartTime;
        loadTrollBattleImages();
        showDialogue("Let's start easy.");
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        gameTimer.start();
        startBattleMusicOnce();
        if (hellMode) {
            if (hellAmbienceClip != null) {
                try { if (hellAmbienceClip.isRunning()) hellAmbienceClip.stop(); hellAmbienceClip.close(); } catch (Exception ignore) {}
                hellAmbienceClip = null;
            }
            hellAmbienceClip = StartScene.loadMusicFromMusicDir("o.wav");
            if (hellAmbienceClip == null) hellAmbienceClip = StartScene.loadMusicFromMusicDir("'.mp3");
            if (hellAmbienceClip != null) {
                StartScene.applyVolumeToClipForScene(hellAmbienceClip, true, 0.18f);
                hellAmbienceClip.loop(Clip.LOOP_CONTINUOUSLY);
                hellAmbienceClip.start();
            }
        }
        requestFocusInWindow();
    }

    private void restartBattle(boolean reduceDifficulty) {
        dead = false;
        deathPhase = 0;
        updateBoxBounds();
        player.x = boxX + boxW / 2 - HEART_SIZE / 2;
        player.y = boxY + boxH / 2 - HEART_SIZE / 2;
        phase = 1;
        waveInPhase = 0;
        hp = hellMode ? MAX_HP_HELL : MAX_HP_NORMAL;
        bullets.clear();
        goldenItems.clear();
        victory = false;
        victoryAskReplay = false;
        phase3SpawnStopped = false;
        goldenItemSpawned = false;
        goldenItemCaught = false;
        if (allowGoldenItem && !hellMode && !goldenItemAcquired) {
            goldenSpawnPhase = 1 + rnd.nextInt(3);
        } else {
            goldenSpawnPhase = -1;
        }
        wavesComplete = false;
        defeatReactionStart = -1;
        topTrollState = "default";
        lastHitTime = 0;
        lastScaredTime = 0;
        lastPeriodicDialogueTime = System.currentTimeMillis();
        currentBoxScale = NORMAL_BOX_SCALE;
        battleStartTime = System.currentTimeMillis();
        phaseStartTime = battleStartTime;
        showDialogue("Try again.");
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        gameTimer.start();
        requestFocusInWindow();
    }

    private void loadTrollBattleImages() {
        topTrollDefault = ForestImageLoader.loadCharacter("troll_boss", "default");
        topTrollLaugh = ForestImageLoader.loadCharacter("troll_boss", "laugh");
        topTrollScared = ForestImageLoader.loadCharacter("troll_boss", "scared");
        topTrollAngry = ForestImageLoader.loadCharacter("troll_boss", "angry");
        topTrollDefeat = ForestImageLoader.loadCharacter("troll_boss", "defeat");
        bulletTrollImages.clear();
        if (topTrollDefault != null) bulletTrollImages.add(topTrollDefault);
        if (topTrollScared != null) bulletTrollImages.add(topTrollScared);
        if (topTrollLaugh != null) bulletTrollImages.add(topTrollLaugh);
        if (bulletTrollImages.isEmpty() && topTrollDefeat != null) bulletTrollImages.add(topTrollDefeat);
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

    /** Battle BGM: start once, no restart on phase change. */
    private void startBattleMusicOnce() {
        if (battleMusicClip != null) return;
        Clip next = StartScene.loadMusicFromMusicDir("fighttroll.wav");
        if (next == null) next = StartScene.loadMusicFromMusicDir("fighttroll.mp3");
        if (next != null) {
            battleMusicClip = next;
            StartScene.applyVolumeToClipForScene(battleMusicClip, true);
            battleMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            battleMusicClip.start();
            lastMusicPhase = 1;
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    private void showDialogue(String text) {
        if (text == null) text = "";
        if (!text.isEmpty() && !text.contains(":")) {
            dialogueText = "Troll Boss: " + text;
        } else {
            dialogueText = text;
        }
        dialogueUntil = System.currentTimeMillis() + 4200;
    }

    private void showTaunt() {
        tauntText = "Troll Boss: " + TAUNTS[rnd.nextInt(TAUNTS.length)];
        tauntUntil = System.currentTimeMillis() + 3800;
        if (!victory && !dead) {
            int roll = rnd.nextInt(100);
            if (roll < 20) {
                topTrollState = "laugh";
                lastHitTime = System.currentTimeMillis();
            } else if (roll < 30) {
                topTrollState = "angry";
                lastAngryTime = System.currentTimeMillis();
            } else if (roll < 55) {
                topTrollState = "scared";
                lastScaredTime = System.currentTimeMillis();
            } else {
                topTrollState = "default";
            }
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (dead) {
            if (deathPhase == 1 && now > tauntShowUntil) {
                if (gameTimer != null) gameTimer.stop();
                if (onVictory != null) onVictory.run();
            }
            repaint();
            return;
        }
        if (victory) {
            if (defeatReactionStart >= 0 && (now - defeatReactionStart) > DEFEAT_REACTION_MS) {
                // Minigame normal: ask replay instead of immediately returning.
                if (!hellMode && !allowGoldenItem) {
                    victoryAskReplay = true;
                } else {
                    if (gameTimer != null) gameTimer.stop();
                    if (onVictory != null) onVictory.run();
                }
            }
            repaint();
            return;
        }
        if (topTrollState.equals("laugh") && (now - lastHitTime) > ANGRY_REACTION_MS)
            topTrollState = "default";
        if (topTrollState.equals("scared") && (now - lastScaredTime) > SCARED_DURATION_MS)
            topTrollState = "default";
        if (topTrollState.equals("angry") && (now - lastAngryTime) > ANGRY_DURATION_MS)
            topTrollState = "default";
        if (!victory && topTrollState.equals("default") && (now - lastHitTime) > SCARED_AFTER_NO_HIT_MS && bullets.size() < 15) {
            topTrollState = "scared";
            lastScaredTime = now;
        }
        if (now > dialogueUntil && now > tauntUntil && now - lastPeriodicDialogueTime > 2200) {
            lastPeriodicDialogueTime = now;
            showTaunt();
        }
        updateBoxBounds();

        if (up) player.y = Math.max(boxY, player.y - 4);
        if (down) player.y = Math.min(boxY + boxH - HEART_SIZE, player.y + 4);
        if (left) player.x = Math.max(boxX, player.x - 4);
        if (right) player.x = Math.min(boxX + boxW - HEART_SIZE, player.x + 4);

        spawnBullets(now);
        // Main story only: golden item appears randomly in phases until it is caught once.
        if (allowGoldenItem && !hellMode && !goldenItemAcquired
            && goldenSpawnPhase == phase && !goldenItemSpawned && waveInPhase >= 1) {
            spawnGoldenItem();
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b.orbit) {
                b.orbitAngle += b.orbitSpeed;
                b.x = b.orbitCx + b.orbitRadius * (float)Math.cos(b.orbitAngle) - b.w / 2f;
                b.y = b.orbitCy + b.orbitRadius * (float)Math.sin(b.orbitAngle) - b.h / 2f;
            } else {
                if (b.driftRotSpeed != 0) {
                    float cos = (float)Math.cos(b.driftRotSpeed);
                    float sin = (float)Math.sin(b.driftRotSpeed);
                    float nx = b.vx * cos - b.vy * sin;
                    float ny = b.vx * sin + b.vy * cos;
                    b.vx = nx;
                    b.vy = ny;
                }
                b.x += b.vx;
                b.y += b.vy;
            }
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
                lastHitTime = now;
                    // Expression distribution: fairly varied, angry slightly less.
                    boolean lowHp = hp <= (hellMode ? MAX_HP_HELL : MAX_HP_NORMAL) / 3;
                    int roll = rnd.nextInt(100);
                    if (lowHp) {
                        if (roll < 45) topTrollState = "default";
                        else if (roll < 65) topTrollState = "laugh";
                        else if (roll < 85) { topTrollState = "angry"; lastAngryTime = now; }
                        else topTrollState = "scared";
                    } else {
                        if (roll < 60) topTrollState = "default";
                        else if (roll < 80) topTrollState = "laugh";
                        else if (roll < 90) topTrollState = "scared";
                        else { topTrollState = "angry"; lastAngryTime = now; }
                    }
                if (hp <= 0) {
                    hp = 0;
                    dead = true;
                    deathPhase = 0;
                    if (knightImage == null) knightImage = ForestImageLoader.loadCharacter("darabongba", "default");
                    // Keep the timer running so the "death dialogue" can keep repainting.
                    // (We stop bullet spawning because tick() returns early when dead==true.)
                    repaint();
                    return;
                }
                showDialogue(HIT_LINES[rnd.nextInt(HIT_LINES.length)]);
            }
        }

        for (int i = goldenItems.size() - 1; i >= 0; i--) {
            GoldenItem g = goldenItems.get(i);
            g.x += g.vx;
            g.y += g.vy;
            if (intersects(player.x, player.y, HEART_SIZE, HEART_SIZE, (int)g.x, (int)g.y, GOLDEN_ITEM_SIZE, GOLDEN_ITEM_SIZE)) {
                goldenItemCaught = true;
                goldenItemAcquired = true;
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

    private int reducedCount(int base) {
        if (difficultyReductions <= 0) return base;
        // Each "lower difficulty" confirmation reduces bullet count by ~10%.
        double scale = Math.max(0.15, 1.0 - 0.10 * difficultyReductions);
        return Math.max(1, (int)(base * scale));
    }

    private void spawnBullets(long now) {
        long elapsed = (now - phaseStartTime);

        if (phase == 1) {
            if (waveInPhase == 0 && elapsed > 500) {
                waveInPhase = 1;
                showDialogue("Let's start easy.");
            }
            if (waveInPhase == 1) {
                int interval = hellMode ? 800 : 1200;
                if ((int)(elapsed / interval) > (int)((elapsed - TICK_MS) / interval)) {
                    int n = reducedCount(hellMode ? 8 : 2);
                    for (int i = 0; i < n; i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.vx = 0;
                        b.vy = hellMode ? 1.8f : 1.2f;
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if (elapsed > 5000) { waveInPhase = 2; showDialogue("Oh great, now it's opinions from both sides."); }
            }
            if (waveInPhase == 2) {
                if ((int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000) && elapsed > 5500) {
                    boolean leftToRight = rnd.nextBoolean();
                    int row = boxY + 20 + rnd.nextInt(Math.max(1, boxH - 80));
                    int gap = hellMode ? 32 : 45;
                    for (int col = 0; col < boxW; col += gap) {
                        Bullet b = new Bullet();
                        b.w = 18;
                        b.h = 8;
                        b.y = row;
                        b.vy = 0;
                        if (leftToRight) { b.x = boxX - 20; b.vx = hellMode ? 3f : 2.2f; }
                        else { b.x = boxX + boxW + 20; b.vx = hellMode ? -3f : -2.2f; }
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
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
                    b.text = "";
                    b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
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
                    int n = reducedCount(hellMode ? 6 : 2);
                    for (int i = 0; i < n; i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.vx = 0;
                        b.vy = hellMode ? 2f : 1.4f;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                    Bullet b2 = new Bullet();
                    b2.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                    b2.y = boxY + boxH;
                    b2.vx = 0;
                    b2.vy = hellMode ? -2f : -1.4f;
                    b2.w = BULLET_SIZE;
                    b2.h = BULLET_SIZE;
                    b2.text = "";
                    b2.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                    bullets.add(b2);
                }
                if (elapsed > 5000) { waveInPhase = 2; showDialogue("It warned me!"); }
            }
            if (waveInPhase == 2) {
                int colGap = hellMode ? Math.max(18, 26 - difficultyReductions * 2) : 40;
                if ((int)(elapsed / 1200) > (int)((elapsed - TICK_MS) / 1200) && elapsed > 5500) {
                    boolean leftToRight = rnd.nextBoolean();
                    int row = boxY + 30 + rnd.nextInt(Math.max(1, boxH - 60));
                    for (int col = 0; col < boxW; col += colGap) {
                        Bullet b = new Bullet();
                        b.w = 20;
                        b.h = 10;
                        b.y = row;
                        b.vy = 0;
                        if (leftToRight) { b.x = boxX - 25; b.vx = hellMode ? 3.2f : 2.5f; }
                        else { b.x = boxX + boxW + 25; b.vx = hellMode ? -3.2f : -2.5f; }
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if (elapsed > 12000) { waveInPhase = 3; showDialogue("No safe opinions."); }
            }
            if (hellMode && elapsed > 6000 && (int)(elapsed / 4000) > (int)((elapsed - TICK_MS) / 4000)) {
                float cx = boxX + boxW / 2f;
                float cy = boxY + boxH / 2f;
                int n = 14;
                float radius = Math.min(boxW, boxH) * 0.35f;
                for (int i = 0; i < n; i++) {
                    Bullet b = new Bullet();
                    b.w = BULLET_SIZE;
                    b.h = BULLET_SIZE;
                    b.orbit = true;
                    b.orbitCx = cx;
                    b.orbitCy = cy;
                    b.orbitRadius = radius + (i % 3) * 8;
                    b.orbitAngle = (float)(i * Math.PI * 2 / n + elapsed * 0.001);
                    b.orbitSpeed = 0.028f + (i % 2) * 0.01f;
                    b.x = b.orbitCx + b.orbitRadius * (float)Math.cos(b.orbitAngle) - b.w / 2f;
                    b.y = b.orbitCy + b.orbitRadius * (float)Math.sin(b.orbitAngle) - b.h / 2f;
                    b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                    bullets.add(b);
                }
            }
            if (hellMode && elapsed > 8000 && (int)(elapsed / 3200) > (int)((elapsed - TICK_MS) / 3200)) {
                for (int i = 0; i < 5; i++) {
                    Bullet b = new Bullet();
                    b.x = boxX + 30 + rnd.nextInt(Math.max(1, boxW - 80));
                    b.y = boxY - BULLET_SIZE;
                    b.w = BULLET_SIZE;
                    b.h = BULLET_SIZE;
                    b.vx = (rnd.nextFloat() - 0.5f) * 0.8f;
                    b.vy = 1.4f + rnd.nextFloat() * 0.6f;
                    b.driftRotSpeed = 0.022f;
                    b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                    bullets.add(b);
                }
            }
            if (hellMode && elapsed > 10000 && (int)(elapsed / 4200) > (int)((elapsed - TICK_MS) / 4200)) {
                int gapCenter = boxW / 2;
                int gapHalf = 50;
                for (int row = 0; row < 2; row++) {
                    int y = boxY + 40 + row * (boxH - 80) / 2;
                    for (int col = 0; col < boxW; col += 28) {
                        if (col >= gapCenter - gapHalf && col <= gapCenter + gapHalf) continue;
                        Bullet b = new Bullet();
                        b.w = 20;
                        b.h = 10;
                        b.x = boxX + col;
                        b.y = y;
                        b.vx = col < gapCenter ? 1.8f : -1.8f;
                        b.vy = 0;
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
            }
            if (waveInPhase == 3) {
                if ((int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000) && elapsed > 12500) {
                    int cx = boxX + boxW / 2 - 10;
                    int cy = boxY + boxH / 2 - 10;
                    int rays = hellMode ? 8 : 4;
                    for (int i = 0; i < rays; i++) {
                        Bullet b = new Bullet();
                        b.w = 20;
                        b.h = 20;
                        b.x = cx;
                        b.y = cy;
                        double a = i * Math.PI * 2 / rays + (rnd.nextDouble() * 0.3);
                        double spd = hellMode ? 2.5 : 2;
                        b.vx = (float)(Math.cos(a) * spd);
                        b.vy = (float)(Math.sin(a) * spd);
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if (elapsed > 18000) { waveInPhase = 4; showDialogue("This is chaos!"); }
            }
            if (waveInPhase == 4) {
                if ((int)(elapsed / 850) > (int)((elapsed - TICK_MS) / 850) && elapsed > 18500) {
                    int n = hellMode ? 6 : 2;
                    for (int i = 0; i < n; i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                        b.y = boxY - BULLET_SIZE;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.vx = (rnd.nextFloat() - 0.5f) * 1.5f;
                        b.vy = 1.5f + rnd.nextFloat() * 0.8f;
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if (elapsed > 22000) {
                    phase = 3;
                    phaseStartTime = now;
                    waveInPhase = 0;
                    wavesComplete = true;
                    showDialogue("Still standing?");
                }
            }
        } else if (phase == 3) {
            if (!hellMode && !phase3SpawnStopped && elapsed >= PHASE3_SPAWN_MS) {
                phase3SpawnStopped = true;
                showDialogue("No more opinions.");
            }
            boolean canSpawnPhase3 = hellMode || (!phase3SpawnStopped && elapsed < PHASE3_SPAWN_MS);
            if (waveInPhase == 0) {
                currentBoxScale = NORMAL_BOX_SCALE - (float)((elapsed / 2000.0) * (NORMAL_BOX_SCALE - SHRINK_BOX_SCALE));
                if (currentBoxScale < SHRINK_BOX_SCALE) currentBoxScale = SHRINK_BOX_SCALE;
                if (canSpawnPhase3 && elapsed > 500 && (int)(elapsed / 1000) > (int)((elapsed - TICK_MS) / 1000)) {
                    Bullet b = new Bullet();
                    b.x = boxX + rnd.nextInt(Math.max(1, boxW - BULLET_SIZE));
                    b.y = boxY - BULLET_SIZE;
                    b.w = BULLET_SIZE;
                    b.h = BULLET_SIZE;
                    b.vx = 0;
                    b.vy = 1.8f;
                    b.text = "";
                    b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                    bullets.add(b);
                }
                if (elapsed > 2500) { waveInPhase = 1; showDialogue("Let's make this… tighter."); }
            }
            if (waveInPhase == 1 && canSpawnPhase3) {
                if ((int)(elapsed / 1100) > (int)((elapsed - TICK_MS) / 1100) && elapsed > 3500) {
                    int cx = boxX + boxW / 2 - BULLET_SIZE / 2;
                    int cy = boxY + boxH / 2 - BULLET_SIZE / 2;
                    int rays = hellMode ? 18 : 16;
                    for (int i = 0; i < rays; i++) {
                        Bullet b = new Bullet();
                        double a = i * Math.PI * 2 / rays;
                        b.x = cx;
                        b.y = cy;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        double spd = hellMode ? 2.4 : 2.2;
                        b.vx = (float)(Math.cos(a) * spd);
                        b.vy = (float)(Math.sin(a) * spd);
                        // Phase 3 should clear naturally; no bouncing bullets here.
                        b.bounce = false;
                        b.text = "";
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if (elapsed > 6500) { waveInPhase = 2; showDialogue("Final verdict."); }
            }
            if (waveInPhase == 2 && canSpawnPhase3) {
                if ((int)(elapsed / 500) > (int)((elapsed - TICK_MS) / 500) && elapsed > 7000) {
                    boolean leftToRight = rnd.nextBoolean();
                    int gap = hellMode ? 48 : 58;
                    int rows = hellMode ? 3 : 2;
                    for (int r = 0; r < rows; r++) {
                        int row = boxY + 20 + (boxH - 40) * r / Math.max(1, rows - 1) + rnd.nextInt(8);
                        for (int col = 0; col < boxW; col += gap) {
                            if (rnd.nextInt(4) == 0) continue;
                            Bullet b = new Bullet();
                            b.w = 22;
                            b.h = 10;
                            b.y = row;
                            b.vy = 0;
                            if (leftToRight) { b.x = boxX - 30; b.vx = 3.2f; }
                            else { b.x = boxX + boxW + 30; b.vx = -3.2f; }
                            b.text = "";
                            b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                            bullets.add(b);
                        }
                    }
                }
            }
            if (hellMode && phase == 3 && elapsed > 3000) {
                if ((int)(elapsed / 3500) > (int)((elapsed - TICK_MS) / 3500)) {
                    float cx = boxX + boxW / 2f;
                    float cy = boxY + boxH / 2f;
                    int n = 16;
                    float radius = Math.min(boxW, boxH) * 0.32f;
                    for (int i = 0; i < n; i++) {
                        Bullet b = new Bullet();
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.orbit = true;
                        b.orbitCx = cx;
                        b.orbitCy = cy;
                        b.orbitRadius = radius + (i % 4) * 6;
                        b.orbitAngle = (float)(i * Math.PI * 2 / n - elapsed * 0.002);
                        b.orbitSpeed = -0.032f;
                        b.x = b.orbitCx + b.orbitRadius * (float)Math.cos(b.orbitAngle) - b.w / 2f;
                        b.y = b.orbitCy + b.orbitRadius * (float)Math.sin(b.orbitAngle) - b.h / 2f;
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if ((int)(elapsed / 2800) > (int)((elapsed - TICK_MS) / 2800)) {
                    for (int i = 0; i < reducedCount(6); i++) {
                        Bullet b = new Bullet();
                        b.x = boxX + 20 + rnd.nextInt(Math.max(1, boxW - 60));
                        b.y = boxY + boxH + BULLET_SIZE;
                        b.w = BULLET_SIZE;
                        b.h = BULLET_SIZE;
                        b.vx = (rnd.nextFloat() - 0.5f) * 1f;
                        b.vy = -1.6f - rnd.nextFloat() * 0.5f;
                        b.driftRotSpeed = -0.018f;
                        b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                        bullets.add(b);
                    }
                }
                if ((int)(elapsed / 3800) > (int)((elapsed - TICK_MS) / 3800) && elapsed > 5000) {
                    int gapCenter = boxW / 2;
                    int gapHalf = 45;
                    for (int row = 0; row < 3; row++) {
                        int y = boxY + 30 + row * (boxH - 60) / 2;
                        for (int col = 0; col < boxW; col += 24) {
                            if (col >= gapCenter - gapHalf && col <= gapCenter + gapHalf) continue;
                            Bullet b = new Bullet();
                            b.w = 18;
                            b.h = 10;
                            b.x = boxX + col;
                            b.y = y;
                            b.vx = col < gapCenter ? 2.2f : -2.2f;
                            b.vy = 0;
                            b.imageIndex = bulletTrollImages.isEmpty() ? 0 : rnd.nextInt(bulletTrollImages.size());
                            bullets.add(b);
                        }
                    }
                }
            }
        }
        // Normal/story completion:
        // - stop spawning after phase3 (20s)
        // - then wait until all bullets are gone
        // - only then let the trollface say "I lost" and proceed
        if (!hellMode && wavesComplete && phase3SpawnStopped && bullets.isEmpty() && !victory) {
            victory = true;
            defeatReactionStart = now;
            topTrollState = "defeat";
            showDialogue("I lost.");
        }
    }

    private void advancePhaseAndWaves(long now) {
        if (now > tauntUntil && now > dialogueUntil && now - lastPeriodicDialogueTime > 2000 && rnd.nextInt(4) == 0) {
            lastPeriodicDialogueTime = now;
            showTaunt();
        }
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

        if (dead) {
            g2.setColor(new Color(0, 0, 0, 220));
            g2.fillRect(0, 0, w, h);
            String msg;
            if (hellMode) {
                long seconds = Math.max(0, (now - battleStartTime) / 1000);
                msg = deathPhase == 0 ? ("You survived " + seconds + "s. Want to retry?") : (dialogueText != null ? dialogueText : "");
            } else {
                if (allowGoldenItem) {
                    msg = deathPhase == 0
                        ? "Darabongba: You got cancelled. Lower difficulty by 10% and retry?"
                        : (dialogueText != null ? dialogueText : "");
                } else {
                    // Minigame normal: no difficulty reduction.
                    msg = deathPhase == 0
                        ? "Darabongba: You got cancelled. Retry?"
                        : (dialogueText != null ? dialogueText : "");
                }
            }
            int boxW2 = Math.min(500, w - 80);
            int boxX2 = (w - boxW2) / 2;
            int boxH2 = 120;
            int boxY2 = h / 2 - boxH2 / 2 - 40;
            if (knightImage != null) {
                int kw = 120;
                int kh = 140;
                g2.drawImage(knightImage, w / 2 - kw / 2, boxY2 - kh - 20, kw, kh, this);
            }
            g2.setColor(new Color(30, 25, 40));
            g2.fillRoundRect(boxX2, boxY2, boxW2, boxH2, 12, 12);
            g2.setColor(new Color(180, 140, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(boxX2, boxY2, boxW2, boxH2, 12, 12);
            g2.setColor(new Color(240, 235, 220));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            if (tw > boxW2 - 24) {
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
                fm = g2.getFontMetrics();
            }
            g2.drawString(msg, boxX2 + (boxW2 - Math.min(fm.stringWidth(msg), boxW2 - 24)) / 2, boxY2 + 28);
            if (deathPhase == 0) {
                int btnW = 110;
                int btnH = 36;
                int by = boxY2 + boxH2 - btnH - 16;

                boolean minigameNormal = (!hellMode && !allowGoldenItem);
                if (minigameNormal) {
                    yesBounds.setBounds(boxX2 + (boxW2 - btnW) / 2, by, btnW, btnH);
                    noBounds.setBounds(-9999, -9999, 1, 1);
                    g2.setColor(new Color(50, 90, 50));
                    g2.fillRoundRect(yesBounds.x, yesBounds.y, btnW, btnH, 8, 8);
                    g2.setColor(new Color(220, 220, 200));
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                    fm = g2.getFontMetrics();
                    g2.drawString("Yes", yesBounds.x + (btnW - fm.stringWidth("Yes")) / 2, yesBounds.y + (btnH + fm.getAscent()) / 2 - 2);
                } else {
                    yesBounds.setBounds(boxX2 + boxW2 / 2 - btnW - 12, by, btnW, btnH);
                    noBounds.setBounds(boxX2 + boxW2 / 2 + 12, by, btnW, btnH);
                    g2.setColor(new Color(50, 90, 50));
                    g2.fillRoundRect(yesBounds.x, yesBounds.y, btnW, btnH, 8, 8);
                    g2.setColor(new Color(90, 50, 50));
                    g2.fillRoundRect(noBounds.x, noBounds.y, btnW, btnH, 8, 8);
                    g2.setColor(new Color(220, 220, 200));
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                    fm = g2.getFontMetrics();
                    g2.drawString("Yes", yesBounds.x + (btnW - fm.stringWidth("Yes")) / 2, yesBounds.y + (btnH + fm.getAscent()) / 2 - 2);
                    g2.drawString("No", noBounds.x + (btnW - fm.stringWidth("No")) / 2, noBounds.y + (btnH + fm.getAscent()) / 2 - 2);
                }
            }
            g2.dispose();
            return;
        }

        if (victoryAskReplay) {
            g2.setColor(new Color(0, 0, 0, 220));
            g2.fillRect(0, 0, w, h);

            int boxW2 = Math.min(520, w - 80);
            int boxX2 = (w - boxW2) / 2;
            int boxH2 = 160;
            int boxY2 = h / 2 - boxH2 / 2;

            // Center message box
            g2.setColor(new Color(30, 25, 40));
            g2.fillRoundRect(boxX2, boxY2, boxW2, boxH2, 12, 12);
            g2.setColor(new Color(180, 140, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(boxX2, boxY2, boxW2, boxH2, 12, 12);

            g2.setColor(new Color(245, 240, 230));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
            FontMetrics fm = g2.getFontMetrics();
            String lostLine = (dialogueText != null && !dialogueText.isEmpty()) ? dialogueText : "I lost.";
            String askLine = "Want to try again?";
            int tw = fm.stringWidth(lostLine);
            g2.drawString(lostLine, boxX2 + (boxW2 - tw) / 2, boxY2 + 48);
            tw = fm.stringWidth(askLine);
            g2.drawString(askLine, boxX2 + (boxW2 - tw) / 2, boxY2 + 78);

            int btnW = 110;
            int btnH = 38;
            int by = boxY2 + boxH2 - btnH - 16;
            yesBounds.setBounds(boxX2 + boxW2 / 2 - btnW - 16, by, btnW, btnH);
            noBounds.setBounds(boxX2 + boxW2 / 2 + 16, by, btnW, btnH);

            g2.setColor(new Color(50, 90, 50));
            g2.fillRoundRect(yesBounds.x, yesBounds.y, btnW, btnH, 8, 8);
            g2.setColor(new Color(90, 50, 50));
            g2.fillRoundRect(noBounds.x, noBounds.y, btnW, btnH, 8, 8);

            g2.setColor(new Color(220, 220, 200));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            fm = g2.getFontMetrics();
            g2.drawString("Yes", yesBounds.x + (btnW - fm.stringWidth("Yes")) / 2, yesBounds.y + (btnH + fm.getAscent()) / 2 - 2);
            g2.drawString("No", noBounds.x + (btnW - fm.stringWidth("No")) / 2, noBounds.y + (btnH + fm.getAscent()) / 2 - 2);

            g2.dispose();
            return;
        }

        updateBoxBounds();
        int topTrollY = boxY - 52;
        int topTrollSize = 48;
        int topMoveX = 0;
        int topMoveY = 0;
        if ("laugh".equals(topTrollState)) {
            topMoveX = (int) (Math.sin(now / 90.0) * 4);
            topMoveY = (int) (Math.cos(now / 120.0) * 2);
        } else if ("scared".equals(topTrollState)) {
            topMoveY = (int) (Math.sin(now / 70.0) * 3);
        } else if ("angry".equals(topTrollState)) {
            topMoveX = (int) (Math.sin(now / 45.0) * 6);
            topMoveY = (int) (Math.cos(now / 60.0) * 2);
        } else if ("default".equals(topTrollState)) {
            topMoveY = (int) (Math.sin(now / 140.0) * 2);
        }
        Image topImg = topTrollDefault;
        if ("defeat".equals(topTrollState)) topImg = topTrollDefeat;
        else if ("laugh".equals(topTrollState)) topImg = topTrollLaugh;
        else if ("scared".equals(topTrollState)) topImg = topTrollScared;
        else if ("angry".equals(topTrollState)) topImg = topTrollAngry;
        if (topImg != null) {
            int tx = boxX + (boxW - topTrollSize) / 2 + topMoveX;
            int ty = topTrollY + topMoveY;
            g2.drawImage(topImg, tx, ty, topTrollSize, topTrollSize, this);
        }

        boolean dialogueActive = now < dialogueUntil && dialogueText != null && !dialogueText.isEmpty();
        boolean tauntActive = now < tauntUntil && tauntText != null && !tauntText.isEmpty();
        boolean showDialogueBox = dialogueActive || tauntActive;
        String boxContent = dialogueActive ? dialogueText : (tauntActive ? tauntText : "");
        if (showDialogueBox && !boxContent.isEmpty() && boxW > 20) {
            int dbPad = 14;
            int dbY = topTrollY + topTrollSize + 4;
            int dbW = Math.max(boxW, 200);
            int dbH = 44;
            int dbX = boxX;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
            g2.setColor(new Color(28, 22, 35));
            g2.fillRoundRect(dbX, dbY, dbW, dbH, 10, 10);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(180, 140, 60));
            g2.drawRoundRect(dbX, dbY, dbW, dbH, 10, 10);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(new Color(245, 240, 230));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15f));
            FontMetrics fm = g2.getFontMetrics();
            String toDraw = boxContent.length() > 60 ? boxContent.substring(0, 57) + "..." : boxContent;
            int tw = fm.stringWidth(toDraw);
            if (tw > dbW - dbPad * 2) {
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                fm = g2.getFontMetrics();
                toDraw = boxContent.length() > 80 ? boxContent.substring(0, 77) + "..." : boxContent;
                tw = Math.min(fm.stringWidth(toDraw), dbW - dbPad * 2);
            }
            g2.drawString(toDraw, dbX + (dbW - tw) / 2, dbY + (dbH + fm.getAscent()) / 2 - 2);
        }

        g2.setColor(Color.BLACK);
        g2.fillRect(boxX, boxY, boxW, boxH);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(Color.WHITE);
        g2.drawRect(boxX, boxY, boxW, boxH);

        for (Bullet b : bullets) {
            if (!bulletTrollImages.isEmpty() && b.imageIndex >= 0 && b.imageIndex < bulletTrollImages.size()) {
                Image img = bulletTrollImages.get(b.imageIndex);
                if (img != null) {
                    g2.drawImage(img, (int)b.x, (int)b.y, b.w, b.h, this);
                } else {
                    g2.setColor(new Color(180, 80, 80));
                    g2.fillRect((int)b.x, (int)b.y, b.w, b.h);
                }
            } else {
                g2.setColor(new Color(180, 80, 80));
                g2.fillRect((int)b.x, (int)b.y, b.w, b.h);
            }
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

        drawHeart(g2, player.x, player.y, HEART_SIZE, HEART_SIZE);

        g2.setColor(new Color(220, 200, 100));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        int maxHp = hellMode ? MAX_HP_HELL : MAX_HP_NORMAL;
        g2.drawString("HP: " + hp + " / " + maxHp, boxX, boxY - 12);


        g2.setColor(new Color(120, 120, 110));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Arrow keys to move | Stay inside the box", w / 2 - 140, h - 25);
        g2.dispose();
    }

    private void drawHeart(Graphics2D g2, int x, int y, int w, int h) {
        Path2D p = new Path2D.Float();
        float cx = x + w / 2f;
        float cy = y + h * 0.35f;
        float r = Math.min(w, h) * 0.35f;
        p.moveTo(cx, cy - r * 0.3f);
        p.curveTo(cx + r * 1.2f, cy - r * 1.4f, cx + r * 1.2f, cy + r * 0.5f, cx, cy + r * 1.1f);
        p.curveTo(cx - r * 1.2f, cy + r * 0.5f, cx - r * 1.2f, cy - r * 1.4f, cx, cy - r * 0.3f);
        p.closePath();
        g2.setColor(Color.RED);
        g2.fill(p);
        g2.setColor(new Color(200, 50, 50));
        g2.draw(p);
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
        int imageIndex = 0;
        float driftRotSpeed = 0;
        boolean orbit;
        float orbitCx, orbitCy, orbitAngle, orbitSpeed, orbitRadius;
    }

    private static class GoldenItem {
        double x, y, vx, vy;
    }
}

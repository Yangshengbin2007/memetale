package game.scene;

import game.io.SaveLoad;
import game.model.DialogueRecord;
import game.model.GameState;
import game.model.StoryState;

import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Chapter 3 epilogue: English dialogue on {@code goodending} art, {@code cg2.wav}, credits with {@code rickroll.wav},
 * optional extra lines, then title. Typewriter on dialogue; hold Space to fast-forward; ESC pause (save/load/settings/history/quit).
 */
public class DragonReunionEndingScene extends JPanel implements Scene {
    private static final int PHASE_MAIN = 0;
    private static final int PHASE_CREDITS = 1;
    private static final int PHASE_EXTRA = 2;
    private static final int PHASE_FINAL = 3;

    private static final int TEXT_DURATION_MS = 1000;
    private static final int TEXT_ANIM_DELAY_MS = 40;
    private static final int SPACE_HOLD_MS = 2800;
    private static final int FAST_FORWARD_INTERVAL_MS = 90;

    private final Runnable onCompleteToTitle;
    private final Runnable navigateAfterLoad;

    private Image backgroundImage;
    private Clip mainBgmClip;
    private Clip creditsBgmClip;

    private int phase = PHASE_MAIN;
    private int mainLineIndex;
    private int creditPageIndex;
    private int extraLineIndex;

    private long lineStartTime;
    private boolean spaceKeyHeld;
    /** Suppresses key-repeat on Space so one physical press = one reveal/advance. */
    private boolean spaceDownPhysical;
    private boolean fastForwardActive;
    private javax.swing.Timer holdSpaceTimer;
    private javax.swing.Timer fastForwardTimer;
    private javax.swing.Timer typewriterRepaintTimer;

    private boolean restoreOnNextEnter;
    /** Prevents double title transition from rapid click + key on the final screen. */
    private boolean finishingToTitle;

    private boolean pauseMenuVisible;
    private boolean hoverPauseSave, hoverPauseLoad, hoverPauseSettings, hoverPauseHistory, hoverPauseQuit;
    private boolean pressedPauseSave, pressedPauseLoad, pressedPauseSettings, pressedPauseHistory, pressedPauseQuit;
    private final Rectangle pauseSaveBounds = new Rectangle();
    private final Rectangle pauseLoadBounds = new Rectangle();
    private final Rectangle pauseSettingsBounds = new Rectangle();
    private final Rectangle pauseHistoryBounds = new Rectangle();
    private final Rectangle pauseQuitBounds = new Rectangle();

    private final List<DialogueRecord> epilogueHistory = new ArrayList<>();
    private int lastRecordedMainIndex = -1;
    private int lastRecordedExtraIndex = -1;

    public DragonReunionEndingScene(Runnable onCompleteToTitle, Runnable navigateAfterLoad) {
        this.onCompleteToTitle = onCompleteToTitle;
        this.navigateAfterLoad = navigateAfterLoad;
        setBackground(Color.BLACK);
        setFocusable(true);
        loadBackground();
        initTimers();
        initKey();
        initMouse();
    }

    /** When set, {@link #onEnter()} restores phase and indices from {@link GameState#getState()}. */
    public void setRestoreOnNextEnter(boolean v) {
        this.restoreOnNextEnter = v;
    }

    private Window dialogOwner() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return w != null ? w : JOptionPane.getRootFrame();
    }

    private void requestSceneFocusLater() {
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) requestFocusInWindow();
        });
    }

    private void initTimers() {
        holdSpaceTimer = new javax.swing.Timer(SPACE_HOLD_MS, e -> {
            if (spaceKeyHeld && (phase == PHASE_MAIN || phase == PHASE_EXTRA || phase == PHASE_CREDITS)) {
                fastForwardActive = true;
                if (fastForwardTimer != null && !fastForwardTimer.isRunning()) fastForwardTimer.start();
            }
        });
        holdSpaceTimer.setRepeats(false);
        fastForwardTimer = new javax.swing.Timer(FAST_FORWARD_INTERVAL_MS, e -> {
            if (!fastForwardActive) return;
            if (phase == PHASE_MAIN || phase == PHASE_EXTRA) {
                advanceOneStep();
            } else if (phase == PHASE_CREDITS) {
                advanceOneStep();
            } else {
                fastForwardTimer.stop();
            }
            repaint();
        });
        typewriterRepaintTimer = new javax.swing.Timer(TEXT_ANIM_DELAY_MS, e -> {
            if (pauseMenuVisible) return;
            if (phase != PHASE_MAIN && phase != PHASE_EXTRA) return;
            String t = getCurrentBodyText();
            if (t == null || t.isEmpty()) return;
            if (computeVisibleChars(t) < t.length()) repaint();
        });
    }

    private void loadBackground() {
        String[] names = {"goodending.cg", "goodending.jpg", "goodending.png", "goodending.jpeg"};
        String[][] roots = {
            {"/image/Chapter Three/", "/image/Chapter%20Three/", "image/Chapter Three/"},
            {"/image/Chapter one/", "/image/chapter%20one/", "image/Chapter one/"},
        };
        for (String[] root : roots) {
            for (String n : names) {
                Image img = tryLoadImage(root[0] + n, root[2] + n);
                if (img != null) {
                    backgroundImage = img;
                    return;
                }
                img = tryLoadImage(root[1] + n, root[2] + n);
                if (img != null) {
                    backgroundImage = img;
                    return;
                }
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

    private String getCurrentBodyText() {
        if (phase == PHASE_MAIN && mainLineIndex < DragonReunionEndingData.MAIN_LINES.length) {
            return DragonReunionEndingData.MAIN_LINES[mainLineIndex][1];
        }
        if (phase == PHASE_EXTRA && extraLineIndex < DragonReunionEndingData.EXTRA_LINES.length) {
            return DragonReunionEndingData.EXTRA_LINES[extraLineIndex][1];
        }
        return "";
    }

    private int computeVisibleChars(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (fastForwardActive) return text.length();
        long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : 0;
        int total = text.length();
        return total == 0 ? 0 : (int) Math.min(total, (long) total * elapsed / TEXT_DURATION_MS);
    }

    private void tryRevealOrAdvance() {
        if (phase == PHASE_MAIN || phase == PHASE_EXTRA) {
            String text = getCurrentBodyText();
            int totalChars = text.length();
            int visibleChars = computeVisibleChars(text);
            if (visibleChars < totalChars) {
                lineStartTime = System.currentTimeMillis() - TEXT_DURATION_MS;
                repaint();
                updateTypewriterTimerRunning();
            } else {
                advanceOneStep();
            }
        }
    }

    private void recordMainLineAtCurrentIndex() {
        if (mainLineIndex < 0 || mainLineIndex >= DragonReunionEndingData.MAIN_LINES.length) return;
        if (lastRecordedMainIndex == mainLineIndex) return;
        String[] row = DragonReunionEndingData.MAIN_LINES[mainLineIndex];
        epilogueHistory.add(new DialogueRecord(row[0], row[1]));
        lastRecordedMainIndex = mainLineIndex;
    }

    private void recordExtraLineAtCurrentIndex() {
        if (extraLineIndex < 0 || extraLineIndex >= DragonReunionEndingData.EXTRA_LINES.length) return;
        if (lastRecordedExtraIndex == extraLineIndex) return;
        String[] row = DragonReunionEndingData.EXTRA_LINES[extraLineIndex];
        epilogueHistory.add(new DialogueRecord(row[0], row[1]));
        lastRecordedExtraIndex = extraLineIndex;
    }

    private void advanceOneStep() {
        if (phase == PHASE_MAIN) {
            recordMainLineAtCurrentIndex();
            if (mainLineIndex + 1 < DragonReunionEndingData.MAIN_LINES.length) {
                mainLineIndex++;
                lineStartTime = System.currentTimeMillis();
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
            recordExtraLineAtCurrentIndex();
            if (extraLineIndex + 1 < DragonReunionEndingData.EXTRA_LINES.length) {
                extraLineIndex++;
                lineStartTime = System.currentTimeMillis();
            } else {
                phase = PHASE_FINAL;
            }
        } else if (phase == PHASE_FINAL) {
            finishToTitle();
        }
        updateTypewriterTimerRunning();
        repaint();
    }

    private void updateTypewriterTimerRunning() {
        if (phase != PHASE_MAIN && phase != PHASE_EXTRA) {
            typewriterRepaintTimer.stop();
            return;
        }
        String t = getCurrentBodyText();
        if (t != null && !t.isEmpty() && computeVisibleChars(t) < t.length()) {
            if (!typewriterRepaintTimer.isRunning()) typewriterRepaintTimer.start();
        } else {
            typewriterRepaintTimer.stop();
        }
    }

    private void startCreditsPhase() {
        stopMainBgm();
        phase = PHASE_CREDITS;
        creditPageIndex = 0;
        typewriterRepaintTimer.stop();
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
        lineStartTime = System.currentTimeMillis();
        updateTypewriterTimerRunning();
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

    private void syncProgressToStoryState(StoryState state) {
        state.setCurrentScene("dragon_reunion_ending");
        state.setSavedChapter(1);
        state.setDragonEpiloguePhase(phase);
        state.setDragonEpilogueMainIndex(mainLineIndex);
        state.setDragonEpilogueCreditIndex(creditPageIndex);
        state.setDragonEpilogueExtraIndex(extraLineIndex);
    }

    private void rebuildHistoryAfterRestore() {
        epilogueHistory.clear();
        if (phase == PHASE_MAIN) {
            for (int i = 0; i < mainLineIndex && i < DragonReunionEndingData.MAIN_LINES.length; i++) {
                String[] row = DragonReunionEndingData.MAIN_LINES[i];
                epilogueHistory.add(new DialogueRecord(row[0], row[1]));
            }
            lastRecordedMainIndex = mainLineIndex > 0 ? mainLineIndex - 1 : -1;
        } else {
            for (int i = 0; i < DragonReunionEndingData.MAIN_LINES.length; i++) {
                String[] row = DragonReunionEndingData.MAIN_LINES[i];
                epilogueHistory.add(new DialogueRecord(row[0], row[1]));
            }
            lastRecordedMainIndex = DragonReunionEndingData.MAIN_LINES.length - 1;
        }
        if (phase == PHASE_EXTRA) {
            for (int i = 0; i < extraLineIndex && i < DragonReunionEndingData.EXTRA_LINES.length; i++) {
                String[] row = DragonReunionEndingData.EXTRA_LINES[i];
                epilogueHistory.add(new DialogueRecord(row[0], row[1]));
            }
            lastRecordedExtraIndex = extraLineIndex > 0 ? extraLineIndex - 1 : -1;
        } else if (phase == PHASE_FINAL) {
            for (String[] row : DragonReunionEndingData.EXTRA_LINES) {
                epilogueHistory.add(new DialogueRecord(row[0], row[1]));
            }
            lastRecordedExtraIndex = DragonReunionEndingData.EXTRA_LINES.length - 1;
        } else {
            lastRecordedExtraIndex = -1;
        }
    }

    private void finishToTitle() {
        if (finishingToTitle) return;
        finishingToTitle = true;
        typewriterRepaintTimer.stop();
        stopMainBgm();
        stopCreditsBgm();
        if (onCompleteToTitle != null) onCompleteToTitle.run();
    }

    private void initKey() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    pauseMenuVisible = !pauseMenuVisible;
                    if (pauseMenuVisible) {
                        fastForwardActive = false;
                        spaceKeyHeld = false;
                        spaceDownPhysical = false;
                        if (fastForwardTimer != null && fastForwardTimer.isRunning()) fastForwardTimer.stop();
                        if (holdSpaceTimer != null) holdSpaceTimer.stop();
                    }
                    repaint();
                    return;
                }
                if (pauseMenuVisible) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) e.consume();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    e.consume();
                    if (spaceDownPhysical) return;
                    spaceDownPhysical = true;
                    spaceKeyHeld = true;
                    if (phase == PHASE_CREDITS || phase == PHASE_FINAL) {
                        advanceOneStep();
                    } else {
                        tryRevealOrAdvance();
                    }
                    if (holdSpaceTimer != null && !holdSpaceTimer.isRunning()) {
                        holdSpaceTimer.setInitialDelay(SPACE_HOLD_MS);
                        holdSpaceTimer.start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
                e.consume();
                spaceDownPhysical = false;
                spaceKeyHeld = false;
                fastForwardActive = false;
                if (fastForwardTimer != null && fastForwardTimer.isRunning()) fastForwardTimer.stop();
                if (holdSpaceTimer != null) holdSpaceTimer.stop();
            }
        });
    }

    private void initMouse() {
        addMouseMotionListener(new MouseMotionAdapter() {
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
        });
        addMouseListener(new MouseAdapter() {
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
                        int choice = JOptionPane.showConfirmDialog(DragonReunionEndingScene.this,
                            "Are you sure you want to quit?",
                            "Quit to Title",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                        if (choice == JOptionPane.YES_OPTION && onCompleteToTitle != null) {
                            pauseMenuVisible = false;
                            finishToTitle();
                        }
                    } else if (!pauseSaveBounds.contains(p) && !pauseLoadBounds.contains(p) && !pauseSettingsBounds.contains(p)
                        && !pauseHistoryBounds.contains(p) && !pauseQuitBounds.contains(p)) {
                        pauseMenuVisible = false;
                    }
                    pressedPauseSave = pressedPauseLoad = pressedPauseSettings = pressedPauseHistory = pressedPauseQuit = false;
                    repaint();
                    return;
                }
                if (phase == PHASE_MAIN || phase == PHASE_EXTRA) {
                    tryRevealOrAdvance();
                } else if (phase == PHASE_CREDITS || phase == PHASE_FINAL) {
                    advanceOneStep();
                }
            }
        });
    }

    private void handleSave() {
        StoryState state = GameState.getState();
        syncProgressToStoryState(state);
        int lastSlot = state.getLastUsedSaveSlot();
        JDialog dialog = new JDialog(dialogOwner(), "Save (last used: Slot " + lastSlot + ")", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        for (int i = 1; i <= 8; i++) {
            int slot = i;
            JButton b = new JButton("Slot " + slot);
            b.addActionListener(ev -> {
                state.setLastUsedSaveSlot(slot);
                try {
                    SaveLoad.save(state, "saves/slot" + slot + ".dat");
                    JOptionPane.showMessageDialog(DragonReunionEndingScene.this, "Saved to slot " + slot);
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DragonReunionEndingScene.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                requestSceneFocusLater();
            }
        });
        dialog.setVisible(true);
        requestSceneFocusLater();
    }

    private void handleLoad() {
        JDialog dialog = new JDialog(dialogOwner(), "Load", Dialog.ModalityType.APPLICATION_MODAL);
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
                        JOptionPane.showMessageDialog(DragonReunionEndingScene.this, "Empty slot " + slot);
                        return;
                    }
                    int savedCh = loaded.getSavedChapter();
                    if (savedCh == 2) {
                        JOptionPane.showMessageDialog(DragonReunionEndingScene.this, "Chapter 2 is not available yet.", "Not Implemented", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    GameState.setState(loaded);
                    dialog.dispose();
                    pauseMenuVisible = false;
                    if (navigateAfterLoad != null) navigateAfterLoad.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DragonReunionEndingScene.this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                requestSceneFocusLater();
            }
        });
        dialog.setVisible(true);
        requestSceneFocusLater();
    }

    private void handleSettings() {
        float vol = StartScene.getMasterVolume();
        JDialog d = new JDialog(dialogOwner(), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
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
            StartScene.applyVolumeToClipForSceneNoFloor(mainBgmClip, true);
            StartScene.applyVolumeToClipForSceneNoFloor(creditsBgmClip, true);
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
        for (DialogueRecord r : epilogueHistory) {
            if (sb.length() > 0) sb.append("\n");
            String s = r.getSpeaker();
            if (s == null || s.isEmpty()) sb.append(r.getText());
            else sb.append(s).append(": ").append(r.getText());
        }
        JDialog d = new JDialog(dialogOwner(), "History", Dialog.ModalityType.APPLICATION_MODAL);
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
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                requestSceneFocusLater();
            }
        });
        d.setVisible(true);
        requestSceneFocusLater();
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
            String hint = "Place goodending.jpg / goodending.png (or goodending.cg) in image/Chapter Three/";
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

        g2.setColor(new Color(120, 120, 120));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Click / Space | Hold Space to skip | ESC menu", w - 380, h - 12);

        if (pauseMenuVisible) paintPauseMenu(g2, w, h);
        g2.dispose();
    }

    private void drawMainDialogue(Graphics2D g2, int w, int h) {
        if (mainLineIndex >= DragonReunionEndingData.MAIN_LINES.length) return;
        String[] row = DragonReunionEndingData.MAIN_LINES[mainLineIndex];
        String who = row[0];
        String text = row[1];
        int vis = computeVisibleChars(text);
        String shown = text.substring(0, Math.min(vis, text.length()));

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
        drawWrapped(g2, shown, pad + 8, y, w - 2 * pad - 16, 22);
    }

    private void drawCredits(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.94f));
        g2.setColor(new Color(8, 8, 12));
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        String[][] pages = DragonReunionEndingData.CREDIT_PAGES;
        if (pages == null || pages.length == 0) {
            g2.setColor(new Color(200, 200, 210));
            g2.drawString("(No credits data.)", 32, 48);
            return;
        }
        int pi = creditPageIndex;
        if (pi < 0 || pi >= pages.length) pi = 0;
        String[] page = pages[pi];
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
        String text = row[1];
        int vis = computeVisibleChars(text);
        String shown = text.substring(0, Math.min(vis, text.length()));
        int boxY = h / 2 - 40;
        g2.setColor(new Color(240, 240, 235));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.drawString(row[0], 40, boxY);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 17f));
        drawWrapped(g2, shown, 40, boxY + 28, w - 80, 24);
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

    private void startAudioForPhaseFromRestore() {
        stopMainBgm();
        stopCreditsBgm();
        if (phase == PHASE_MAIN) {
            mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.wav");
            if (mainBgmClip == null) mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.mp3");
            if (mainBgmClip != null) {
                StartScene.applyVolumeToClipForSceneNoFloor(mainBgmClip, true);
                mainBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                mainBgmClip.start();
            }
        } else if (phase == PHASE_CREDITS) {
            creditsBgmClip = StartScene.loadMusicFromMusicDir("rickroll.wav");
            if (creditsBgmClip == null) creditsBgmClip = StartScene.loadMusicFromMusicDir("rickroll.mp3");
            if (creditsBgmClip != null) {
                StartScene.applyVolumeToClipForSceneNoFloor(creditsBgmClip, true);
                creditsBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                creditsBgmClip.start();
            }
        }
    }

    @Override
    public void onEnter() {
        holdSpaceTimer.stop();
        fastForwardTimer.stop();
        spaceKeyHeld = false;
        spaceDownPhysical = false;
        fastForwardActive = false;
        pauseMenuVisible = false;
        finishingToTitle = false;

        if (restoreOnNextEnter) {
            restoreOnNextEnter = false;
            StoryState st = GameState.getState();
            phase = Math.max(PHASE_MAIN, Math.min(PHASE_FINAL, st.getDragonEpiloguePhase()));
            mainLineIndex = st.getDragonEpilogueMainIndex();
            creditPageIndex = st.getDragonEpilogueCreditIndex();
            extraLineIndex = st.getDragonEpilogueExtraIndex();
            int nMain = DragonReunionEndingData.MAIN_LINES.length;
            int nCred = DragonReunionEndingData.CREDIT_PAGES.length;
            int nExtra = DragonReunionEndingData.EXTRA_LINES.length;
            if (nMain > 0) mainLineIndex = Math.max(0, Math.min(nMain - 1, mainLineIndex));
            else mainLineIndex = 0;
            if (nCred > 0) creditPageIndex = Math.max(0, Math.min(nCred - 1, creditPageIndex));
            else creditPageIndex = 0;
            if (nExtra > 0) extraLineIndex = Math.max(0, Math.min(nExtra - 1, extraLineIndex));
            else extraLineIndex = 0;
            rebuildHistoryAfterRestore();
            lineStartTime = System.currentTimeMillis();
            startAudioForPhaseFromRestore();
        } else {
            phase = PHASE_MAIN;
            mainLineIndex = 0;
            creditPageIndex = 0;
            extraLineIndex = 0;
            epilogueHistory.clear();
            lastRecordedMainIndex = -1;
            lastRecordedExtraIndex = -1;
            lineStartTime = System.currentTimeMillis();
            stopCreditsBgm();
            mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.wav");
            if (mainBgmClip == null) mainBgmClip = StartScene.loadMusicFromMusicDir("cg2.mp3");
            if (mainBgmClip != null) {
                StartScene.applyVolumeToClipForSceneNoFloor(mainBgmClip, true);
                mainBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                mainBgmClip.start();
            }
        }
        updateTypewriterTimerRunning();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        holdSpaceTimer.stop();
        fastForwardTimer.stop();
        typewriterRepaintTimer.stop();
        spaceKeyHeld = false;
        fastForwardActive = false;
        stopMainBgm();
        stopCreditsBgm();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}

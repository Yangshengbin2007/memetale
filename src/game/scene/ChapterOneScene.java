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
import javax.sound.sampled.*;

/**
 * 游戏第一章场景。底部对话点击推进，CG 随剧情切换，结尾淡入淡出 "Chapter 1 Meme Forest"。
 * 存档含位置与 History，读档从同一位置继续。ESC 暂停：存档/读档/设置/历史/返回主菜单。
 */
@SuppressWarnings("fallthrough")
public class ChapterOneScene extends JPanel implements Scene {
    private final Runnable onQuitToTitle;
    /** 第一章结尾黑屏 phase 7 点击 Continue 后进入森林入口，可为 null */
    private final Runnable onChapterOneComplete;
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

    /** 当前使用的存档槽位（与 StoryState 同步，存/读同一槽）；存档对话框标题显示。 */
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
    private static final int TEXT_DURATION_MS = 1000;
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

    /** Last line index that was added to history (so we record once when typewriter finishes that line). */
    private int lastLineRecordedToHistory = -1;

    /** 第一章字幕播完后黑屏，预留点击交互地图（之后实现） */
    private boolean chapterEndBlackScreen = false;

    /** CG BGM: 1=cg1, 2=cg2, 3=cg3. Only used when actually playing (not during enter/quote). */
    private Clip cgBgmClip;

    /** Post-chapter sequence: 黑屏 2s -> title.png + begin.wav -> title 淡出 -> "Chapter One Meme Forest" 文字 + chapteronesound.wav -> 文字淡出音效停 -> 黑屏 */
    private int postChapterPhase = 0;  // 0=inactive, 1=black wait 1s, 2=show title+begin.wav, 3=title fade out, 4=chapter text+chapteronesound.wav fade in, 5=hold, 6=fade out, 7=black map
    private long postChapterStartTime;
    private Image titleStickerImage;
    private Clip beginOggClip;
    private Clip chapterOneSoundClip;
    private float postChapterAlpha = 0f;
    private static final int POST_BLACK_WAIT_MS = 2000;
    private static final int POST_TITLE_FADE_MS = 600;
    /** CG3+文字框一起淡出：淡出期间 active=true，淡完黑屏2秒再显示 title */
    private boolean chapterEndFadeOutActive = false;
    private float chapterEndFadeOutAlpha = 1f;
    private long chapterEndFadeOutStartTime;
    private static final int CHAPTER_END_FADEOUT_MS = 1000;
    private javax.swing.Timer chapterEndFadeOutTimer;
    private static final int POST_CHAPTER_FADE_MS = 800;
    private static final int POST_CHAPTER_HOLD_MS = 1500;
    private javax.swing.Timer postChapterTimer;

    public ChapterOneScene(Runnable onQuitToTitle, Runnable onChapterOneComplete) {
        this.onQuitToTitle = onQuitToTitle;
        this.onChapterOneComplete = onChapterOneComplete;
        setBackground(Color.BLACK);
        loadBackgroundForDialogueIndex(0);
        initEnterFadeTimer();
        initTitleTimer();
        initSceneFadeTimer();
        initPostChapterTimer();
        initEndFadeOutTimer();
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
            if (idx >= ChapterOneData.LINES.length || chapterTitlePhase != 0 || chapterEndFadeOutActive) {
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
                    startPostChapterSequence();
                }
            }
            repaint();
        });
    }

    private void initPostChapterTimer() {
        postChapterTimer = new javax.swing.Timer(40, e -> {
            if (postChapterPhase == 0) return;
            long elapsed = System.currentTimeMillis() - postChapterStartTime;
            switch (postChapterPhase) {
                case 1:
                    if (elapsed >= POST_BLACK_WAIT_MS) {
                        postChapterPhase = 2;
                        postChapterStartTime = System.currentTimeMillis();
                        loadTitleStickerAndBeginOgg();
                    }
                    break;
                case 2:
                    if (beginOggClip != null && !beginOggClip.isRunning()) {
                        postChapterPhase = 3;
                        postChapterStartTime = System.currentTimeMillis();
                    } else if (beginOggClip == null && elapsed > 500) {
                        postChapterPhase = 3;
                        postChapterStartTime = System.currentTimeMillis();
                    }
                    break;
                case 3:
                    postChapterAlpha = Math.max(0f, 1f - (elapsed / (float) POST_TITLE_FADE_MS));
                    if (postChapterAlpha <= 0f) {
                        postChapterPhase = 4;
                        postChapterStartTime = System.currentTimeMillis();
                        playChapterOneSoundAndShowText();
                    }
                    break;
                case 4:
                    postChapterAlpha = Math.min(1f, elapsed / (float) POST_CHAPTER_FADE_MS);
                    if (postChapterAlpha >= 1f) {
                        postChapterPhase = 5;
                        postChapterStartTime = System.currentTimeMillis();
                    }
                    break;
                case 5:
                    if (elapsed >= POST_CHAPTER_HOLD_MS) {
                        postChapterPhase = 6;
                        postChapterStartTime = System.currentTimeMillis();
                    }
                    break;
                case 6:
                    postChapterAlpha = Math.max(0f, 1f - (elapsed / (float) POST_CHAPTER_FADE_MS));
                    if (postChapterAlpha <= 0f) {
                        postChapterPhase = 7;
                        stopChapterOneSound();
                        if (postChapterTimer != null) postChapterTimer.stop();
                        scheduleTransitionToForest();
                    }
                    break;
                default:
                    break;
            }
            repaint();
        });
    }

    private static final int FOREST_TRANSITION_DELAY_MS = 1800;

    private void scheduleTransitionToForest() {
        if (onChapterOneComplete == null) return;
        javax.swing.Timer t = new javax.swing.Timer(FOREST_TRANSITION_DELAY_MS, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            if (onChapterOneComplete != null) onChapterOneComplete.run();
        });
        t.setRepeats(false);
        t.start();
    }

    private void startPostChapterSequence() {
        postChapterPhase = 1;
        postChapterStartTime = System.currentTimeMillis();
        if (postChapterTimer != null && !postChapterTimer.isRunning())
            postChapterTimer.start();
    }

    private void initEndFadeOutTimer() {
        chapterEndFadeOutTimer = new javax.swing.Timer(40, e -> {
            if (!chapterEndFadeOutActive) return;
            long elapsed = System.currentTimeMillis() - chapterEndFadeOutStartTime;
            chapterEndFadeOutAlpha = Math.max(0f, 1f - elapsed / (float) CHAPTER_END_FADEOUT_MS);
            if (chapterEndFadeOutAlpha <= 0f) {
                chapterEndFadeOutActive = false;
                if (chapterEndFadeOutTimer != null) chapterEndFadeOutTimer.stop();
                GameState.getState().setChapterOneDialogueIndex(ChapterOneData.LINES.length);
                chapterEndBlackScreen = true;
                startPostChapterSequence();
            }
            repaint();
        });
    }

    /** CG3 与文字框一起淡出，淡完后黑屏 2 秒再显示 title；同时停止 CG3 音乐 */
    private void startEndFadeOut() {
        stopCgBgm();
        chapterEndFadeOutActive = true;
        chapterEndFadeOutAlpha = 1f;
        chapterEndFadeOutStartTime = System.currentTimeMillis();
        if (chapterEndFadeOutTimer != null && !chapterEndFadeOutTimer.isRunning())
            chapterEndFadeOutTimer.start();
    }

    private void loadTitleStickerAndBeginOgg() {
        titleStickerImage = loadImageFromStickers("title.png");
        beginOggClip = loadAudioClip("sound/begin.wav", false);
        if (beginOggClip != null) {
            applyChapterVolumeToClip(beginOggClip, false);
            beginOggClip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP && postChapterPhase == 2) {
                    SwingUtilities.invokeLater(() -> {
                        postChapterPhase = 3;
                        postChapterStartTime = System.currentTimeMillis();
                    });
                }
            });
            beginOggClip.start();
        }
        postChapterAlpha = 1f;
    }

    private Image loadImageFromStickers(String filename) {
        URL url = getClass().getResource("/Stickers/" + filename);
        if (url != null)
            return new ImageIcon(url).getImage();
        File f = new File("Stickers/" + filename);
        if (f.exists())
            return new ImageIcon(f.getAbsolutePath()).getImage();
        return null;
    }

    private void playChapterOneSoundAndShowText() {
        chapterOneSoundClip = loadAudioClip("sound/chapteronesound.wav", false);
        if (chapterOneSoundClip != null) {
            applyChapterVolumeToClip(chapterOneSoundClip, false);
            chapterOneSoundClip.start();
        }
        postChapterAlpha = 0f;
    }

    private void stopChapterOneSound() {
        if (chapterOneSoundClip != null) {
            try {
                if (chapterOneSoundClip.isRunning()) chapterOneSoundClip.stop();
                chapterOneSoundClip.close();
            } catch (Exception ignore) {}
            chapterOneSoundClip = null;
        }
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
                    playCgBgm(ChapterOneData.cgIndexForLine(GameState.getState().getChapterOneDialogueIndex()));
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
        if (enterFadeAlpha <= 0f && quoteTextAlpha <= 0f && !chapterEndFadeOutActive)
            playCgBgm(cg);
    }

    /** Play BGM for CG index (1=cg1 birdsound.wav, 2=cg2.wav, 3=cg3.wav). Respects Settings volume and Mute all music. */
    @SuppressWarnings("fallthrough")
    private void playCgBgm(int cgIndex) {
        stopCgBgm();
        String path;
        if (cgIndex == 1) path = "music/cg1 birdsound.wav";
        else if (cgIndex == 2) path = "music/cg2.wav";
        else if (cgIndex == 3) path = "music/cg3.wav";
        else return;
        cgBgmClip = loadAudioClip(path, true);
        if (cgBgmClip != null) {
            applyChapterVolumeToClip(cgBgmClip, true);
            cgBgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            cgBgmClip.start();
        }
    }

    private void stopCgBgm() {
        if (cgBgmClip != null) {
            try {
                if (cgBgmClip.isRunning()) cgBgmClip.stop();
                cgBgmClip.close();
            } catch (Exception ignore) {}
            cgBgmClip = null;
        }
    }

    /** Load audio from resource or file. Supports WAV; MP3/OGG need SPI on classpath. Returns null if unsupported or missing. */
    private Clip loadAudioClip(String path, boolean fromMusic) {
        try {
        String rel = path.contains("/") ? path.substring(path.indexOf('/') + 1) : path;
            URL url = getClass().getResource((fromMusic ? "/music/" : "/sound/") + rel);
            AudioInputStream ais = null;
            if (url != null)
                ais = AudioSystem.getAudioInputStream(url);
            if (ais == null) {
                File f = new File(path);
                if (f.exists())
                    ais = AudioSystem.getAudioInputStream(f);
            }
            if (ais == null) return null;
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    /** Apply Settings volume and mute to a clip (for chapter BGM/SFX). */
    private void applyChapterVolumeToClip(Clip clip, boolean isMusic) {
        if (clip == null) return;
        float vol = StartScene.getMasterVolume() * (isMusic ? (StartScene.getMuteAllMusic() ? 0f : 1f) : (StartScene.getMuteAllSoundEffects() ? 0f : 1f));
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (vol <= 0f) gain.setValue(gain.getMinimum());
                else {
                    float dB = (float) (20.0 * Math.log10(vol));
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
            }
        } catch (Exception ignore) {}
    }

    public void updateChapterVolumes() {
        applyChapterVolumeToClip(cgBgmClip, true);
        applyChapterVolumeToClip(beginOggClip, false);
        applyChapterVolumeToClip(chapterOneSoundClip, false);
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
        int displayIdx = chapterEndFadeOutActive ? ChapterOneData.LINES.length - 1 : idx;
        if (!chapterEndFadeOutActive)
            loadBackgroundForDialogueIndex(idx);

        boolean inDialogue = displayIdx < ChapterOneData.LINES.length && chapterTitlePhase == 0;
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
            if (chapterEndFadeOutActive) {
                String lineText = ChapterOneData.LINES[displayIdx][1];
                paintDialogueBox(g2, w, h, displayIdx, lineText.length());
            } else {
                long elapsed = lineStartTime > 0 ? System.currentTimeMillis() - lineStartTime : TEXT_DURATION_MS;
                String lineText = ChapterOneData.LINES[displayIdx][1];
                int totalChars = lineText.length();
                int visibleChars = totalChars == 0 ? 0 : (int) Math.min(totalChars, (long) totalChars * elapsed / TEXT_DURATION_MS);
                ensureLineRecordedToHistoryWhenComplete(displayIdx, visibleChars, totalChars);
                paintDialogueBox(g2, w, h, displayIdx, visibleChars);
                if (visibleChars < totalChars)
                    repaint(TEXT_ANIM_DELAY_MS);
            }
        }
        if (sceneFadeAlpha < 1f)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // CG3+文字框淡出：黑 overlay 随 chapterEndFadeOutAlpha 从 0 到 1
        if (chapterEndFadeOutActive && chapterEndFadeOutAlpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - chapterEndFadeOutAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 章节标题淡入淡出（已废弃：结尾改为先 title 再 chapter 文字，此处保留兼容）
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
            if (postChapterPhase >= 1) {
                if (postChapterPhase == 2 || postChapterPhase == 3) {
                    if (titleStickerImage != null && postChapterAlpha > 0f) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, postChapterAlpha));
                        int iw = titleStickerImage.getWidth(null);
                        int ih = titleStickerImage.getHeight(null);
                        if (iw > 0 && ih > 0) {
                            double scale = Math.min((double) w / iw, (double) h / ih);
                            int sw = (int) (iw * scale);
                            int sh = (int) (ih * scale);
                            int x = (w - sw) / 2;
                            int y = (h - sh) / 2;
                            g2.drawImage(titleStickerImage, x, y, sw, sh, null);
                        }
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                    } else if (postChapterPhase == 3 && postChapterAlpha <= 0f)
                        g2.setColor(Color.BLACK);
                } else if (postChapterPhase >= 4 && postChapterPhase <= 6 && postChapterAlpha > 0f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, postChapterAlpha));
                    g2.setColor(Color.WHITE);
                    Font f = g2.getFont().deriveFont(Font.BOLD, 36f);
                    g2.setFont(f);
                    FontMetrics fm = g2.getFontMetrics();
                    String txt = "Chapter One Meme Forest";
                    g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, h / 2 + fm.getAscent() / 2);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }
            if (postChapterPhase == 7) {
                g2.setColor(new Color(200, 220, 180));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
                FontMetrics fm = g2.getFontMetrics();
                String txt = "Heading to the forest...";
                g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, h / 2 + 20);
            }
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

            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (pauseMenuVisible) {
                    pressedPauseSave = pauseSaveBounds.contains(p);
                    pressedPauseLoad = pauseLoadBounds.contains(p);
                    pressedPauseSettings = pauseSettingsBounds.contains(p);
                    pressedPauseHistory = pauseHistoryBounds.contains(p);
                    pressedPauseQuit = pauseQuitBounds.contains(p);
                }
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
                if (enterFadeAlpha <= 0f && quoteTextAlpha <= 0f && !chapterEndFadeOutActive) {
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
                }
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void handleSave() {
        StoryState state = GameState.getState();
        state.setSavedChapter(1);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Save (last used: Slot " + lastUsedSaveSlot + ")", Dialog.ModalityType.APPLICATION_MODAL);
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
                startPostChapterSequence();
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
                        chapterEndBlackScreen = true;
                        startPostChapterSequence();
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
                startPostChapterSequence();
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
        if (idx >= ChapterOneData.LINES.length || chapterTitlePhase != 0 || chapterEndFadeOutActive) return;
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

    /** When the current line is fully shown (typewriter complete), record it to history once. */
    private void ensureLineRecordedToHistoryWhenComplete(int lineIndex, int visibleChars, int totalChars) {
        if (lineIndex < 0 || lineIndex >= ChapterOneData.LINES.length) return;
        if (lastLineRecordedToHistory == lineIndex) return;
        if (totalChars == 0 || visibleChars >= totalChars) {
            String[] line = ChapterOneData.LINES[lineIndex];
            GameState.getState().addChapterOneHistory(line[0], line[1]);
            lastLineRecordedToHistory = lineIndex;
        }
    }

    /** 推进一条对话；若到达最后一句则启动 CG3+文字框淡出（不播第一次章节标题文字），淡完黑屏 2 秒再 title -> chapter 文字+音效 -> 黑屏。 */
    private void advanceDialogue() {
        StoryState state = GameState.getState();
        int idx = state.getChapterOneDialogueIndex();
        if (idx >= ChapterOneData.LINES.length) return;
        if (idx + 1 >= ChapterOneData.LINES.length) {
            startEndFadeOut();
            return;
        }
        state.setChapterOneDialogueIndex(idx + 1);
        lastLineRecordedToHistory = -1;
        lineStartTime = System.currentTimeMillis();
        loadBackgroundForDialogueIndex(idx + 1);
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
        d.setVisible(true);
    }

    private void handleSettings() {
        float vol = StartScene.getMasterVolume();
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
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
            updateChapterVolumes();
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

    @Override
    public void onEnter() {
        boolean skipIntro = skipQuoteNextEnter;
        if (skipQuoteNextEnter) skipQuoteNextEnter = false;

        StoryState state = GameState.getState();
        lastUsedSaveSlot = state.getLastUsedSaveSlot();
        int idx = state.getChapterOneDialogueIndex();
        loadBackgroundForDialogueIndex(idx);
        lineStartTime = System.currentTimeMillis();

        if (skipIntro) {
            enterFadeAlpha = 0f;
            quoteTextAlpha = 0f;
            chapterEndFadeOutActive = false;
            if (idx >= ChapterOneData.LINES.length) {
                chapterTitlePhase = 4;
                chapterEndBlackScreen = true;
                sceneFadeAlpha = 1f;
                startPostChapterSequence();
            } else {
                chapterTitlePhase = 0;
                chapterEndBlackScreen = false;
                startSceneFade();
            }
        } else {
            if (idx >= ChapterOneData.LINES.length) {
                chapterTitlePhase = 4;
                chapterEndBlackScreen = true;
                startPostChapterSequence();
                enterFadeAlpha = 0f;
                quoteTextAlpha = 0f;
            } else {
                chapterTitlePhase = 0;
                chapterEndBlackScreen = false;
                chapterEndFadeOutActive = false;
                enterFadeAlpha = 1f;
                quoteTextAlpha = 0f;
                enterFadeStartTime = System.currentTimeMillis();
                if (enterFadeTimer != null && !enterFadeTimer.isRunning())
                    enterFadeTimer.start();
            }
        }
        if (titleTimer != null && titleTimer.isRunning())
            titleTimer.stop();
        requestFocusInWindow();
    }

    @Override
    public void onExit() {
        stopCgBgm();
        if (beginOggClip != null) {
            try { beginOggClip.stop(); beginOggClip.close(); } catch (Exception ignore) {}
            beginOggClip = null;
        }
        stopChapterOneSound();
        if (enterFadeTimer != null && enterFadeTimer.isRunning())
            enterFadeTimer.stop();
        if (titleTimer != null && titleTimer.isRunning())
            titleTimer.stop();
        if (postChapterTimer != null && postChapterTimer.isRunning())
            postChapterTimer.stop();
        if (chapterEndFadeOutTimer != null && chapterEndFadeOutTimer.isRunning())
            chapterEndFadeOutTimer.stop();
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

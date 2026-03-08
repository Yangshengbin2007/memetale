package game.model;

/**
 * 第一章对话与 CG 分段数据。CG1=阳台，CG2=龙出现，CG3=国王给任务。
 * 点击推进对话，按索引切换 cg1/cg2/cg3，最后淡入淡出 "Chapter 1 Meme Forest"。
 */
public final class ChapterOneData {
    private ChapterOneData() {}

    /** 每条对话 [0]=speaker, [1]=text。舞台说明用 speaker "" */
    public static final String[][] LINES = {
        // CG1 Balcony Scene
        {"Princess", "What a beautiful day."},
        {"Prince", "Yeah."},
        {"Princess", "The sun is shining."},
        {"Prince", "Yeah."},
        {"Princess", "The birds are singing."},
        {"Prince", "Yeah."},
        {"Princess", "Are you okay?"},
        {"Prince", "I'm just waiting for the plot to happen."},
        {"Princess", "What plot?"},
        {"Prince", "The one where our peaceful life gets ruined."},
        {"Princess", "Relax."},
        {"Princess", "This isn't a video game."},
        {"Prince", "We're literally standing in front of a dialogue box."},
        {"Princess", "Stop being dramatic."},
        {"Prince", "I'm not dramatic."},
        {"Prince", "I'm genre-aware."},
        {"Princess", "Look around."},
        {"Princess", "Everything is peaceful."},
        {"Prince", "That's exactly what characters say five seconds before something explodes."},
        {"Princess", "Nothing is going to explode."},
        {"Prince", "You just jinxed it."},
        {"Princess", "Imagine if a dragon suddenly appeared."},
        {"Prince", "WHY WOULD YOU SAY THAT."},
        {"Princess", "Relax."},
        {"Princess", "Dragons aren't real."},
        {"Prince", "We live in a kingdom called Memetale."},
        {"Prince", "Our economy runs on jokes."},
        {"Princess", "Fair point."},
        {"Prince", "You know what would be funny?"},
        {"Princess", "What?"},
        {"Prince", "If the dragon shows up and immediately kidnaps you."},
        {"Princess", "That would be very inconvenient."},
        {"", "LOUD ROAR"},
        {"Princess", "…"},
        {"Prince", "…"},
        {"Princess", "Did you hear that?"},
        {"Prince", "I regret everything."},
        // CG2 Dragon Appears
        {"Dragon", "HELLO MORTALS."},
        {"Prince", "Oh cool."},
        {"Prince", "A dragon."},
        {"Princess", "This feels like the start of a side quest."},
        {"Dragon", "I HAVE COME FOR THE PRINCESS."},
        {"Prince", "Of course you have."},
        {"Prince", "Couldn't you kidnap someone else?"},
        {"Dragon", "THE SCRIPT SAYS PRINCESS."},
        {"Prince", "Understandable."},
        {"Princess", "Why me?"},
        {"Dragon", "ALGORITHM."},
        {"Princess", "Excuse me?"},
        {"Dragon", "YOU ARE CURRENTLY TRENDING."},
        {"Prince", "This feels like social media."},
        {"Dragon", "CORRECT."},
        {"Dragon", "YOU HAVE 3 MILLION VIEWS."},
        {"Princess", "I DIDN'T EVEN POST ANYTHING."},
        {"Dragon", "THE INTERNET WORKS IN MYSTERIOUS WAYS."},
        {"Prince", "Take me instead!"},
        {"Dragon", "WHY."},
        {"Prince", "Because I'm the protagonist."},
        {"Dragon", "EXACTLY."},
        {"Dragon", "THAT'S WHY YOU HAVE PLOT ARMOR."},
        {"Princess", "This is the worst date ever."},
        {"Prince", "Technically it's still better than my last Tinder match."},
        {"", "Dragon grabs princess"},
        {"Princess", "HELP!"},
        {"Prince", "WAIT!"},
        {"Prince", "I DIDN'T SAVE MY GAME!"},
        {"Dragon", "SKILL ISSUE."},
        {"", "Dragon flies away"},
        {"Prince", "Well."},
        {"Prince", "That escalated quickly."},
        {"Prince", "Like a Twitter argument."},
        {"Prince", "Also what was that number he yelled earlier?"},
        {"Princess", "What number?"},
        {"Prince", "I swear he said 67."},
        {"Princess", "What does that mean?"},
        {"Prince", "Nobody knows."},
        {"Prince", "And that's why it's funny."},
        // CG3 King Gives Quest
        {"King", "My son."},
        {"Prince", "Hi dad."},
        {"King", "The princess has been kidnapped."},
        {"Prince", "Yes."},
        {"Prince", "That tends to happen when dragons appear."},
        {"King", "You must rescue her."},
        {"Prince", "Do I get an army?"},
        {"King", "No."},
        {"Prince", "A sword?"},
        {"King", "No."},
        {"Prince", "Armor?"},
        {"King", "No."},
        {"Prince", "A helpful tutorial?"},
        {"King", "You get Lagalot."},
        {"Prince", "That sounds worse."},
        {"King", "Your journey begins in the forest."},
        {"Prince", "Every RPG begins in a forest."},
        {"King", "It's tradition."},
        {"Prince", "Is there a map?"},
        {"King", "No."},
        {"Prince", "A guide?"},
        {"King", "No."},
        {"Prince", "Instructions?"},
        {"King", "Click things."},
        {"Prince", "That's your plan?"},
        {"King", "That's every game developer's plan."},
        {"Prince", "What if I die?"},
        {"King", "Then you reload."},
        {"Prince", "Fair enough."},
        {"Prince", "Before I go…"},
        {"Prince", "One question."},
        {"King", "Yes?"},
        {"Prince", "Why did the dragon yell 67?"},
        {"King", "Ah."},
        {"King", "Ancient prophecy."},
        {"Prince", "What does it mean?"},
        {"King", "Nobody knows."},
        {"King", "And that's why it's terrifying."},
        {"King", "Now go save the princess!"},
        {"Prince", "Fine."},
        {"Prince", "But if I get rickrolled out there I'm quitting."},
        {"King", "That is a risk we are willing to take."},
    };

    /** CG1 结束位置（不含），即 lineIndex < CG1_END 为 cg1 */
    public static final int CG1_END = 37;
    /** CG2 结束位置（不含） */
    public static final int CG2_END = 88;
    /** 对话结束后显示的章节标题 */
    public static final String CHAPTER_TITLE = "Chapter 1 Meme Forest";

    public static int cgIndexForLine(int lineIndex) {
        if (lineIndex < CG1_END) return 1;
        if (lineIndex < CG2_END) return 2;
        return 3;
    }

    public static String bgFileForCg(int cgIndex) {
        switch (cgIndex) {
            case 1: return "cg1.jpg";
            case 2: return "cg2.jpg";
            case 3: return "cg3.jpg";
            default: return "cg1.jpg";
        }
    }
}

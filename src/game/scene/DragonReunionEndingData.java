package game.scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapter 3 epilogue script: "After the dragon · reunion" + credits pages (plain lines per page).
 * Lines use {@code Speaker|text}; empty speaker = stage direction.
 */
public final class DragonReunionEndingData {
    private DragonReunionEndingData() {}

    private static final String MAIN_BLOCK = """
| (Scene: Dragon mountain peak. The dragon falls. The sky clears.)
Prince|…Is it over?
Darabongba|If it gets back up, I'm resigning.
| (The dragon dissolves completely.)
Prince|We… actually did it.
Darabongba|I would like to formally apologize to myself. I did not believe in us.
Prince|You never do.
Darabongba|And yet I am still here. Unfortunate.
| (A figure appears in the distance.)
Prince|…Wait.
Darabongba|If that's another phase—
Prince|No.
Prince|It's her.
| (The princess stands at the cliff edge. Wind blows.)
Princess|You're late.
Prince|…Late?!
Darabongba|We fought a dragon!
Princess|Yes. I saw.
Darabongba|YOU SAW?!
Prince|You were watching?!
Princess|Of course.
Prince|Why didn't you help?!
Princess|I wanted to see if you could do it.
Darabongba|We almost died for character development?!
Princess|Exactly.
Prince|…We came all this way to rescue you.
Princess|Rescue?
Prince|Yeah.
Princess|From what?
| (A short pause.)
Prince|…The dragon?
Princess|I wasn't trapped.
Darabongba|I KNEW IT.
Prince|You knew nothing.
Princess|I came here on purpose.
Prince|Why?!
Princess|To test you.
Darabongba|We are lab rats.
Prince|Test me… for what?
Princess|To see if you could actually finish something.
Darabongba|That's fair.
Prince|Hey!
Princess|You used to give up.
Prince|That was… strategic retreat.
Darabongba|You ran away from a tutorial once.
Prince|It was hard!
Princess|But this time… you didn't.
| (A short pause.)
Prince|…Yeah. I didn't.
Princess|You changed.
Darabongba|He still makes bad decisions.
Princess|I expected that.
Prince|So… what now?
Princess|Now?
Princess|Now we go back.
Prince|Just like that?
Princess|Unless you want another dragon.
Darabongba|NO.
Prince|Wait.
Prince|That's it?
Princess|What did you expect?
Prince|I don't know… A celebration? A speech?
Darabongba|A credit roll?
Princess|Fine.
| (She clears her throat.)
Princess|Congratulations. You did the bare minimum.
Prince|…Wow.
Darabongba|She's brutal.
Princess|But you finished the journey. That matters.
| (She smiles.)
Prince|…Yeah.
Darabongba|So we're just… done?
Princess|For now.
Prince|For now?
Princess|You think this was the whole story?
Darabongba|I was hoping.
Princess|There's more.
| (Wind rises. The view pulls back.)
Princess|There's always more.
Darabongba|I regret everything.
Prince|Come on. Let's go home.
Darabongba|Do we at least get food?
Princess|No.
Darabongba|Unbelievable.
| (The picture fades to dark.)
""";

    public static final String[][] MAIN_LINES = parseLines(MAIN_BLOCK);

    /** Credits pages while rickroll.wav plays; each page is several lines (same column = one line of text). */
    public static final String[][] CREDIT_PAGES = {
        {
            "Chapter Complete",
            "But the journey continues…",
            "",
            "References & Attribution"
        },
        {
            "Gameplay inspiration",
            "• Undertale — bullet-hell / battle box ideas",
            "• Chrome Dino — endless runner / dodge",
            "• Classic memory-matching — card flip mechanics",
            "• Red Light, Green Light — stop-and-go movement",
        },
        {
            "Meme & internet culture (parody / transformative)",
            "Doge, Cheems, Troll face, Rickroll — satire context only.",
        },
        {
            "Music references",
            "Never Gonna Give You Up — cultural nod for Rickroll bits.",
            "Retro / meme-style audio elsewhere.",
        },
        {
            "Art & visuals",
            "AI-assisted or edited backgrounds; original or transformed for parody.",
        },
        {
            "Development",
            "Shengbin Yang. Java, editors, debugging tools.",
            "",
            "Legal: no direct redistribution of copyrighted media;",
            "parody, original implementation, inspiration only.",
        },
    };

    /** After credits music stops — short gag, no BGM. */
    public static final String[][] EXTRA_LINES = {
        {"Darabongba", "I would like to credit… stress."},
        {"Prince", "We couldn't have done this without determination."},
        {"Darabongba", "And poor decision-making."},
    };

    private static String[][] parseLines(String block) {
        List<String[]> out = new ArrayList<>();
        for (String line : block.split("\n")) {
            if (line.isEmpty()) continue;
            int sep = line.indexOf('|');
            if (sep < 0) continue;
            String who = line.substring(0, sep).trim();
            String text = line.substring(sep + 1).trim();
            out.add(new String[]{who, text});
        }
        return out.toArray(new String[0][]);
    }
}

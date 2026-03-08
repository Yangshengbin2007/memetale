package game.model;

import java.io.Serializable;

/** 一条对话记录，用于 History 显示与存档。 */
public final class DialogueRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String speaker;
    private final String text;

    public DialogueRecord(String speaker, String text) {
        this.speaker = speaker != null ? speaker : "";
        this.text = text != null ? text : "";
    }

    public String getSpeaker() { return speaker; }
    public String getText() { return text; }
}

package starlords.ui;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import lombok.Setter;
import starlords.util.Constants;

import java.awt.*;
import java.util.List;

public class ToolTip implements TooltipMakerAPI.TooltipCreator {

    private float width;
    private String message;
    private Color color;
    private boolean colorCode;
    private List<String> messages;
    private String codexEntryId;

    public ToolTip(float width, String message) {
        this(width, message, null);
    }

    public ToolTip(float width, String message, Color color) {
        this.width = width;
        this.message = message;
        this.color = color;
    }

    public ToolTip(float width, String message, Color color, String codexEntryId) {
        this(width,message,color);
        this.codexEntryId = codexEntryId;
    }



    public ToolTip(float width, List<String> messages) {
        this(width, messages, false);
    }

    /**
     Color code is specifically for coloring +/- lines for vote breakdown tooltips
     */
    public ToolTip(float width, List<String> messages, boolean colorCode) {
        this.width = width;
        this.messages = messages;
        this.colorCode = colorCode;
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return width;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        if (message != null) {
            if (color != null) {
                tooltip.addPara(message, color, 0);
            } else {
                tooltip.addPara(message, 0);
            }
        } else {
            for (String message : messages) {
                if (colorCode) {
                    Color curr = null;
                    if (message.startsWith("+")) curr = Constants.LIGHT_GREEN;
                    if (message.startsWith("-")) curr = Constants.LIGHT_RED;
                    if (curr != null) {
                        String highlight = message.split(" ")[0];
                        tooltip.addPara(message, 3, curr, highlight);
                    } else {
                        tooltip.addPara(message, 3);
                    }
                } else {
                    tooltip.addPara(message, 3);
                }
            }
        }
    if (codexEntryId != null) {
        tooltip.setCodexEntryId(codexEntryId);
    }

    }
}

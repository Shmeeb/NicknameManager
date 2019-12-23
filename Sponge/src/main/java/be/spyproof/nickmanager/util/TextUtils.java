package be.spyproof.nickmanager.util;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.ShiftClickAction;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TextUtils {
  private static final String NEW_LINE = "\n";

  /**
   * Split a message with every new line
   * @param text to be split
   * @return a list of texts
   */
  public static List<Text> splitLines(Text text) {
    List<Text.Builder> splitBuilders = splitLines(text, 0, null, null, null, null);

    List<Text> texts = new ArrayList<>();
    for (Text.Builder builder : splitBuilders) {
      texts.add(builder.build());
    }

    return texts;
  }

  private static List<Text.Builder> splitLines(Text text, int depth, TextStyle parentStyle, HoverAction parentHoverAction, ClickAction parentClickAction, ShiftClickAction parentShiftClickAction) {
    List<Text.Builder> splitBuilders = new ArrayList<>();

    TextStyle style = processStyle(text.getStyle(), parentStyle);
    TextColor color = text.getColor();
    HoverAction hoverAction = text.getHoverAction().orElse(parentHoverAction);
    ClickAction clickAction = text.getClickAction().orElse(parentClickAction);
    ShiftClickAction shiftClickAction = text.getShiftClickAction().orElse(parentShiftClickAction);
    String t = text.toPlainSingle();
    Collection<String> plainTexts = separateNewLines(text.toPlainSingle());

    for (String plainText : plainTexts) {
      Text.Builder builder = Text.builder(plainText).color(color).style(style);
      if (hoverAction != null) {
        builder.onHover(hoverAction);
      }
      if (clickAction != null) {
        builder.onClick(clickAction);
      }
      if (shiftClickAction != null) {
        builder.onShiftClick(shiftClickAction);
      }
      splitBuilders.add(builder);
    }

    Text.Builder lastBuilder = splitBuilders.get(splitBuilders.size()-1);
    for (Text child : text.getChildren()) {
      List<Text.Builder> childSplit = splitLines(child, depth+1, style, hoverAction, clickAction, shiftClickAction);
      for (int i = 0; i < childSplit.size(); i++) {
        if (i == 0) {
          lastBuilder.append(childSplit.get(i).build());
        } else {
          lastBuilder = childSplit.get(i).color(color).style(style);
          splitBuilders.add(lastBuilder);
        }
      }
    }

    return splitBuilders;
  }

  private static TextStyle processStyle(TextStyle selfStyle, TextStyle parentStyle) {
    if (selfStyle == null) {
      if (parentStyle == null) {
        return new TextStyle(null, null, null, null, null);
      }
      return parentStyle;
    }

    if (parentStyle == null) {
      return new TextStyle(null, null, null, null, null);
    }

    return new TextStyle(
      selfStyle.isBold().orElse(parentStyle.isBold().orElse(null)),
      selfStyle.isItalic().orElse(parentStyle.isItalic().orElse(null)),
      selfStyle.hasUnderline().orElse(parentStyle.hasUnderline().orElse(null)),
      selfStyle.hasStrikethrough().orElse(parentStyle.hasStrikethrough().orElse(null)),
      selfStyle.isObfuscated().orElse(parentStyle.isObfuscated().orElse(null))
    );
  }

  private static Collection<String> separateNewLines(String text) {
    if (text == null || text.length() == 0) {
      return Collections.singleton("");
    }

    List<String> parts = new ArrayList<>();
    int nextNewLine;
    while (text != null && text.contains(NEW_LINE)) {
      nextNewLine = text.indexOf('\n');
      parts.add(text.substring(0, nextNewLine));
      parts.add("");
      if (text.length() > nextNewLine+1) {
        text = text.substring(nextNewLine+1);
      } else {
        text = null;
      }
    }

    if (text != null) {
      parts.add(text);
    }

    return parts;
  }

  public static String color(String string) {
    return TextSerializers.FORMATTING_CODE.serialize(Text.of(string));
  }

  public static Text getText(String message) {
    return TextSerializers.FORMATTING_CODE.deserialize(color(message));
  }
}

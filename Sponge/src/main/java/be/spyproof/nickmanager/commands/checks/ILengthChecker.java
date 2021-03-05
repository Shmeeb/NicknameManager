package be.spyproof.nickmanager.commands.checks;

import be.spyproof.nickmanager.commands.IMessageControllerHolder;
import be.spyproof.nickmanager.util.*;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Spyproof on 17/11/2016.
 */
public interface ILengthChecker extends IMessageControllerHolder
{
    default void checkLength(String nickname) throws CommandException
    {

        if (SpongeUtils.getText(nickname).toPlain().isEmpty()) {
            throw new CommandException(Text.of(TextColors.RED, "This nickname is too short!"));
        }

        if (!SpongeUtils.INSTANCE.lengthCheck(nickname))
        {
            Map<String, Integer> placeholders = new HashMap<>();
            placeholders.putAll(TemplateUtils.getParameters("length-with-colour", SpongeUtils.INSTANCE.getConfigController().maxNickLengthWithColour()));
            placeholders.putAll(TemplateUtils.getParameters("length-without-colour", SpongeUtils.INSTANCE.getConfigController().maxNickLengthWithoutColour()));
            throw new CommandException(this.getMessageController().getMessage(Reference.ErrorMessages.NICKNAME_TO_LONG).apply(placeholders).build());
        }
    }
}

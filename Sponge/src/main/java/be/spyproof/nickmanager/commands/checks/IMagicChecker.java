package be.spyproof.nickmanager.commands.checks;

import be.spyproof.nickmanager.commands.IMessageControllerHolder;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface IMagicChecker extends IMessageControllerHolder {
    default void checkMagic(CommandSource src, String nickname) throws CommandException {
        String nick = nickname.toLowerCase();

        if (nick.toLowerCase().contains("&k") && !src.hasPermission("nickmanager.bypass.magiclimit")) {
            //&k@@@
            if (Pattern.compile("((&k).[^&])+").matcher(nick).find()) {
                throw new CommandException(Text.of(TextColors.RED, "Your nickname may contain a maximum of 1 magic character in a row!"));
            }

            //&k@&k (attempting to bypass)
            if (Pattern.compile("((&k).(&k))+").matcher(nick).find()) {
                throw new CommandException(Text.of(TextColors.RED, "Your nickname may contain a maximum of 1 magic character in a row!"));
            }

            //&k@ > 2
            Matcher m = Pattern.compile("((&k).)+").matcher(nick);
            int count = 0;
            int from = 0;

            while (m.find(from)) {
                count++;
                from = m.start() + 1;
            }

            if (count > 2) {
                throw new CommandException(Text.of(TextColors.RED, "Your nickname may contain a maximum of 2 magic characters!"));
            }
        }
    }
}
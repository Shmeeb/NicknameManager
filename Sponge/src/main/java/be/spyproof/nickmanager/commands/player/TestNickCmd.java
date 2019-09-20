package be.spyproof.nickmanager.commands.player;

import be.spyproof.nickmanager.commands.AbstractCmd;
import be.spyproof.nickmanager.commands.argument.OldNicknameArg;
import be.spyproof.nickmanager.commands.checks.*;
import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.controller.MessageController;
import be.spyproof.nickmanager.util.Reference;
import be.spyproof.nickmanager.util.TemplateUtils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;

/**
 * Created by Spyproof on 28/10/2016.
 */
public class TestNickCmd extends AbstractCmd implements IPlayerCmd, IBlacklistChecker, IFormatChecker, ILengthChecker, IArgumentChecker, IMagicChecker
{
    private static final String ARG = "nickname";

    private TestNickCmd(MessageController messageController, ISpongeNicknameController playerController)
    {
        super(messageController, playerController);
    }

    @Override
    public CommandResult execute(Player src, CommandContext args) throws CommandException
    {
        String nick = getArgument(args, ARG);

        checkBlacklist(src, nick);
        checkFormat(src, nick);
        checkLength(nick);
        checkMagic(src, nick);

        src.sendMessage(this.getMessageController().getMessage(Reference.SuccessMessages.NICK_PREVIEW).apply(TemplateUtils.getParameters("nickname", TextSerializers.FORMATTING_CODE.deserialize(nick))).build());

        return CommandResult.success();
    }

    public static CommandSpec getCommandSpec(MessageController messageController, ISpongeNicknameController playerController)
    {
        return CommandSpec.builder()
                          .arguments(new OldNicknameArg(ARG, playerController))
                          .executor(new TestNickCmd(messageController, playerController))
                          .permission(Reference.Permissions.GENERIC_PLAYER_COMMANDS)
                          .build();
    }
}

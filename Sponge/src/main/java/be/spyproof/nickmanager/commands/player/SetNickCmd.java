package be.spyproof.nickmanager.commands.player;

import be.spyproof.nickmanager.commands.AbstractCmd;
import be.spyproof.nickmanager.commands.argument.OldNicknameArg;
import be.spyproof.nickmanager.commands.checks.*;
import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.controller.MessageController;
import be.spyproof.nickmanager.model.NicknameData;
import be.spyproof.nickmanager.util.Reference;
import be.spyproof.nickmanager.util.SpongeUtils;
import be.spyproof.nickmanager.util.TemplateUtils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;

/**
 * Created by Spyproof on 28/10/2016.
 */
public class SetNickCmd extends AbstractCmd implements IPlayerCmd, IArgumentChecker, IBlacklistChecker, ICooldownChecker, IFormatChecker, ILengthChecker, ITokenChecker, IMagicChecker
{
    private static final String ARG = "nickname";

    private SetNickCmd(MessageController messageController, ISpongeNicknameController playerController)
    {
        super(messageController, playerController);
    }

    @Override
    public CommandResult execute(Player src, CommandContext args) throws CommandException
    {
        String nick = getArgument(args, ARG);
        NicknameData nicknameData = this.getPlayerController().wrapPlayer(src);

        checkTokens(nicknameData, src);
        checkCooldown(nicknameData, src);
        checkBlacklist(src, nick);
        checkFormat(src, nick);
        checkLength(nick);
        checkMagic(src, nick);

        // Apply
        nicknameData.setNickname(nick);
        SpongeUtils.INSTANCE.applyNicknameToTabList(nicknameData, src);
        nicknameData.setLastChanged();
        if (!src.hasPermission(Reference.Permissions.BYPASS_CHANGE_LIMIT))
            nicknameData.setTokensRemaining(nicknameData.getTokensRemaining()-1);
        this.getPlayerController().savePlayer(nicknameData);

        src.sendMessage(this.getMessageController().getMessage(Reference.SuccessMessages.NICK_SET).apply(TemplateUtils.getParameters(nicknameData)).build());
        return CommandResult.success();
    }

    public static CommandSpec getCommandSpec(MessageController messageController, ISpongeNicknameController playerController)
    {
        return CommandSpec.builder()
                          .arguments(new OldNicknameArg(ARG, playerController))
                          .executor(new SetNickCmd(messageController, playerController))
                          .permission(Reference.Permissions.GENERIC_PLAYER_COMMANDS)
                          .build();
    }
}

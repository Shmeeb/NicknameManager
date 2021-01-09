package be.spyproof.nickmanager;

import be.spyproof.nickmanager.commands.moderator.*;
import be.spyproof.nickmanager.commands.player.*;
import be.spyproof.nickmanager.controller.ConfigController;
import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.controller.MessageController;
import be.spyproof.nickmanager.controller.SpongeNicknameController;
import be.spyproof.nickmanager.da.config.IConfigStorage;
import be.spyproof.nickmanager.listener.PlayerListener;
import be.spyproof.nickmanager.listener.UltimateChatListener;
import be.spyproof.nickmanager.listener.VanillaNicknameApplier;
import be.spyproof.nickmanager.model.NicknameData;
import be.spyproof.nickmanager.util.Reference;
import be.spyproof.nickmanager.util.SpongeUtils;
import be.spyproof.nickmanager.util.TextUtils;
import com.google.inject.Inject;
import me.rojo8399.placeholderapi.Placeholder;
import me.rojo8399.placeholderapi.PlaceholderService;
import me.rojo8399.placeholderapi.Source;
import me.rojo8399.placeholderapi.Token;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Spyproof on 28/10/2016.
 */
@Plugin(
        id = Reference.MetaData.PLUGIN_ID,
        name = Reference.MetaData.PLUGIN_NAME,
        version = Reference.MetaData.VERSION,
        description = Reference.MetaData.DESCRIPTIONS,
        authors = {"TPNils"},
        dependencies = {
                @Dependency(id="ultimatechat", optional=true),
                @Dependency(id="placeholderapi", optional=true)
        })
public class Main
{
    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private ISpongeNicknameController playerController;
    private IConfigStorage configController;
    private MessageController messageController;
    public static PlaceholderService placeholderService;

    /**
     * During this event, all configs are are being read and initialised all local variables
     * @param event the fired event
     */
    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        if (!this.configDir.exists())
            this.configDir.mkdirs();

        try
        {
            this.configController = new ConfigController(new File(configDir, "config.conf"));
            this.configController.load();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            this.messageController = new MessageController(new File(configDir, "lang"));
            this.messageController.load(this.configController.getLanguage());
        }
        catch (IOException | ObjectMappingException e)
        {
            e.printStackTrace();
        }

        try {
            this.playerController = new SpongeNicknameController(this.configController.getPlayerStorage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        SpongeUtils.initInstance(playerController, configController);
    }

    @Listener
    public void aboutToStart(GameAboutToStartServerEvent e) {
        placeholderService = Sponge.getServiceManager().provide(PlaceholderService.class).get();

        placeholderService.loadAll(this, this).stream().map(builder -> {
            switch (builder.getId()) {
                case "nickmanager":
                    return builder.tokens("nick").description("Gets the players nick data");
            }
            return builder;
        }).map(builder -> builder.author("Shmeeb")).forEach(builder -> {
            try {
                builder.buildAndRegister();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Listener
    public void onInit(GameInitializationEvent event)
    {
        registerListeners();
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event)
    {
        registerServices();
    }

    @Listener
    public void onServerStart(GameStartingServerEvent event)
    {
        registerCommands();
    }

    @Listener
    public void onServerStop(GameStoppingEvent event)
    {
        try
        {
            this.playerController.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void registerServices()
    {
        Sponge.getServiceManager().setProvider(this, ISpongeNicknameController.class, this.playerController);
    }

    private void registerListeners()
    {
        Sponge.getGame().getEventManager().registerListeners(this, new VanillaNicknameApplier(this.playerController));
        Sponge.getGame().getEventManager().registerListeners(this, new PlayerListener(this.playerController));

        try
        {
            Class.forName("br.net.fabiozumbi12.UltimateChat.Sponge.API.SendChannelMessageEvent");
            Sponge.getEventManager().registerListener(this, br.net.fabiozumbi12.UltimateChat.Sponge.API.SendChannelMessageEvent.class, new UltimateChatListener(this.playerController));
        }
        catch (ClassNotFoundException ignored)
        {}
    }

    private void registerCommands()
    {
        Sponge.getCommandManager().register(this, AcceptRulesCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ACCEPT_RULES);

        CommandSpec playerHelp = PlayerHelpCmd.getCommandSpec(this.messageController);
        CommandSpec nickCmd =
                CommandSpec.builder()
                           .executor(playerHelp.getExecutor())
                           .child(SetNickCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_SET)
                           .child(TestNickCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_PREVIEW)
                           .child(CheckSelfCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_CHECK)
                           .child(ResetOwnNickCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_RESET)
                           .child(RulesCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_RULES)
                           .child(RealNameCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.PLAYER_REAL_NAME)
                           .child(DisplayFormatsCmd.getCommandSpec(), Reference.CommandKeys.PLAYER_FORMAT)
                           .child(UnlockCmd.getCommandSpec(this.messageController), Reference.CommandKeys.PLAYER_UNLOCK)
                           .child(playerHelp, "help", "hep")
                           .build();
        Sponge.getCommandManager().register(this, nickCmd, "nick", "nickname");

        CommandSpec adminHelp = AdminHelpCmd.getCommandSpec(this.messageController);
        CommandSpec resetCmd = CommandSpec.builder()
                                          .executor(adminHelp.getExecutor())
                                          .child(adminHelp, "help", "hep")
                                          .child(ResetOtherCooldownCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_RESET_COOLDOWN)
                                          .child(ResetOtherNickCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_RESET_NICKNAME)
                                          .child(ResetOtherRulesCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_RESET_RULES)
                                          .child(ResetOtherTokensCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_RESET_TOKENS)
                                          .build();

        CommandSpec adminCmd =
                CommandSpec.builder()
                           .executor(adminHelp.getExecutor())
                           .child(adminHelp, "help", "hep")
                           .child(SetNickOthersCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_SET)
                           .child(CheckOtherCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_CHECK)
                           .child(GiveNickChangesCmd.getCommandSpec(this.messageController, this.playerController), Reference.CommandKeys.ADMIN_GIVE)
                           .child(resetCmd, "reset")
                           .build();
        Sponge.getCommandManager().register(this, adminCmd, "admnick", "adminnick", "adminnickname");
    }

    @Placeholder(id = "nickmanager")
    public Text nickmanager(@Source Player source, @Token String token) {
        NicknameData nicknameData = playerController.wrapPlayer(source);
        Optional<String> nickname = nicknameData.getNickname();

        if (!token.equalsIgnoreCase("nick")) return Text.EMPTY;

        if (nickname.isPresent()) {
            return TextUtils.getText(nickname.get());
        } else {
            return Text.of(source.getName());
        }
    }
}
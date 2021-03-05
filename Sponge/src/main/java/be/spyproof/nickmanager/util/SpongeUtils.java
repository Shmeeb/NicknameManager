package be.spyproof.nickmanager.util;

import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.da.config.IConfigStorage;
import be.spyproof.nickmanager.model.NicknameData;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Spyproof on 30/10/2016.
 *
 * This contains various utilities
 */
public class SpongeUtils
{
    protected ISpongeNicknameController playerController;
    protected IConfigStorage configController;

    public static SpongeUtils INSTANCE;

    /**
     * Initiate the instance for everyone to be able to access
     * @param playerController The controller to wrap a player in
     * @param configController The controller that contains all configuration options
     */
    public static void initInstance(ISpongeNicknameController playerController, IConfigStorage configController)
    {
        INSTANCE = new SpongeUtils(playerController, configController);
    }

    public static Text getText(String message) {
        return TextSerializers.FORMATTING_CODE.deserialize(color(message));
    }

    public static String color(String string) {
        return TextSerializers.FORMATTING_CODE.serialize(Text.of(string));
    }

    private SpongeUtils(ISpongeNicknameController playerController, IConfigStorage configController)
    {
        this.playerController = playerController;
        this.configController = configController;
    }

    public IConfigStorage getConfigController()
    {
        return configController;
    }

    /**
     * @param source     Used to check to see if the player has accepted the rules or can bypass it
     * @return true if they don't need to accept the rules, if they can bypass it or have accepted it, false if not.
     */
    public boolean acceptedRules(Player source)
    {
        return acceptedRules(this.playerController.wrapPlayer(source), source);
    }

    /**
     * @param nicknameData The player that will be checked if they have accepted the rules or not
     * @param source     Used to check to see if the player can bypass accepting the rules
     * @return true if they don't need to accept the rules, if they can bypass it or have accepted it, false if not.
     */
    public boolean acceptedRules(NicknameData nicknameData, CommandSource source)
    {
        if (nicknameData.hasAcceptedRules())
            return true;
        if (source.hasPermission(Reference.Permissions.BYPASS_RULES))
            return true;

        return !this.configController.mustAcceptRules();
    }

    /**
     * @param source The player that will be checked for the amount of nickname tokens they have or can bypass
     * @return true if they have enough tokens or bypass the limit, false if not.
     */
    public boolean canChangeNickname(Player source)
    {
        return canChangeNickname(this.playerController.wrapPlayer(source), source);
    }

    /**
     * @param nicknameData The player that will be checked for the amount of nickname tokens they have
     * @param source     Used to check to see if the player can bypass the change limit
     * @return true if they have enough tokens or bypass the limit, false if not.
     */
    public boolean canChangeNickname(NicknameData nicknameData, CommandSource source)
    {
        if (nicknameData.getTokensRemaining() <= 0)
            return source.hasPermission(Reference.Permissions.BYPASS_CHANGE_LIMIT);

        return true;
    }

    /**
     * @param source   Used to check to see if the player can bypass the blacklist
     * @param nickname The nickname that needs to be checked if it matches the blacklist
     * @return The first blacklist regex string that matched, empty if no matches were found
     */
    public Optional<String> isBlacklisted(CommandSource source, String nickname)
    {
        if (source.hasPermission(Reference.Permissions.BYPASS_BLACKLIST))
            return Optional.empty();

        nickname = nickname.toLowerCase();

        List<String> patterns = this.configController.getBlacklist();
        for (String s : patterns)
            if (nickname.matches(s.toLowerCase()))
                return Optional.of(s);


        return Optional.empty();
    }

    /**
     * The length is always to long if the total character count is larger than 255.
     *
     * Additional length checks will happen:
     * 1. The length of all characters can not exceed as specified in the config
     * 1. The length of all characters excluding colour characters can not exceed as specified in the config
     *
     * @param nickname The nickname that needs to be checked for its length
     * @return true if the length is ok, false if not
     */
    public boolean lengthCheck(String nickname)
    {
        if (nickname.length() > 255)
            return false;

        if (nickname.length() > this.configController.maxNickLengthWithColour())
            return false;

        if (TextSerializers.FORMATTING_CODE.stripCodes(nickname).length() > this.configController.maxNickLengthWithoutColour())
            return false;

        return true;
    }

    /**
     * @return The default cooldown on how long a player will have to wait before they can change their nickname again
     */
    public long getDefaultCooldown()
    {
        return this.configController.getCooldowns().getOrDefault("default", DateUtil.parseDateDiff("1mo", true) - System.currentTimeMillis());
    }

    /**
     * @return All extra cooldown categories on how long a player will have to wait before they can change their nickname again
     */
    public Map<String, Long> getExtraCooldowns()
    {
        Map<String, Long> cooldowns = this.configController.getCooldowns();
        cooldowns.remove("default");
        return cooldowns;
    }

    public void applyNicknameToTabList(NicknameData nicknameData, Player player) {
        if (this.configController.setTabListName()) {
            String nicknameString = nicknameData.getNickname().orElse(nicknameData.getName());

            // If the nickname has formatting, apply a reset at the end
            if (nicknameString.indexOf('&') > -1 && !nicknameString.endsWith("&r")) {
                nicknameString = nicknameString + "&r";
            }

            final Text nickname = TextSerializers.FORMATTING_CODE.deserialize(nicknameString);

            player.getTabList().getEntry(player.getUniqueId()).ifPresent(
              tabListEntry -> tabListEntry.setDisplayName(nickname)
            );
        }
    }
}

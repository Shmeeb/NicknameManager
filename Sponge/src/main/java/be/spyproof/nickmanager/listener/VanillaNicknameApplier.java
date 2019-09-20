package be.spyproof.nickmanager.listener;

import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.model.NicknameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

/**
 * Created by Spyproof on 30/10/2016.
 *
 * The nickname applier will listen to all chat messages.
 * If the sender has a nickname, the header will be reset and replace with its nickname.
 *
 * The new header will have the same layout as the default minecraft header.
 */
public class VanillaNicknameApplier
{
    private ISpongeNicknameController playerController;

    public VanillaNicknameApplier(ISpongeNicknameController playerController)
    {
        this.playerController = playerController;
    }

    @Listener(order = Order.LATE)
    public void onMessageEvent(MessageChannelEvent.Chat event, @First Player player)
    {
        NicknameData nicknameData = this.playerController.wrapPlayer(player);

        if (!event.getFormatter().getHeader().isEmpty() && nicknameData.getNickname().isPresent())
        {
            Text.Builder builder = Text.builder();

            Text header = event.getFormatter().getHeader().format();
            String rawHeader = TextSerializers.FORMATTING_CODE.serialize(header);
            if (rawHeader.contains(player.getName()))
                rawHeader = rawHeader.replace(player.getName(), nicknameData.getNickname().get());

            builder.append(Text.of(TextSerializers.FORMATTING_CODE.deserialize(rawHeader)));

            Optional<ClickAction<?>> clickAction = header.getClickAction();
            if (clickAction.isPresent())
                builder.onClick(clickAction.get());
            else
                builder.onClick(TextActions.suggestCommand("/msg " + player.getName() + " "));

            Optional<HoverAction<?>> hoverAction = header.getHoverAction();
            if (clickAction.isPresent())
                builder.onHover(hoverAction.get());
            else
                builder.onHover(TextActions.showEntity(player.getUniqueId(), player.getName()));

            event.getFormatter().setHeader(builder.build());
        }
    }
}

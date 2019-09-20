package be.spyproof.nickmanager.listener;

import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.model.NicknameData;
import br.net.fabiozumbi12.UltimateChat.Sponge.API.SendChannelMessageEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

/**
 * Created by Spyproof on 25/04/2017.
 */
public class UltimateChatListener implements EventListener<SendChannelMessageEvent>
{
    private ISpongeNicknameController playerController;

    public UltimateChatListener(ISpongeNicknameController playerController)
    {
        this.playerController = playerController;
    }

    public void handle(SendChannelMessageEvent event)
    {
        if (event.getSender() instanceof Player)
        {
            NicknameData nicknameData = playerController.wrapPlayer((Player) event.getSender());
            Optional<String> nickname = nicknameData.getNickname();

            if (nickname.isPresent())
                event.addTag("{Nickname}", nickname.get());
            else
                event.addTag("{Nickname}", event.getSender().getName());
        }
    }
}

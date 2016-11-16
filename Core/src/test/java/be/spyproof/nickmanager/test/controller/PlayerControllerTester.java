package be.spyproof.nickmanager.test.controller;

import be.spyproof.nickmanager.controller.IPlayerController;
import be.spyproof.nickmanager.model.PlayerData;
import be.spyproof.nickmanager.test.TestReference;
import org.testng.Assert;

import java.util.List;
import java.util.Optional;

/**
 * Created by Spyproof on 28/10/2016.
 */
public class PlayerControllerTester
{
    private IPlayerController playerController;
    private PlayerData testPlayer;

    public PlayerControllerTester(IPlayerController playerController)
    {
        this.playerController = playerController;
    }

    public void wrapNewPlayer()
    {
        PlayerData init = TestReference.getPlayerData();
        this.testPlayer = this.playerController.wrap(init.getUuid(), init.getName());
        Assert.assertNotNull(this.testPlayer);
    }

    public void updatePlayer()
    {
        this.testPlayer.setName("New name");
        this.testPlayer.setReadRules(true);
        this.testPlayer.setNickname("A nickname");
        this.testPlayer.setTokensRemaining(4);
        this.testPlayer.setLastChanged();
        this.playerController.savePlayer(this.testPlayer);
    }

    public void getPlayerByName()
    {
        Optional<? extends PlayerData> storedPlayer = this.playerController.getPlayer(this.testPlayer.getName());
        Assert.assertTrue(storedPlayer.isPresent());
        Assert.assertEquals(storedPlayer.get(), this.testPlayer);
    }

    public void getPlayerByUuid()
    {
        Optional<? extends PlayerData> storedPlayer = this.playerController.getPlayer(this.testPlayer.getUuid());
        Assert.assertTrue(storedPlayer.isPresent());
        Assert.assertEquals(storedPlayer.get(), this.testPlayer);
    }

    public void getPlayerByNickname()
    {
        List<? extends PlayerData> storedPlayers = this.playerController.getPlayerByNickname(this.testPlayer.getNickname().get());

        for (PlayerData storedPlayer : storedPlayers)
            if (this.testPlayer.getUuid().equals(storedPlayer.getUuid()))
            {
                Assert.assertEquals(storedPlayer, this.testPlayer);
                return;
            }

        Assert.fail();
    }

    public void removePlayer()
    {
        this.playerController.removePlayer(this.testPlayer);
        Optional<? extends PlayerData> storedPlayer = this.playerController.getPlayer(this.testPlayer.getUuid());
        Assert.assertFalse(storedPlayer.isPresent());
    }
}
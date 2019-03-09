package de.pauhull.nickapi.listener;

import de.pauhull.nickapi.NickApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class PlayerQuitListener implements Listener {

    private NickApi nickApi;

    public PlayerQuitListener(NickApi nickApi) {
        this.nickApi = nickApi;

        Bukkit.getPluginManager().registerEvents(this, nickApi);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        nickApi.getNickManager().unnick(player, false, false);
    }

}

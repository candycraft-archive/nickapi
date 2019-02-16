package de.pauhull.nickapi.listener;

import de.pauhull.nickapi.NickAPI;
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

    private NickAPI nickAPI;

    public PlayerQuitListener(NickAPI nickAPI) {
        this.nickAPI = nickAPI;

        Bukkit.getPluginManager().registerEvents(this, nickAPI);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        nickAPI.getNickManager().unnick(player, false);
    }

}

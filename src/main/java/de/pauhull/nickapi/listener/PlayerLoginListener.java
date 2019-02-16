package de.pauhull.nickapi.listener;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import de.pauhull.nickapi.NickAPI;
import de.pauhull.nickapi.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class PlayerLoginListener implements Listener {

    private NickAPI nickAPI;

    public PlayerLoginListener(NickAPI nickAPI) {
        this.nickAPI = nickAPI;

        Bukkit.getPluginManager().registerEvents(this, nickAPI);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {

        Player player = event.getPlayer();
        nickAPI.getNickTable().isAutoNick(player.getUniqueId(), autoNick -> {
            if (autoNick) {
                if (!player.hasPermission(Permissions.NICK)) {
                    nickAPI.getNickTable().setAutoNick(player.getUniqueId(), false);
                    return;
                }

                if (!TimoCloudAPI.getBukkitAPI().getThisServer().getGroup().getName().equals("Lobby")) {
                    nickAPI.getNickManager().nick(player);
                }
            }
        });
    }

}

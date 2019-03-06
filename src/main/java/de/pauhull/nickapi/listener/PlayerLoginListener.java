package de.pauhull.nickapi.listener;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import de.pauhull.nickapi.NickApi;
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

    private NickApi nickApi;

    public PlayerLoginListener(NickApi nickApi) {
        this.nickApi = nickApi;

        Bukkit.getPluginManager().registerEvents(this, nickApi);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {

        Player player = event.getPlayer();
        nickApi.getNickTable().isAutoNick(player.getUniqueId(), autoNick -> {
            if (autoNick) {
                if (!player.hasPermission(Permissions.NICK)) {
                    nickApi.getNickTable().setAutoNick(player.getUniqueId(), false);
                    return;
                }

                if (!TimoCloudAPI.getBukkitAPI().getThisServer().getGroup().getName().equals("Lobby")
                        && !TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("CandyCane")
                        && !TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("Gingerbread")) {
                    nickApi.getNickManager().nick(player);
                }
            }
        });
    }

}

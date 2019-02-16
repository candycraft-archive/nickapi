package de.pauhull.nickapi.listener;

import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class PlayerJoinListener implements Listener {

    private NickAPI nickAPI;

    public PlayerJoinListener(NickAPI nickAPI) {
        this.nickAPI = nickAPI;

        Bukkit.getPluginManager().registerEvents(this, nickAPI);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (nickAPI.getNickManager().getNicked().containsKey(player.getUniqueId())) {
            String nick = nickAPI.getNickManager().getNicked().get(player.getUniqueId());
            player.sendMessage(Messages.PREFIX + "Du bist nun als \"§e" + nick + "§7\" genickt!");
        }
    }

}

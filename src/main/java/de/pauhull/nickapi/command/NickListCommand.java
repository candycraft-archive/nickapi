package de.pauhull.nickapi.command;

import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickApi;
import de.pauhull.nickapi.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Paul
 * on 04.03.2019
 *
 * @author pauhull
 */
public class NickListCommand implements CommandExecutor {

    private NickApi nickApi;

    public NickListCommand(NickApi nickApi) {
        this.nickApi = nickApi;
        nickApi.getCommand("nicklist").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.NICKLIST)) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSIONS);
            return true;
        }

        Map<UUID, String> nicked = nickApi.getNickManager().getNicked();
        if (nicked.isEmpty()) {
            sender.sendMessage(Messages.PREFIX + "§cAuf deinem Server ist niemand genickt.");
        } else {
            sender.sendMessage(" ");
            sender.sendMessage(Messages.PREFIX + "Folgende Spieler sind auf deinem Server genickt:");
            for (UUID uuid : nicked.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                sender.sendMessage("§8● §a" + player.getDisplayName() + " §8(§e" + player.getName() + "§8)");
            }
            sender.sendMessage(" ");
        }

        return true;
    }

}

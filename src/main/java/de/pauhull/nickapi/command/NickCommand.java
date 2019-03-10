package de.pauhull.nickapi.command;

import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickApi;
import de.pauhull.nickapi.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class NickCommand implements CommandExecutor {

    private NickApi nickApi;

    public NickCommand(NickApi nickApi) {
        this.nickApi = nickApi;

        nickApi.getCommand("nick").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.NICK)) {
            sender.sendMessage(Messages.NO_PERMISSIONS);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.ONLY_PLAYERS);
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("refresh")) {
            if (nickApi.getNickManager() != null) {
                nickApi.getNickManager().refresh(player);
                player.sendMessage(Messages.PREFIX + "Dein Skin wurde aktualisiert.");
            } else {
                player.sendMessage(Messages.PREFIX + "§cHier nicht möglich.");
            }
            return true;
        }

        nickApi.getNickTable().isAutoNick(player.getUniqueId(), autoNick -> {
            nickApi.getNickTable().setAutoNick(player.getUniqueId(), !autoNick);
            if (autoNick) {
                sender.sendMessage(Messages.PREFIX + "Du wirst nun §cnicht §7mehr genickt!");
                if (nickApi.getNickManager() != null) {
                    nickApi.getNickManager().unnick(player, false, true);
                }
            } else {
                sender.sendMessage(Messages.PREFIX + "Du wirst nun §aautomatisch §7genickt!");
                if (nickApi.getNickManager() != null) {
                    if (!nickApi.getNickManager().getNicked().containsKey(player.getUniqueId())) {
                        nickApi.getNickManager().nick(player);
                    }
                }
            }
        });

        return false;
    }

}

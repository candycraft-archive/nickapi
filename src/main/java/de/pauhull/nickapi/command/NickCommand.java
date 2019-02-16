package de.pauhull.nickapi.command;

import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickAPI;
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

    private NickAPI nickAPI;

    public NickCommand(NickAPI nickAPI) {
        this.nickAPI = nickAPI;

        nickAPI.getCommand("nick").setExecutor(this);
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
            nickAPI.getNickManager().refresh(player);
            player.sendMessage(Messages.PREFIX + "Dein Skin wurde aktualisiert.");
            return true;
        }

        nickAPI.getNickTable().isAutoNick(player.getUniqueId(), autoNick -> {
            nickAPI.getNickTable().setAutoNick(player.getUniqueId(), !autoNick);
            if (autoNick) {
                sender.sendMessage(Messages.PREFIX + "Du wirst nun §cnicht §7mehr genickt!");
                nickAPI.getNickManager().unnick(player, false);
            } else {
                sender.sendMessage(Messages.PREFIX + "Du wirst nun §aautomatisch §7genickt!");
                if (!nickAPI.getNickManager().getNicked().containsKey(player.getUniqueId())) {
                    nickAPI.getNickManager().nick(player);
                }
            }
        });

        return false;
    }

}

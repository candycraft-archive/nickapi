package de.pauhull.nickapi.command;

import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Paul
 * on 06.03.2019
 *
 * @author pauhull
 */
public class SetNickCommand implements CommandExecutor {

    private NickApi nickApi;

    public SetNickCommand(NickApi nickApi) {
        this.nickApi = nickApi;

        nickApi.getCommand("setnick").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nickapi.setnick")) {
            sender.sendMessage(Messages.PREFIX + Messages.NO_PERMISSIONS);
            return true;
        }

        if (!(sender instanceof Player)) {
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Messages.PREFIX + "§c/setnick <Nick>");
        }

        nickApi.getUuidFetcher().fetchProfileAsync(args[0], profile -> {
            if (profile == null) {
                sender.sendMessage(Messages.PREFIX + "§cDieser Spieler existiert nicht");
                return;
            }

            nickApi.getNickManager().getSkinTexture(profile.getUuid(), skinTexture -> {
                nickApi.getNickManager().nick((Player) sender, profile.getPlayerName(), skinTexture);
            });
        });

        return true;
    }

}

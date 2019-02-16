package de.pauhull.nickapi.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class PostPlayerUnnickEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    @Getter
    private String nick;

    public PostPlayerUnnickEvent(Player player, String nick) {
        super(player);
        this.nick = nick;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}

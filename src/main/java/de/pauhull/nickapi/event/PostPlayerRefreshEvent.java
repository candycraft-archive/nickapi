package de.pauhull.nickapi.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Created by Paul
 * on 13.12.2018
 *
 * @author pauhull
 */
public class PostPlayerRefreshEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    public PostPlayerRefreshEvent(Player player) {
        super(player);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}

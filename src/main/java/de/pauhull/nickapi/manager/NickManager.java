package de.pauhull.nickapi.manager;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickApi;
import de.pauhull.nickapi.event.*;
import de.pauhull.nickapi.gson.NickData;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Paul
 * on 12.12.2018
 *
 * @author pauhull
 */
public class NickManager {

    @Getter
    private Map<UUID, String> nicked = new HashMap<>();

    @Getter
    private Map<UUID, GameProfile> oldProfiles = new HashMap<>();

    private NickApi nickApi;

    public NickManager(NickApi nickApi) {
        this.nickApi = nickApi;
    }

    public void nick(Player player) {
        nickApi.getExecutorService().execute(() -> {
            try {

                URL url = new URL("https://api.mcstats.net/v2/server/95e010fe-fbaf-435f-bd2c-e07635e8f266/player/" + player.getUniqueId() + "/nick/create?get2post=1&secret=OTFkZTAzNzczYmNiNmJhNzQ0MmFkODgxZThlMjA1ZWQ");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                NickData data = new Gson().fromJson(reader, NickData.class);
                Property property = new Property("textures", data.response.skin.value, data.response.skin.signature);

                Bukkit.getScheduler().runTask(nickApi, () -> {
                    nick(player, data.response.playername, property);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void nick(Player player, String nick, Property texture) {

        if (TimoCloudAPI.getBukkitAPI().getThisServer().getGroup().getName().equals("Lobby")
                || TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("CandyCane")
                || TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("Gingerbread"))
            return;

        PlayerNickEvent event = new PlayerNickEvent(player, nick);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        player.setDisplayName(player.getName());

        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        oldProfiles.put(player.getUniqueId(), entityPlayer.getProfile());

        GameProfile profile = new GameProfile(player.getUniqueId(), nick);

        if (texture != null) {
            profile.getProperties().put("textures", texture);
        }

        setProfile(entityPlayer, profile);
        refresh(player);
        nicked.put(player.getUniqueId(), nick);

        if (player.isValid()) {
            player.sendMessage(Messages.PREFIX + "Du bist nun als §e" + nick + "§7 genickt!");
        }

        PostPlayerNickEvent postEvent = new PostPlayerNickEvent(player, nick);
        Bukkit.getPluginManager().callEvent(postEvent);
    }

    public void setProfile(EntityPlayer player, GameProfile profile) {
        try {
            Field field = player.getClass().getSuperclass().getDeclaredField("bH");
            field.setAccessible(true);
            field.set(player, profile);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void unnick(Player player, boolean sendMessage) {
        if (nicked.containsKey(player.getUniqueId())) {

            String nick = nicked.get(player.getUniqueId());
            PlayerUnnickEvent event = new PlayerUnnickEvent(player, nick);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;

            EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
            GameProfile oldProfile = oldProfiles.get(player.getUniqueId());
            setProfile(entityPlayer, oldProfile);
            oldProfiles.remove(player.getUniqueId());
            nicked.remove(player.getUniqueId());
            refresh(player);

            if (player.isValid() && sendMessage) {
                player.sendMessage(Messages.PREFIX + "Du bist §cnicht §7mehr genickt!");
            }

            PostPlayerUnnickEvent postEvent = new PostPlayerUnnickEvent(player, nick);
            Bukkit.getPluginManager().callEvent(postEvent);
        }
    }

    public void refresh(Player player) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(craftPlayer.getEntityId());
        PacketPlayOutPlayerInfo tabRemove = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, craftPlayer.getHandle());

        Bukkit.getScheduler().scheduleSyncDelayedTask(nickApi, () -> {
            craftPlayer.getHandle().server.getPlayerList().moveToWorld(craftPlayer.getHandle(), craftPlayer.getHandle().dimension, false, player.getLocation(), true);
            for (Player all : Bukkit.getOnlinePlayers()) {
                CraftPlayer craftAll = (CraftPlayer) all;
                craftAll.getHandle().playerConnection.sendPacket(tabRemove);
                if (!all.equals(player)) {
                    craftAll.getHandle().playerConnection.sendPacket(destroy);
                }
            }
        }, 1);

        PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle());
        PacketPlayOutPlayerInfo tabAdd = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, craftPlayer.getHandle());

        Bukkit.getScheduler().scheduleSyncDelayedTask(nickApi, () -> {
            for (Player all : Bukkit.getOnlinePlayers()) {
                CraftPlayer craftAll = (CraftPlayer) all;
                craftAll.getHandle().playerConnection.sendPacket(tabAdd);
                if (!all.equals(player)) {
                    craftAll.getHandle().playerConnection.sendPacket(spawn);
                }

                PostPlayerRefreshEvent event = new PostPlayerRefreshEvent(player);
                Bukkit.getPluginManager().callEvent(event);
            }
        }, 20);
    }

}

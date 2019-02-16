package de.pauhull.nickapi.manager;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickAPI;
import de.pauhull.nickapi.event.*;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by Paul
 * on 12.12.2018
 *
 * @author pauhull
 */
public class NickManager {

    private static final String API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final Random RANDOM = new Random();

    @Getter
    private Map<String, Property> skins = new HashMap<>();

    @Getter
    private Map<UUID, String> nicked = new HashMap<>();

    @Getter
    private Map<UUID, GameProfile> oldProfiles = new HashMap<>();

    private NickAPI nickAPI;

    public NickManager(NickAPI nickAPI) {
        this.nickAPI = nickAPI;

        try {
            InputStream nickListStream = nickAPI.getResource("nicks.txt");
            InputStreamReader streamReader = new InputStreamReader(nickListStream);
            BufferedReader reader = new BufferedReader(streamReader);

            String playerName;
            while ((playerName = reader.readLine()) != null) {
                final String finalPlayerName = playerName;
                nickAPI.getUuidFetcher().fetchProfileAsync(playerName, profile -> {
                    getSkinTexture(profile.getUuid(), texture -> {
                        skins.put(finalPlayerName, texture);
                    });
                });
            }

            reader.close();
            streamReader.close();
            nickListStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nick(Player player) {
        String nick;
        String[] nicks = skins.keySet().toArray(new String[0]);

        findNickLoop:
        while (true) {
            nick = nicks[RANDOM.nextInt(nicks.length)];

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().equalsIgnoreCase(nick)) {
                    continue findNickLoop;
                }
            }

            break;
        }

        nick(player, nick);
    }

    public void nick(Player player, String nick) {

        if (skins.containsKey(nick)) {
            nick(player, nick, skins.get(nick));
            return;
        }

        nickAPI.getUuidFetcher().fetchProfileAsync(nick, profile -> {
            getSkinTexture(profile.getUuid(), texture -> {
                Bukkit.getScheduler().runTask(nickAPI, () -> {
                    nick(player, nick, texture);
                });
            });
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

    public void getSkinTexture(UUID uuid, Consumer<Property> consumer) {

        if (uuid == null) {
            consumer.accept(null);
            return;
        }

        nickAPI.getExecutorService().execute(() -> {
            try {

                HttpURLConnection connection = (HttpURLConnection) new URL(String.format(API_URL, uuid.toString().replace("-", ""))).openConnection();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream stream = connection.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(stream);
                    BufferedReader reader = new BufferedReader(streamReader);

                    JsonElement element = new JsonParser().parse(reader);
                    JsonObject response = element.getAsJsonObject();

                    JsonArray properties = response.getAsJsonArray("properties");

                    boolean success = false;
                    for (int i = 0; i < properties.size(); i++) {
                        JsonObject property = properties.get(i).getAsJsonObject();

                        if (property.get("name") != null && property.get("name").getAsString().equals("textures")) {
                            String value = property.get("value").getAsString();
                            String signature = property.get("signature").getAsString();
                            consumer.accept(new Property("textures", value, signature));
                            success = true;
                            break;
                        }
                    }

                    if (!success) {
                        consumer.accept(null);
                    }

                    reader.close();
                    streamReader.close();
                    stream.close();
                } else {
                    consumer.accept(null);
                }

            } catch (IOException e) {
                e.printStackTrace();
                consumer.accept(null);
            }
        });

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

        Bukkit.getScheduler().scheduleSyncDelayedTask(nickAPI, () -> {
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

        Bukkit.getScheduler().scheduleSyncDelayedTask(nickAPI, () -> {
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

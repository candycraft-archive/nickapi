package de.pauhull.nickapi.manager;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.pauhull.nickapi.Messages;
import de.pauhull.nickapi.NickApi;
import de.pauhull.nickapi.event.*;
import de.pauhull.nickapi.gson.NickData;
import de.pauhull.nickapi.gson.NickData.Response.Nick;
import de.pauhull.uuidfetcher.common.fetcher.TimedHashMap;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Paul
 * on 12.12.2018
 *
 * @author pauhull
 */
public class NickManager implements Runnable {

    private static final String API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    @Getter
    private Map<UUID, String> nicked = new HashMap<>();

    @Getter
    private Map<UUID, GameProfile> oldProfiles = new HashMap<>();

    @Getter
    private LinkedList<Nick> nickCache = new LinkedList<>();

    @Getter
    private TimedHashMap<UUID, Property> skinCache = new TimedHashMap<>(TimeUnit.MINUTES, 5);

    private NickApi nickApi;

    public NickManager(NickApi nickApi) {
        this.nickApi = nickApi;
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(nickApi, this, 0, 40);
    }

    @Override
    public void run() {
        int neededNicks = 10 - nickCache.size();

        if (neededNicks <= 0) {
            return;
        }

        try {
            URL url = new URL("https://api.mcstats.net/v2/server/95e010fe-fbaf-435f-bd2c-e07635e8f266/nick/" + neededNicks + "?get2post=1&secret=OTFkZTAzNzczYmNiNmJhNzQ0MmFkODgxZThlMjA1ZWQ");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            NickData data = new Gson().fromJson(reader, NickData.class);
            reader.close();
            nickCache.addAll(Arrays.asList(data.response.nicks));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Nick getRandomNickData() {
        return nickCache.removeLast();
    }

    public void nick(Player player) {
        Nick nick = getRandomNickData();
        Property property = new Property("textures", nick.skin.value, nick.skin.signature);
        nick(player, nick.playername, property);
    }

    public void nick(Player player, String nick, Property texture) {

        if (TimoCloudAPI.getBukkitAPI().getThisServer().getGroup().getName().equals("Lobby")
                || TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("CandyCane")
                || TimoCloudAPI.getBukkitAPI().getThisServer().getName().equals("Gingerbread"))
            return;

        if (nicked.containsKey(player.getUniqueId())) {
            unnick(player, false, false);
        }

        PlayerNickEvent event = new PlayerNickEvent(player, nick);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

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

    public void unnick(Player player, boolean sendMessage, boolean refresh) {
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

            if (refresh) {
                refresh(player);
            }

            if (player.isValid() && sendMessage) {
                player.sendMessage(Messages.PREFIX + "Du bist §cnicht §7mehr genickt!");
            }

            PostPlayerUnnickEvent postEvent = new PostPlayerUnnickEvent(player, nick);
            Bukkit.getPluginManager().callEvent(postEvent);
        }
    }

    public void getSkinTexture(UUID uuid, Consumer<Property> consumer) {

        if (uuid == null) {
            consumer.accept(null);
            return;
        }

        nickApi.getExecutorService().execute(() -> {
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
                            Property prop = new Property("textures", value, signature);
                            skinCache.put(uuid, prop);
                            consumer.accept(prop);
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

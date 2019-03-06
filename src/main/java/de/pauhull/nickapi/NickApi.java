package de.pauhull.nickapi;

import de.pauhull.nickapi.command.NickCommand;
import de.pauhull.nickapi.command.NickListCommand;
import de.pauhull.nickapi.data.MySQL;
import de.pauhull.nickapi.data.table.NickTable;
import de.pauhull.nickapi.listener.PlayerJoinListener;
import de.pauhull.nickapi.listener.PlayerLoginListener;
import de.pauhull.nickapi.manager.NickManager;
import de.pauhull.uuidfetcher.spigot.SpigotUUIDFetcher;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Paul
 * on 12.12.2018
 *
 * @author pauhull
 */
public class NickApi extends JavaPlugin {

    @Getter
    private static NickApi instance;

    @Getter
    private SpigotUUIDFetcher uuidFetcher;

    @Getter
    private ExecutorService executorService;

    @Getter
    private NickManager nickManager;

    @Getter
    private FileConfiguration config;

    @Getter
    private MySQL mySQL;

    @Getter
    private NickTable nickTable;

    @Override
    public void onEnable() {
        instance = this;
        this.uuidFetcher = SpigotUUIDFetcher.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.nickManager = new NickManager(this);
        this.config = copyAndLoad("config.yml", new File(getDataFolder(), "config.yml"));
        this.mySQL = new MySQL(config.getString("MySQL.Host"),
                config.getString("MySQL.Port"),
                config.getString("MySQL.Database"),
                config.getString("MySQL.User"),
                config.getString("MySQL.Password"),
                config.getBoolean("MySQL.SSL"));

        if (!mySQL.connect()) {
            return;
        }

        this.nickTable = new NickTable(mySQL, executorService);

        new NickListCommand(this);
        new NickCommand(this);
        new PlayerLoginListener(this);
        new PlayerJoinListener(this);
    }

    @Override
    public void onDisable() {
        instance = null;
        this.executorService.shutdown();
    }

    private FileConfiguration copyAndLoad(String resource, File file) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();

            try {
                Files.copy(getResource(resource), file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

}

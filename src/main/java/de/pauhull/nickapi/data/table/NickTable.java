package de.pauhull.nickapi.data.table;

import de.pauhull.nickapi.data.MySQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class NickTable {

    private static final String TABLE = "autonick";

    private MySQL mySQL;
    private ExecutorService executorService;

    public NickTable(MySQL mySQL, ExecutorService executorService) {
        this.mySQL = mySQL;
        this.executorService = executorService;

        mySQL.update("CREATE TABLE IF NOT EXISTS `" + TABLE + "` (`id` INT AUTO_INCREMENT, `uuid` VARCHAR(255), PRIMARY KEY (`id`))");
    }

    public void isAutoNick(UUID uuid, Consumer<Boolean> consumer) {
        executorService.execute(() -> {
            try {
                ResultSet result = mySQL.query("SELECT * FROM `" + TABLE + "` WHERE `uuid`='" + uuid + "'");
                consumer.accept(result.isBeforeFirst());
            } catch (SQLException e) {
                e.printStackTrace();
                consumer.accept(false);
            }
        });
    }

    public void setAutoNick(UUID uuid, boolean autoNick) {
        isAutoNick(uuid, isAutoNick -> {
            if (autoNick) {
                if (!isAutoNick) {
                    mySQL.update("INSERT INTO `" + TABLE + "` VALUES (0, '" + uuid.toString() + "')");
                }
            } else {
                if (isAutoNick) {
                    mySQL.update("DELETE FROM `" + TABLE + "` WHERE `uuid`='" + uuid.toString() + "'");
                }
            }
        });
    }
}

package gc.grivyzom.rtp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerRTPDataManager {
    private File dataFile;
    private YamlConfiguration dataConfig;

    public PlayerRTPDataManager(File dataFolder) {
        dataFile = new File(dataFolder, "playerRTPData.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean hasUsedFreeRtp(UUID uuid) {
        return dataConfig.getBoolean("players." + uuid + ".usedFreeRtp", false);
    }

    public void setUsedFreeRtp(UUID uuid, boolean used) {
        dataConfig.set("players." + uuid + ".usedFreeRtp", used);
        save();
    }

    public boolean isFreeRtpEnabled(UUID uuid) {
        return dataConfig.getBoolean("players." + uuid + ".freeRtpEnabled", true);
    }

    public void setFreeRtpEnabled(UUID uuid, boolean enabled) {
        dataConfig.set("players." + uuid + ".freeRtpEnabled", enabled);
        save();
    }

    public String getFreeRtpWorld(UUID uuid) {
        return dataConfig.getString("players." + uuid + ".freeRtpWorld", "world");
    }

    public void setFreeRtpWorld(UUID uuid, String world) {
        dataConfig.set("players." + uuid + ".freeRtpWorld", world);
        save();
    }

    private void save() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

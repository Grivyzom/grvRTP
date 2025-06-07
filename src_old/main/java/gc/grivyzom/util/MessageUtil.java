package gc.grivyzom.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageUtil {
    private final FileConfiguration cfg;
    public MessageUtil(FileConfiguration cfg){ this.cfg = cfg; }

    public String msg(String key){
        return ChatColor.translateAlternateColorCodes('&',
                cfg.getString("messages."+key, "&cMensaje "+key+" no definido"));
    }

    public String format(String key, String[][] placeholders){
        String m = msg(key);
        for (String[] ph : placeholders){
            m = m.replace(ph[0], ph[1]);
        }
        return m;
    }
}

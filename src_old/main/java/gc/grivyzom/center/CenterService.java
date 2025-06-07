package gc.grivyzom.center;

import gc.grivyzom.GrvRTP;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class CenterService {

    private final GrvRTP plugin;

    public CenterService(GrvRTP plugin){
        this.plugin = plugin;
    }

    /** Devuelve {x, z} para el mundo indicado; si no existe vuelve a los valores globales o (0,0). */
    public int[] getCenter(World world){
        FileConfiguration cfg = plugin.getConfig();
        String path = "centers."+world.getName()+".";
        if(cfg.contains(path+"x") && cfg.contains(path+"z")){
            return new int[]{ cfg.getInt(path+"x"), cfg.getInt(path+"z") };
        }
        return new int[]{
                cfg.getInt("center-x", 0),
                cfg.getInt("center-z", 0)
        };
    }

    /** Guarda un nuevo centro para el mundo y persiste la configuración. */
    public void setCenter(World world, Location loc){
        String path = "centers."+world.getName()+".";
        plugin.getConfig().set(path+"x", loc.getBlockX());
        plugin.getConfig().set(path+"z", loc.getBlockZ());
        plugin.saveConfig();
    }
}

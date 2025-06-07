package gc.grivyzom.teleport;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;

public class RandomTeleportService {
    private final Random rnd = new Random();

    public Location randomLocation(World world, int centerX, int centerZ,
                                   int minRange, int maxRange){
        double dist = minRange + rnd.nextDouble() * (maxRange - minRange);
        double ang  = rnd.nextDouble() * 2 * Math.PI;
        int x = centerX + (int)(Math.cos(ang) * dist);
        int z = centerZ + (int)(Math.sin(ang) * dist);
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x, y, z);
    }
}

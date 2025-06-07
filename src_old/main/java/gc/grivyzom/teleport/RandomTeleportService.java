package gc.grivyzom.teleport;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class RandomTeleportService {
    private final Random rnd = new Random();
    private final SafetyService safetyService;
    private final FileConfiguration config;

    public RandomTeleportService(FileConfiguration config) {
        this.config = config;
        this.safetyService = new SafetyService(config);
    }

    /**
     * Genera una ubicación aleatoria segura dentro del rango especificado
     */
    public Location findSafeRandomLocation(World world, int centerX, int centerZ,
                                           int minRange, int maxRange) {
        int maxAttempts = config.getInt("teleport.max-attempts", 50);
        int minY = config.getInt("teleport.min-y", 64);
        int maxY = config.getInt("teleport.max-y", 256);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generar ubicación aleatoria
            Location randomLoc = generateRandomLocation(world, centerX, centerZ, minRange, maxRange);

            // Ajustar altura
            int x = randomLoc.getBlockX();
            int z = randomLoc.getBlockZ();
            int y = world.getHighestBlockYAt(x, z) + 1;

            // Aplicar límites de altura
            if (y < minY) y = minY;
            if (y > maxY) y = maxY;

            randomLoc.setY(y);

            // Verificar si la ubicación es segura
            if (safetyService.isSafeLocation(randomLoc)) {
                return randomLoc;
            }

            // Intentar encontrar una ubicación segura cerca
            Location safeLoc = safetyService.findSafeLocation(randomLoc, 10);
            if (safeLoc != null) {
                return safeLoc;
            }
        }

        // No se encontró ubicación segura después de todos los intentos
        return null;
    }

    /**
     * Genera una ubicación aleatoria sin verificaciones de seguridad
     */
    private Location generateRandomLocation(World world, int centerX, int centerZ,
                                            int minRange, int maxRange) {
        double dist = minRange + rnd.nextDouble() * (maxRange - minRange);
        double ang = rnd.nextDouble() * 2 * Math.PI;
        int x = centerX + (int)(Math.cos(ang) * dist);
        int z = centerZ + (int)(Math.sin(ang) * dist);
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * Método legacy para compatibilidad (deprecated)
     * @deprecated Use findSafeRandomLocation instead
     */
    @Deprecated
    public Location randomLocation(World world, int centerX, int centerZ,
                                   int minRange, int maxRange) {
        return findSafeRandomLocation(world, centerX, centerZ, minRange, maxRange);
    }

    /**
     * Recarga la configuración del servicio de seguridad
     */
    public void reloadConfig() {
        safetyService.reloadBlockLists();
    }
}
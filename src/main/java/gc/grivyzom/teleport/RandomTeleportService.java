package gc.grivyzom.teleport;

import gc.grivyzom.teleport.SafetyService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Random;

public class RandomTeleportService {
    private final Random rnd = new Random();
    private SafetyService safetyService;

    public RandomTeleportService() {
        // Constructor sin parámetros para mantener compatibilidad
    }

    public RandomTeleportService(FileConfiguration config) {
        if (config != null) {
            this.safetyService = new SafetyService(config);
        }
    }

    /**
     * Inicializa el servicio de seguridad (para compatibilidad con código existente)
     */
    public void initSafety(FileConfiguration config) {
        if (config != null) {
            this.safetyService = new SafetyService(config);
        }
    }

    /**
     * Genera una ubicación aleatoria segura
     */
    public Location randomLocation(World world, int centerX, int centerZ, int minRange, int maxRange) {
        return randomLocation(world, centerX, centerZ, minRange, maxRange, true);
    }

    /**
     * Genera una ubicación aleatoria con opción de verificación de seguridad
     */
    public Location randomLocation(World world, int centerX, int centerZ,
                                   int minRange, int maxRange, boolean enforceSafety) {

        // Si no hay servicio de seguridad o está desactivado, usar método original
        if (safetyService == null || !enforceSafety) {
            return randomLocationUnsafe(world, centerX, centerZ, minRange, maxRange);
        }

        // Intentar encontrar una ubicación segura
        return findSafeRandomLocation(world, centerX, centerZ, minRange, maxRange);
    }

    /**
     * Método original sin verificaciones de seguridad
     */
    private Location randomLocationUnsafe(World world, int centerX, int centerZ,
                                          int minRange, int maxRange) {
        double dist = minRange + rnd.nextDouble() * (maxRange - minRange);
        double ang = rnd.nextDouble() * 2 * Math.PI;
        int x = centerX + (int)(Math.cos(ang) * dist);
        int z = centerZ + (int)(Math.sin(ang) * dist);
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * Busca una ubicación aleatoria segura con múltiples intentos
     */
    private Location findSafeRandomLocation(World world, int centerX, int centerZ,
                                            int minRange, int maxRange) {

        int maxAttempts = 50; // Valor por defecto
        int safeSearchRadius = 10; // Valor por defecto

        // Intentar obtener configuración si está disponible
        try {
            if (safetyService != null) {
                // Estos valores se pueden obtener del SafetyService si es necesario
                // Por ahora usar valores por defecto
            }
        } catch (Exception e) {
            // Usar valores por defecto en caso de error
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generar coordenadas aleatorias
            double dist = minRange + rnd.nextDouble() * (maxRange - minRange);
            double ang = rnd.nextDouble() * 2 * Math.PI;
            int x = centerX + (int)(Math.cos(ang) * dist);
            int z = centerZ + (int)(Math.sin(ang) * dist);
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            // Verificar si la ubicación es segura
            if (safetyService.isSafeLocation(candidate)) {
                return candidate;
            }

            // Si no es segura, intentar encontrar una ubicación segura cercana
            Location safeLocation = safetyService.findSafeLocation(candidate, safeSearchRadius);
            if (safeLocation != null) {
                return safeLocation;
            }
        }

        // Si no se encontró ubicación segura después de todos los intentos,
        // devolver una ubicación sin verificar como último recurso
        return randomLocationUnsafe(world, centerX, centerZ, minRange, maxRange);
    }

    /**
     * Recarga la configuración del servicio de seguridad
     */
    public void reloadSafety() {
        if (safetyService != null) {
            safetyService.reloadBlockLists();
        }
    }

    /**
     * Verifica si una ubicación específica es segura
     */
    public boolean isSafeLocation(Location location) {
        if (safetyService == null) {
            return true; // Si no hay servicio de seguridad, asumir que es seguro
        }
        return safetyService.isSafeLocation(location);
    }

    /**
     * Busca una ubicación segura cerca de una ubicación dada
     */
    public Location findSafeLocation(Location location, int radius) {
        if (safetyService == null) {
            return location; // Si no hay servicio de seguridad, devolver la ubicación original
        }
        return safetyService.findSafeLocation(location, radius);
    }

    /**
     * Obtiene el servicio de seguridad
     */
    public SafetyService getSafetyService() {
        return safetyService;
    }
}
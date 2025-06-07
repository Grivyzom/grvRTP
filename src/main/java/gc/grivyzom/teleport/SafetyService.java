package gc.grivyzom.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SafetyService {

    private final Set<Material> bannedBlocks;
    private final Set<Material> safeBlocks;
    private final FileConfiguration config;

    public SafetyService(FileConfiguration config) {
        this.config = config;
        this.bannedBlocks = new HashSet<>();
        this.safeBlocks = new HashSet<>();
        loadBlockLists();
    }

    /**
     * Carga las listas de bloques desde la configuración
     */
    private void loadBlockLists() {
        bannedBlocks.clear();
        safeBlocks.clear();

        // Cargar bloques prohibidos
        List<String> bannedList = config.getStringList("banned-blocks");
        for (String blockName : bannedList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                bannedBlocks.add(material);
            } catch (IllegalArgumentException e) {
                // Log de error: bloque no válido
                System.err.println("Bloque no válido en banned-blocks: " + blockName);
            }
        }

        // Cargar bloques seguros
        List<String> safeList = config.getStringList("safe-blocks");
        for (String blockName : safeList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                safeBlocks.add(material);
            } catch (IllegalArgumentException e) {
                // Log de error: bloque no válido
                System.err.println("Bloque no válido en safe-blocks: " + blockName);
            }
        }
    }

    /**
     * Recarga las listas de bloques desde la configuración
     */
    public void reloadBlockLists() {
        loadBlockLists();
    }

    /**
     * Verifica si una ubicación es segura para el teletransporte
     */
    public boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Verificar límites del mundo
        if (y < world.getMinHeight() || y > world.getMaxHeight() - 2) {
            return false;
        }

        // Verificar bloques críticos
        Block groundBlock = world.getBlockAt(x, y - 1, z);  // Bloque donde parará
        Block feetBlock = world.getBlockAt(x, y, z);        // Bloque a la altura de los pies
        Block headBlock = world.getBlockAt(x, y + 1, z);    // Bloque a la altura de la cabeza

        // El jugador necesita espacio para aparecer (2 bloques de aire)
        if (!isAirOrPassable(feetBlock) || !isAirOrPassable(headBlock)) {
            return false;
        }

        // El bloque del suelo no debe ser prohibido
        if (isBannedBlock(groundBlock.getType())) {
            return false;
        }

        // Si hay lista de bloques seguros, verificar que el suelo esté en ella
        if (!safeBlocks.isEmpty() && !safeBlocks.contains(groundBlock.getType())) {
            return false;
        }

        // El bloque del suelo debe ser sólido (no aire)
        if (!groundBlock.getType().isSolid()) {
            return false;
        }

        // Verificaciones adicionales de seguridad
        if (config.getBoolean("teleport.safety.check-surrounding", true)) {
            if (!isSurroundingSafe(location)) {
                return false;
            }
        }

        if (config.getBoolean("teleport.safety.avoid-caves", true)) {
            if (isInCave(location)) {
                return false;
            }
        }

        int minLight = config.getInt("teleport.safety.min-light-level", -1);
        if (minLight >= 0 && location.getBlock().getLightLevel() < minLight) {
            return false;
        }

        return true;
    }

    /**
     * Verifica si un bloque es aire o pasable (como hierba alta, flores, etc.)
     */
    private boolean isAirOrPassable(Block block) {
        Material type = block.getType();
        return type.isAir() ||
                !type.isSolid() ||
                type == Material.GRASS ||
                type == Material.TALL_GRASS ||
                type == Material.FERN ||
                type == Material.LARGE_FERN ||
                type == Material.DANDELION ||
                type == Material.POPPY ||
                type.name().endsWith("_TULIP") ||
                type.name().contains("FLOWER") ||
                type == Material.SNOW;
    }

    /**
     * Verifica si un tipo de bloque está en la lista de prohibidos
     */
    private boolean isBannedBlock(Material material) {
        return bannedBlocks.contains(material);
    }

    /**
     * Verifica que los bloques circundantes sean seguros
     */
    private boolean isSurroundingSafe(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Verificar un radio de 1 bloque alrededor
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y - 1, z + dz);
                if (isBannedBlock(block.getType())) {
                    return false;
                }

                // Verificar también bloques a la altura de los pies
                Block feetBlock = world.getBlockAt(x + dx, y, z + dz);
                if (isBannedBlock(feetBlock.getType()) && feetBlock.getType().isSolid()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verifica si la ubicación está en una cueva (poca luz natural)
     */
    private boolean isInCave(Location location) {
        // Verificar si hay cielo abierto en un radio pequeño
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int highestY = world.getHighestBlockYAt(x + dx, z + dz);
                if (Math.abs(location.getY() - highestY) <= 5) {
                    return false; // Hay superficie cerca, no es una cueva profunda
                }
            }
        }

        return true; // Probablemente es una cueva
    }

    /**
     * Encuentra una ubicación segura cerca de la ubicación dada
     */
    public Location findSafeLocation(Location originalLocation, int maxRadius) {
        if (isSafeLocation(originalLocation)) {
            return originalLocation;
        }

        World world = originalLocation.getWorld();
        int originalX = originalLocation.getBlockX();
        int originalZ = originalLocation.getBlockZ();

        // Buscar en espiral desde la ubicación original
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Solo verificar el borde del radio actual
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = originalX + dx;
                    int z = originalZ + dz;
                    int y = world.getHighestBlockYAt(x, z) + 1;

                    // Verificar límites de altura configurados
                    int minY = config.getInt("teleport.min-y", 64);
                    int maxY = config.getInt("teleport.max-y", 256);

                    if (y < minY) y = minY;
                    if (y > maxY) y = maxY;

                    Location testLocation = new Location(world, x + 0.5, y, z + 0.5);
                    if (isSafeLocation(testLocation)) {
                        return testLocation;
                    }
                }
            }
        }

        return null; // No se encontró ubicación segura
    }
}
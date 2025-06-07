package gc.grivyzom.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomTeleportService {
    private final Random rnd = new Random();
    private final Set<Material> bannedBlocks = new HashSet<>();
    private final int maxAttempts;
    private final int minY;
    private final int maxY;

    public RandomTeleportService(FileConfiguration config) {
        this.maxAttempts = config.getInt("teleport.max-attempts", 20);
        this.minY = config.getInt("teleport.min-y", 64);
        this.maxY = config.getInt("teleport.max-y", 256);
        loadBannedBlocks(config);
    }

    private void loadBannedBlocks(FileConfiguration config) {
        bannedBlocks.clear();
        List<String> bannedList = config.getStringList("teleport.banned-blocks");

        for (String materialName : bannedList) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                bannedBlocks.add(material);
            } catch (IllegalArgumentException e) {
                // Log del material inválido pero continúa cargando los demás
                System.out.println("[GrvRTP] Material inválido ignorado: " + materialName);
            }
        }
    }

    public Location randomLocation(World world, int centerX, int centerZ, int minRange, int maxRange) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generar coordenadas aleatorias
            double dist = minRange + rnd.nextDouble() * (maxRange - minRange);
            double angle = rnd.nextDouble() * 2 * Math.PI;
            int x = centerX + (int) (Math.cos(angle) * dist);
            int z = centerZ + (int) (Math.sin(angle) * dist);

            // Buscar ubicación segura
            Location safeLocation = findSafeLocation(world, x, z);
            if (safeLocation != null) {
                return safeLocation;
            }
        }

        // Si no se encuentra ubicación segura después de los intentos, usar ubicación básica
        int x = centerX + (int) (Math.cos(rnd.nextDouble() * 2 * Math.PI) * minRange);
        int z = centerZ + (int) (Math.sin(rnd.nextDouble() * 2 * Math.PI) * minRange);
        int y = Math.max(world.getHighestBlockYAt(x, z) + 1, minY);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private Location findSafeLocation(World world, int x, int z) {
        // Buscar desde arriba hacia abajo
        for (int y = Math.min(maxY, world.getMaxHeight() - 1); y >= minY; y--) {
            if (isSafeLocation(world, x, y, z)) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private boolean isSafeLocation(World world, int x, int y, int z) {
        // Verificar que las coordenadas estén dentro de los límites del mundo
        if (y < minY || y >= world.getMaxHeight() - 1) {
            return false;
        }

        Block groundBlock = world.getBlockAt(x, y - 1, z);  // Bloque donde pisará
        Block feetBlock = world.getBlockAt(x, y, z);        // Bloque a la altura de los pies
        Block headBlock = world.getBlockAt(x, y + 1, z);    // Bloque a la altura de la cabeza

        // El suelo debe ser sólido y no estar en la lista de bloques prohibidos
        if (!groundBlock.getType().isSolid() || bannedBlocks.contains(groundBlock.getType())) {
            return false;
        }

        // Los bloques de pies y cabeza deben estar libres (aire o materiales seguros)
        if (!isPassableBlock(feetBlock) || !isPassableBlock(headBlock)) {
            return false;
        }

        // Verificaciones adicionales de seguridad
        if (bannedBlocks.contains(feetBlock.getType()) || bannedBlocks.contains(headBlock.getType())) {
            return false;
        }

        // Verificar que no sea lava por debajo (en un radio pequeño)
        if (isLavaNearby(world, x, y - 1, z)) {
            return false;
        }

        return true;
    }

    private boolean isPassableBlock(Block block) {
        Material type = block.getType();
        return type.isAir() ||
                type == Material.GRASS ||
                type == Material.TALL_GRASS ||
                type == Material.FERN ||
                type == Material.LARGE_FERN ||
                type == Material.DEAD_BUSH ||
                type == Material.DANDELION ||
                type == Material.POPPY ||
                type == Material.SNOW;
    }

    private boolean isLavaNearby(World world, int x, int y, int z) {
        // Verificar un área de 3x3 alrededor del punto
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.LAVA) {
                    return true;
                }
            }
        }
        return false;
    }

    // Método para recargar la configuración sin reiniciar el plugin
    public void reloadConfig(FileConfiguration config) {
        loadBannedBlocks(config);
    }

    // Getter para debugging
    public Set<Material> getBannedBlocks() {
        return new HashSet<>(bannedBlocks);
    }
}
package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.teleport.SafetyService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SafetyCommand implements CommandExecutor {

    private final GrvRTP plugin;
    private final MessageUtil msgUtil;

    public SafetyCommand(GrvRTP plugin, MessageUtil msgUtil) {
        this.plugin = plugin;
        this.msgUtil = msgUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("grvrtp.admin")) {
            sender.sendMessage(msgUtil.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "check":
                return handleCheck(sender);

            case "test":
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /grvrtp safety test <x> <y> <z>");
                    return true;
                }
                return handleTest(sender, args);

            case "info":
                return handleInfo(sender);

            case "reload":
                return handleReload(sender);

            default:
                showUsage(sender);
                return true;
        }
    }

    private boolean handleCheck(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msgUtil.msg("player-only"));
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();

        SafetyService safetyService = plugin.getTeleportService().getSafetyService();
        if (safetyService == null) {
            sender.sendMessage("§cSistema de seguridad no inicializado.");
            return true;
        }

        boolean isSafe = safetyService.isSafeLocation(location);
        String coords = location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

        if (isSafe) {
            sender.sendMessage("§aLa ubicación actual (" + coords + ") es SEGURA para RTP.");
        } else {
            sender.sendMessage("§cLa ubicación actual (" + coords + ") NO es segura para RTP.");

            // Intentar encontrar una ubicación segura cercana
            Location safeLocation = safetyService.findSafeLocation(location, 10);
            if (safeLocation != null) {
                String safeCoords = safeLocation.getBlockX() + ", " + safeLocation.getBlockY() + ", " + safeLocation.getBlockZ();
                sender.sendMessage("§eUbicación segura más cercana: " + safeCoords);
            } else {
                sender.sendMessage("§cNo se encontró una ubicación segura cercana.");
            }
        }

        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);

            if (!(sender instanceof Player)) {
                sender.sendMessage(msgUtil.msg("player-only"));
                return true;
            }

            Player player = (Player) sender;
            Location testLocation = new Location(player.getWorld(), x, y, z);

            SafetyService safetyService = plugin.getTeleportService().getSafetyService();
            if (safetyService == null) {
                sender.sendMessage("§cSistema de seguridad no inicializado.");
                return true;
            }

            boolean isSafe = safetyService.isSafeLocation(testLocation);
            String coords = x + ", " + y + ", " + z;

            if (isSafe) {
                sender.sendMessage("§aLa ubicación (" + coords + ") es SEGURA para RTP.");
            } else {
                sender.sendMessage("§cLa ubicación (" + coords + ") NO es segura para RTP.");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordenadas inválidas. Usa números enteros.");
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        boolean safetyEnabled = plugin.getConfig().getBoolean("teleport.safety.enabled", true);
        int maxAttempts = plugin.getConfig().getInt("teleport.safety.max-attempts", 50);
        int searchRadius = plugin.getConfig().getInt("teleport.safety.safe-search-radius", 10);
        boolean checkSurrounding = plugin.getConfig().getBoolean("teleport.safety.check-surrounding", true);
        boolean avoidCaves = plugin.getConfig().getBoolean("teleport.safety.avoid-caves", true);
        int minLight = plugin.getConfig().getInt("teleport.safety.min-light-level", 7);

        int bannedCount = plugin.getConfig().getStringList("banned-blocks").size();
        int safeCount = plugin.getConfig().getStringList("safe-blocks").size();

        sender.sendMessage("§6========== §eInformación del Sistema de Seguridad §6==========");
        sender.sendMessage("§eEstado: " + (safetyEnabled ? "§aActivado" : "§cDesactivado"));
        sender.sendMessage("§eIntentos máximos: §f" + maxAttempts);
        sender.sendMessage("§eRadio de búsqueda: §f" + searchRadius);
        sender.sendMessage("§eVerificar alrededores: " + (checkSurrounding ? "§aActivado" : "§cDesactivado"));
        sender.sendMessage("§eEvitar cuevas: " + (avoidCaves ? "§aActivado" : "§cDesactivado"));
        sender.sendMessage("§eNivel mínimo de luz: §f" + (minLight >= 0 ? minLight : "Desactivado"));
        sender.sendMessage("§eBloques prohibidos: §f" + bannedCount);
        sender.sendMessage("§eBloques seguros: §f" + safeCount);

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getTeleportService().reloadSafety();
        sender.sendMessage("§aConfiguración del sistema de seguridad recargada.");
        return true;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage("§6========== §eComandos de Seguridad RTP §6==========");
        sender.sendMessage("§e/grvrtp safety check §7- Verificar ubicación actual");
        sender.sendMessage("§e/grvrtp safety test <x> <y> <z> §7- Probar ubicación específica");
        sender.sendMessage("§e/grvrtp safety info §7- Mostrar información del sistema");
        sender.sendMessage("§e/grvrtp safety reload §7- Recargar configuración de seguridad");
    }
}
package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.center.CenterService;
import gc.grivyzom.economy.EconomyService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CenterCommand implements CommandExecutor {

    private final GrvRTP plugin;
    private final CenterService centerService;
    private final EconomyService economyService;
    private final MessageUtil msgUtil;

    public CenterCommand(GrvRTP plugin, CenterService centerService, EconomyService economyService) {
        this.plugin = plugin;
        this.centerService = centerService;
        this.economyService = economyService;
        this.msgUtil = new MessageUtil(plugin.getConfig());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Verificar permiso básico
        if (!sender.hasPermission("grvrtp.center")) {
            sender.sendMessage(msgUtil.msg("no-permission"));
            return true;
        }

        Player target;
        World world;

        try {
            switch (args.length) {
                case 0 -> {
                    // /centro - teletransportar al ejecutor al centro de su mundo actual
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(msgUtil.msg("player-only"));
                        return true;
                    }
                    target = (Player) sender;
                    world = target.getWorld();
                }
                case 1 -> {
                    // /centro <mundo> - teletransportar al ejecutor al centro del mundo especificado
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(msgUtil.msg("player-only"));
                        return true;
                    }
                    target = (Player) sender;
                    world = getWorld(args[0]);
                }
                case 2 -> {
                    // /centro <mundo> <jugador> - teletransportar a otro jugador
                    if (!sender.hasPermission("grvrtp.center.others")) {
                        sender.sendMessage(msgUtil.msg("no-permission-others"));
                        return true;
                    }
                    world = getWorld(args[0]);
                    target = getPlayer(args[1]);
                }
                default -> {
                    sender.sendMessage("§cUso: /centro [mundo] [jugador]");
                    return true;
                }
            }

            // Obtener coordenadas del centro para este mundo
            int[] center = centerService.getCenter(world);
            int centerX = center[0];
            int centerZ = center[1];

            // Calcular distancia desde el jugador al centro (solo para economía)
            double distance = 0;
            if (sender.equals(target) && target instanceof Player) {
                Location playerLoc = target.getLocation();
                if (playerLoc.getWorld().equals(world)) {
                    // Si está en el mismo mundo, calcular distancia real
                    distance = Math.sqrt(Math.pow(playerLoc.getX() - centerX, 2) +
                            Math.pow(playerLoc.getZ() - centerZ, 2));
                } else {
                    // Si está en otro mundo, usar distancia máxima configurada
                    distance = plugin.getConfig().getInt("max-range", 20000);
                }
            }

            // Verificar y cobrar economía (solo si el ejecutor es el objetivo)
            if (sender.equals(target) && economyService.isEnabled()) {
                int maxRange = plugin.getConfig().getInt("max-range", 20000);

                // Mostrar información de coste (opcional)
                economyService.showCostInfo(target, "center", distance, maxRange);

                // Procesar el pago
                if (!economyService.processPayment(target, "center", distance, maxRange)) {
                    return true; // El mensaje de error ya se envía en processPayment
                }
            }

            // Teletransportar al centro
            int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
            Location centerLoc = new Location(world, centerX + 0.5, y, centerZ + 0.5);
            target.teleport(centerLoc);

            // Mensajes de confirmación
            String locStr = centerX + ", " + y + ", " + centerZ;
            target.sendMessage(msgUtil.format("center-success",
                    new String[][]{{"%world%", world.getName()}, {"%loc%", locStr}}));

            if (!sender.equals(target)) {
                sender.sendMessage(msgUtil.format("center-other",
                        new String[][]{{"%player%", target.getName()}, {"%world%", world.getName()}}));
            }

            return true;

        } catch (IllegalArgumentException ex) {
            sender.sendMessage(msgUtil.format("invalid-arg", new String[][]{{"%arg%", ex.getMessage()}}));
            return true;
        }
    }

    private Player getPlayer(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) throw new IllegalArgumentException("jugador");
        return p;
    }

    private World getWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null) throw new IllegalArgumentException("mundo");
        return w;
    }
}
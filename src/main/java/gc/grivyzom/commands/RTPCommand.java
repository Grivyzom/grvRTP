package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.economy.EconomyService;
import gc.grivyzom.rtp.PlayerRTPDataManager;
import gc.grivyzom.teleport.RandomTeleportService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

public class RTPCommand implements CommandExecutor {

    private final GrvRTP plugin;
    private final MessageUtil msg;
    private final EconomyService economyService;
    private final RandomTeleportService tpService;
    private final int globalMin;
    private final int globalMax;

    public RTPCommand(GrvRTP plugin, EconomyService economyService){
        this.plugin = plugin;
        this.economyService = economyService;
        FileConfiguration cfg = plugin.getConfig();
        this.globalMin = Math.max(cfg.getInt("min-range",150), 1);
        this.globalMax = Math.max(cfg.getInt("max-range",20000), globalMin+1);
        this.msg = new MessageUtil(cfg);

        // Inicializar el servicio de teletransporte con la configuración
        this.tpService = new RandomTeleportService(cfg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args){

        // ------- Permiso básico -------
        if(!sender.hasPermission("grvrtp.use")){
            sender.sendMessage(msg.msg("no-permission"));
            return true;
        }

        Player target;
        int range;
        World world;

        try {
            // ---------- Argumentos ----------
            switch (args.length){
                case 0 -> {            // /rtp (sin argumentos)
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(msg.msg("player-only"));
                        return true;
                    }
                    checkPlayer(sender);
                    target = (Player) sender;
                    world = target.getWorld();
                    range = globalMax;
                }
                case 1 -> {
                    if(isNumeric(args[0])){
                        checkPlayer(sender);
                        target = (Player) sender;
                        range  = parseRange(args[0]);
                        world  = target.getWorld();
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(msg.msg("player-only"));
                            return true;
                        }
                    }else{
                        checkPermission(sender, "grvrtp.use.others");
                        target = getPlayer(args[0]);
                        range  = globalMax;
                        world  = target.getWorld();
                    }
                }
                case 2 -> {
                    if(isNumeric(args[1])){                 // a)
                        checkPermission(sender, "grvrtp.use.others");
                        target = getPlayer(args[0]);
                        range  = parseRange(args[1]);
                        world  = target.getWorld();
                    }else if(isNumeric(args[0])){           // c)
                        checkPlayer(sender);
                        target = (Player) sender;
                        range  = parseRange(args[0]);
                        world  = getWorld(args[1]);
                    }else{                                  // b)
                        checkPermission(sender, "grvrtp.use.others");
                        target = getPlayer(args[0]);
                        range  = globalMax;
                        world  = getWorld(args[1]);
                    }
                }
                case 3 -> {            // /rtp <jugador> <rango> <mundo>
                    checkPermission(sender, "grvrtp.use.others");
                    target = getPlayer(args[0]);
                    range = parseRange(args[1]);
                    world = getWorld(args[2]);
                }
                default -> { sender.sendMessage(msg.msg("usage")); return true; }
            }

            // Get player data before world restriction check
            UUID uuid = target.getUniqueId();
            PlayerRTPDataManager dataManager = plugin.playerRTPDataManager;

            // 2. Restricción de mundo modificada:
            if (!(dataManager.isFreeRtpEnabled(uuid) && !dataManager.hasUsedFreeRtp(uuid))) {
                if(!isWorldAllowed(world) && !sender.hasPermission("grvrtp.bypass.world")){
                    sender.sendMessage(msg.format("world-restricted",
                            new String[][]{{"%world%", world.getName()}}));
                    return true;
                }
            }

            if (sender.equals(target)) {
                if (dataManager.isFreeRtpEnabled(uuid) && !dataManager.hasUsedFreeRtp(uuid)) {
                    String allowedWorld = dataManager.getFreeRtpWorld(uuid);
                    if (world.getName().equals(allowedWorld)) {
                        // Free RTP available
                        dataManager.setUsedFreeRtp(uuid, true);
                        target.sendMessage(msg.msg("free-rtp-used"));
                        // Skip economy payment
                    } else {
                        // Player is in a different world than allowedWorld
                        // Redirect to "world" with fixed range 1500
                        target.sendMessage(msg.format("free-rtp-wrong-world",
                                new String[][]{{"%world%", allowedWorld}}));
                        target.sendMessage("§eTe estamos teletransportando a \"world\" (rango fijo 1500)...");

                        World defaultWorld = Bukkit.getWorld("world");
                        if (defaultWorld != null) {
                            gc.grivyzom.center.CenterService centerService =
                                    new gc.grivyzom.center.CenterService(plugin);
                            int[] center = centerService.getCenter(defaultWorld);
                            int cx = center[0];
                            int cz = center[1];

                            int min = plugin.getConfig().getInt("min-range");
                            int fixedRange = 1500;

                            // Mostrar mensaje de búsqueda si la seguridad está activada
                            if (plugin.getConfig().getBoolean("teleport.safety.enabled", true)) {
                                target.sendMessage(msg.msg("teleport-searching"));
                            }

                            Location dest = tpService.randomLocation(defaultWorld, cx, cz, min, fixedRange);

                            // Verificar si la ubicación es segura
                            if (!tpService.isSafeLocation(dest)) {
                                target.sendMessage(msg.msg("teleport-unsafe"));
                                return true;
                            }

                            target.teleport(dest);
                            String locStr = dest.getBlockX() + ", " + dest.getBlockY() + ", " + dest.getBlockZ();
                            target.sendMessage(msg.format("teleport-success",
                                    new String[][]{
                                            {"%loc%", locStr},
                                            {"%world%", defaultWorld.getName()}
                                    }));

                            if (!sender.equals(target)) {
                                sender.sendMessage(msg.format("teleport-other",
                                        new String[][]{{"%player%", target.getName()}}));
                            }
                        } else {
                            target.sendMessage("§cError interno: no existe el mundo \"world\".");
                        }

                        return true;
                    }
                } else if (economyService.isEnabled()) {
                    // Normal economy payment
                    double distance = range;
                    economyService.showCostInfo(target, "rtp", distance, globalMax);
                    if (!economyService.processPayment(target, "rtp", distance, globalMax)) {
                        return true;
                    }
                }
            }

            // ------- Teletransporte -------
            int min = plugin.getConfig().getInt("min-range");

            // Usar CenterService para obtener el centro del mundo específico
            gc.grivyzom.center.CenterService centerService = new gc.grivyzom.center.CenterService(plugin);
            int[] center = centerService.getCenter(world);
            int cx = center[0];
            int cz = center[1];

            // Mostrar mensaje de búsqueda si la seguridad está activada
            boolean safetyEnabled = plugin.getConfig().getBoolean("teleport.safety.enabled", true);
            if (safetyEnabled) {
                target.sendMessage(msg.msg("teleport-searching"));
            }

            Location dest = tpService.randomLocation(world, cx, cz, min, range);

            // Verificar si la ubicación final es segura
            if (safetyEnabled && !tpService.isSafeLocation(dest)) {
                target.sendMessage(msg.msg("teleport-unsafe"));
                return true;
            }

            target.teleport(dest);

            String locStr = dest.getBlockX()+", "+dest.getBlockY()+", "+dest.getBlockZ();
            target.sendMessage(msg.format("teleport-success",
                    new String[][]{{"%loc%", locStr},{"%world%", world.getName()}}));

            if(!sender.equals(target)){
                sender.sendMessage(msg.format("teleport-other",
                        new String[][]{{"%player%", target.getName()}}));
            }
            return true;

        } catch (IllegalArgumentException ex){
            sender.sendMessage(msg.format("invalid-arg", new String[][]{{"%arg%", ex.getMessage()}}));
            return true;
        }
    }

    // -------------- Utilidades privadas --------------
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void checkPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalArgumentException("player-only");
        }
    }

    private void checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw new IllegalArgumentException("no-permission-others");
        }
    }

    private int parseRange(String s){
        int r = Integer.parseInt(s);
        if(r < globalMin || r > globalMax)
            throw new NumberFormatException(
                    msg.format("range-error", new String[][]{
                            {"%min%", String.valueOf(globalMin)},
                            {"%max%", String.valueOf(globalMax)}}));
        return r;
    }
    private Player getPlayer(String name){
        Player p = Bukkit.getPlayerExact(name);
        if(p==null) throw new IllegalArgumentException("jugador");
        return p;
    }
    private World getWorld(String name){
        World w = Bukkit.getWorld(name);
        if(w==null) throw new IllegalArgumentException("mundo");
        return w;
    }
    private boolean isWorldAllowed(World w){
        var cfg = plugin.getConfig();
        String mode = cfg.getString("worlds.mode","blacklist").toLowerCase();
        List<String> list = cfg.getStringList("worlds.list");
        boolean contains = list.stream().anyMatch(s -> s.equalsIgnoreCase(w.getName()));
        return mode.equals("whitelist") ? contains : !contains;
    }
}
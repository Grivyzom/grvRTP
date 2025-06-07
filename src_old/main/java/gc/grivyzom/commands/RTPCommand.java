package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.teleport.RandomTeleportService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.List;

public class RTPCommand implements CommandExecutor {

    private final GrvRTP plugin;
    private final MessageUtil msg;
    private final RandomTeleportService tpService;
    private final int globalMin;
    private final int globalMax;

    public RTPCommand(GrvRTP plugin){
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfig();
        // Usa 150-20000 si están ausentes
        this.globalMin = Math.max(cfg.getInt("min-range",150), 1);
        this.globalMax = Math.max(cfg.getInt("max-range",20000), globalMin+1);
        this.msg = new MessageUtil(cfg);
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
                    checkPlayer(sender);
                    target = (Player) sender;
                    range = globalMax;
                    world = target.getWorld();
                }
                case 1 -> {
                    // Si es número → rango propio; si es nombre de jugador → teleportar con rango por defecto (globalMax)
                    if(isNumeric(args[0])){
                        checkPlayer(sender);                 // asegura que el ejecutor sea jugador
                        target = (Player) sender;
                        range  = parseRange(args[0]);
                        world  = target.getWorld();
                    }else{
                        checkPermission(sender, "grvrtp.use.others");
                        target = getPlayer(args[0]);
                        range  = globalMax;
                        world  = target.getWorld();
                    }
                }
                case 2 -> {
                    // 3 combinaciones posibles:
                    // a) <player> <rango>
                    // b) <player> <world>
                    // c) <rango>  <world>
                    if(isNumeric(args[1])){                 // [a]
                        checkPermission(sender, "grvrtp.use.others");
                        target = getPlayer(args[0]);
                        range  = parseRange(args[1]);
                        world  = target.getWorld();
                    }else if(isNumeric(args[0])){           // [c]
                        checkPlayer(sender);
                        target = (Player) sender;
                        range  = parseRange(args[0]);
                        world  = getWorld(args[1]);
                    }else{                                  // [b]
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

            // ------- Restricción de mundo -------
            if(!isWorldAllowed(world) && !sender.hasPermission("grvrtp.bypass.world")){
                sender.sendMessage(msg.format("world-restricted",
                        new String[][]{{"%world%", world.getName()}}));
                return true;
            }

            // ------- Obtener centro del mundo -------
            int[] center = getCenterForWorld(world);
            int cx = center[0];
            int cz = center[1];

            // ------- Buscar ubicación segura -------
            int min = plugin.getConfig().getInt("min-range");
            Location dest = tpService.findSafeRandomLocation(world, cx, cz, min, range);

            if (dest == null) {
                // No se encontró ubicación segura
                int maxAttempts = plugin.getConfig().getInt("teleport.max-attempts", 50);
                sender.sendMessage(msg.format("teleport-failed",
                        new String[][]{{"%attempts%", String.valueOf(maxAttempts)}}));
                return true;
            }

            // ------- Teletransporte -------
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

    /**
     * Obtiene el centro configurado para un mundo específico
     */
    private int[] getCenterForWorld(World world) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "centers." + world.getName() + ".";

        if (cfg.contains(path + "x") && cfg.contains(path + "z")) {
            return new int[]{
                    cfg.getInt(path + "x"),
                    cfg.getInt(path + "z")
            };
        }

        // Usar centro global si no hay específico para el mundo
        return new int[]{
                cfg.getInt("center-x", 0),
                cfg.getInt("center-z", 0)
        };
    }

    /**
     * Recarga la configuración del servicio de teletransporte
     */
    public void reloadConfig() {
        tpService.reloadConfig();
    }
    }
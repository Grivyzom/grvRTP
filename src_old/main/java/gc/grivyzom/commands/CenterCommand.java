package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.center.CenterService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class CenterCommand implements CommandExecutor {

    private final GrvRTP plugin;
    private final CenterService centerService;
    private final MessageUtil msg;

    public CenterCommand(GrvRTP plugin, CenterService centerService) {
        this.plugin = plugin;
        this.centerService = centerService;
        this.msg = new MessageUtil(plugin.getConfig());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {

        // --- permiso básico ---
        if (!sender.hasPermission("grvrtp.center")) {
            sender.sendMessage(msg.msg("no-permission"));
            return true;
        }

        Player target;
        World world;

        try {
            switch (args.length) {
                case 0 -> {                     // /centro
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(msg.msg("player-only")); return true;
                    }
                    target = p;
                    world = p.getWorld();
                }
                case 1 -> {                     // /centro <mundo>   o   /centro <jugador>
                    World w = Bukkit.getWorld(args[0]);
                    if (w != null) {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(msg.msg("player-only")); return true;
                        }
                        target = p;
                        world = w;
                    } else {                      // se trata de un jugador
                        if (!sender.hasPermission("grvrtp.center.others")) {
                            sender.sendMessage(msg.msg("no-permission-others")); return true;
                        }
                        target = getPlayer(args[0]);
                        world = target.getWorld();
                    }
                }
                case 2 -> {                     // /centro <mundo> <jugador>
                    if (!sender.hasPermission("grvrtp.center.others")) {
                        sender.sendMessage(msg.msg("no-permission-others")); return true;
                    }
                    world = getWorld(args[0]);
                    target = getPlayer(args[1]);
                }
                default -> { sender.sendMessage("&cUso: /centro [mundo] [jugador]"); return true; }
            }

            // --- restricción de mundo (black/white list) ---
            if (!isWorldAllowed(world) && !sender.hasPermission("grvrtp.bypass.world")) {
                sender.sendMessage(msg.format("world-restricted",
                        new String[][]{{"%world%", world.getName()}}));
                return true;
            }

            // --- teletransporte ---
            int[] c = centerService.getCenter(world);
            int cx = c[0];
            int cz = c[1];
            int y = world.getHighestBlockYAt(cx, cz) + 1;
            Location dest = new Location(world, cx + 0.5, y, cz + 0.5);
            target.teleport(dest);

            target.sendMessage(msg.format("center-success",
                    new String[][]{{"%world%", world.getName()}}));

            if (!sender.equals(target)) {
                sender.sendMessage(msg.format("center-other",
                        new String[][]{{"%player%", target.getName()}, {"%world%", world.getName()}}));
            }
            return true;

        } catch (IllegalArgumentException ex) {
            sender.sendMessage("&c" + ex.getMessage());
            return true;
        }
    }

    // ---------- helpers ----------
    private Player getPlayer(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) throw new IllegalArgumentException("Jugador no encontrado");
        return p;
    }

    private World getWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null) throw new IllegalArgumentException("Mundo no encontrado");
        return w;
    }

    private boolean isWorldAllowed(World w) {
        var cfg = plugin.getConfig();
        String mode = cfg.getString("worlds.mode", "blacklist").toLowerCase();
        List<String> list = cfg.getStringList("worlds.list");
        boolean contains = list.stream().anyMatch(s -> s.equalsIgnoreCase(w.getName()));
        return mode.equals("whitelist") ? contains : !contains;
    }
}
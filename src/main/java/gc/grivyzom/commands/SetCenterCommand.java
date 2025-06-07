package gc.grivyzom.commands;

import gc.grivyzom.center.CenterService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCenterCommand implements CommandExecutor {

    private final CenterService centerService;
    private final MessageUtil msgUtil;

    public SetCenterCommand(CenterService centerService, MessageUtil msgUtil) {
        this.centerService = centerService;
        this.msgUtil = msgUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Verificar que sea un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage(msgUtil.msg("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Verificar permiso
        if (!player.hasPermission("grvrtp.setcenter")) {
            player.sendMessage(msgUtil.msg("no-permission"));
            return true;
        }

        // Obtener ubicación actual del jugador
        Location loc = player.getLocation();

        // Establecer el centro para el mundo actual
        centerService.setCenter(loc.getWorld(), loc);

        // Mensaje de confirmación
        String locStr = loc.getBlockX() + ", " + loc.getBlockZ();
        player.sendMessage(msgUtil.format("setcenter-success",
                new String[][]{{"%world%", loc.getWorld().getName()}, {"%loc%", locStr}}));

        return true;
    }
}
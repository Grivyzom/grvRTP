package gc.grivyzom.commands;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.rtp.PlayerRTPDataManager;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.UUID;

public class FreeRTPCommand implements CommandExecutor {
    private final GrvRTP plugin;
    private final PlayerRTPDataManager dataManager;
    private final MessageUtil msgUtil;


    public FreeRTPCommand(GrvRTP plugin, PlayerRTPDataManager dataManager, MessageUtil msgUtil) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.msgUtil = msgUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msgUtil.msg("player-only"));
            return true;
        }

        // Verificar si free-rtp está habilitado en la configuración
        if (!plugin.isFreeRTPEnabled()) {
            sender.sendMessage("§cEl sistema de RTP gratis está deshabilitado por configuración.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                dataManager.setFreeRtpEnabled(uuid, true);
                player.sendMessage(msgUtil.msg("free-rtp-enabled"));
                break;

            case "disable":
                dataManager.setFreeRtpEnabled(uuid, false);
                player.sendMessage(msgUtil.msg("free-rtp-disabled"));
                break;

            case "setworld":
                if (args.length < 2) {
                    player.sendMessage(msgUtil.msg("free-rtp-setworld-usage"));
                    return true;
                }

                if (!player.hasPermission("grvrtp.admin")) {
                    player.sendMessage(msgUtil.msg("no-permission"));
                    return true;
                }

                World world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    player.sendMessage(msgUtil.format("invalid-arg", new String[][]{{"%arg%", "mundo"}}));
                    return true;
                }

                dataManager.setFreeRtpWorld(uuid, world.getName());
                player.sendMessage(msgUtil.format("free-rtp-world-set",
                        new String[][]{{"%world%", world.getName()}}));
                break;

            case "info":
                boolean enabled = dataManager.isFreeRtpEnabled(uuid);
                boolean used = dataManager.hasUsedFreeRtp(uuid);
                String worldName = dataManager.getFreeRtpWorld(uuid);

                player.sendMessage(msgUtil.format("free-rtp-info",
                        new String[][]{
                                {"%status%", enabled ? "§aActivado" : "§cDesactivado"},
                                {"%used%", used ? "§cYa usado" : "§aDisponible"},
                                {"%world%", worldName}
                        }));
                break;

            default:
                showUsage(player);
                break;
        }

        return true;
    }

    private void showUsage(Player player) {
        player.sendMessage("§eUso del comando /rtpgratis:");
        player.sendMessage("§e/rtpgratis enable - Activa tu RTP gratis");
        player.sendMessage("§e/rtpgratis disable - Desactiva tu RTP gratis");
        player.sendMessage("§e/rtpgratis setworld <mundo> - Establece el mundo permitido");
        player.sendMessage("§e/rtpgratis info - Muestra tu estado de RTP gratis");
    }
}
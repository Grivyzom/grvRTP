package gc.grivyzom;

import gc.grivyzom.center.CenterService;
import gc.grivyzom.commands.*;
import gc.grivyzom.economy.EconomyService;
import gc.grivyzom.rtp.PlayerRTPDataManager;
import gc.grivyzom.teleport.RandomTeleportService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GrvRTP extends JavaPlugin {
    private int minRange, maxRange, centerX, centerZ;
    private EconomyService economyService;
    public PlayerRTPDataManager playerRTPDataManager;
    private boolean isFreeRTPEnabled; // Nueva variable
    private RandomTeleportService teleportService; // Servicio de teletransporte

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Cargar configuración de free-rtp
        isFreeRTPEnabled = getConfig().getBoolean("free-rtp.enabled", true);

        CenterService centerService = new CenterService(this);
        MessageUtil msgUtil = new MessageUtil(getConfig());
        HelpCommand helpCommand = new HelpCommand(msgUtil);
        playerRTPDataManager = new PlayerRTPDataManager(this.getDataFolder());
        economyService = new EconomyService(this, msgUtil);

        // Inicializar el servicio de teletransporte con la configuración
        teleportService = new RandomTeleportService(getConfig());

        // Registro de comandos
        getCommand("rtp").setExecutor(new RTPCommand(this, economyService));
        getCommand("centro").setExecutor(new CenterCommand(this, centerService, economyService));
        getCommand("setcenter").setExecutor(new SetCenterCommand(centerService, msgUtil));
        getCommand("grvrtp").setExecutor(new GrvRTPCommand(helpCommand));
        getCommand("rtpgratis").setExecutor(new FreeRTPCommand(this, playerRTPDataManager, msgUtil)); // Pasar el plugin como parámetro

        getLogger().info("GrvRTP habilitado.");
        getLogger().info("Free RTP: " + (isFreeRTPEnabled ? "Activado" : "Desactivado"));
        getLogger().info("Sistema de seguridad RTP: " + (getConfig().getBoolean("teleport.safety.enabled", true) ? "Activado" : "Desactivado"));
    }

    // Getter para el estado de free-rtp
    public boolean isFreeRTPEnabled() {
        return isFreeRTPEnabled;
    }

    // Getter para el servicio de teletransporte
    public RandomTeleportService getTeleportService() {
        return teleportService;
    }

    @Override
    public void onDisable() {
        getLogger().info("GrvRTP deshabilitado.");
    }

    private class GrvRTPCommand implements CommandExecutor {
        private final HelpCommand helpCommand;

        public GrvRTPCommand(HelpCommand helpCommand) {
            this.helpCommand = helpCommand;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            // Si no hay argumentos o el primer argumento es "help", mostrar ayuda
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                int page = 1;

                // Si hay un segundo argumento y es un número, usarlo como página
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cNúmero de página inválido. Uso: /grvrtp help [página]");
                        return true;
                    }
                }

                helpCommand.showHelp(sender, page);
                return true;
            }

            // Comando safety
            if (args[0].equalsIgnoreCase("safety")) {
                SafetyCommand safetyCommand = new SafetyCommand(GrvRTP.this, new MessageUtil(getConfig()));
                String[] safetyArgs = new String[args.length - 1];
                System.arraycopy(args, 1, safetyArgs, 0, safetyArgs.length);
                return safetyCommand.onCommand(sender, cmd, label, safetyArgs);
            }

            // Comando reload (mantener funcionalidad original)
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("grvrtp.admin")) {
                    sender.sendMessage("§cNo tienes permiso para recargar la configuración.");
                    return true;
                }

                reloadConfig();
                // Recargar también el sistema de economía
                economyService.reload();

                // Recargar el servicio de teletransporte con la nueva configuración
                teleportService = new RandomTeleportService(getConfig());

                // Actualizar configuración de free-rtp
                isFreeRTPEnabled = getConfig().getBoolean("free-rtp.enabled", true);

                sender.sendMessage("§aConfiguración de GrvRTP recargada exitosamente.");

                // Informar sobre el estado de la economía
                if (economyService.isEnabled()) {
                    sender.sendMessage("§aSistema de economía: §aActivado");
                } else {
                    sender.sendMessage("§aSistema de economía: §cDesactivado");
                }

                // Informar sobre el estado del sistema de seguridad
                boolean safetyEnabled = getConfig().getBoolean("teleport.safety.enabled", true);
                sender.sendMessage("§aSistema de seguridad RTP: " + (safetyEnabled ? "§aActivado" : "§cDesactivado"));

                // Informar sobre el estado de Free RTP
                sender.sendMessage("§aFree RTP: " + (isFreeRTPEnabled ? "§aActivado" : "§cDesactivado"));

                return true;
            }

            // Si el argumento no es reconocido, mostrar ayuda
            sender.sendMessage("§cComando desconocido. Usa §e/grvrtp help §cpara ver los comandos disponibles.");
            return true;
        }
    }

    // Getter para el servicio de economía (por si otros componentes lo necesitan)
    public EconomyService getEconomyService() {
        return economyService;
    }
}
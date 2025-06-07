package gc.grivyzom;

import gc.grivyzom.center.CenterService;
import gc.grivyzom.commands.HelpCommand;
import gc.grivyzom.commands.SetCenterCommand;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import gc.grivyzom.commands.RTPCommand;
import gc.grivyzom.commands.CenterCommand;

public class GrvRTP extends JavaPlugin {
    private int minRange, maxRange, centerX, centerZ;
    private RTPCommand rtpCommand;
    private CenterCommand centerCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CenterService centerService = new CenterService(this);
        MessageUtil   msgUtil       = new MessageUtil(getConfig());
        HelpCommand   helpCommand   = new HelpCommand(msgUtil);

        // Crear instancias de comandos
        rtpCommand = new RTPCommand(this);
        centerCommand = new CenterCommand(this, centerService);

        //  Registro de comandos
        getCommand("rtp").setExecutor(rtpCommand);
        getCommand("centro").setExecutor(centerCommand);
        getCommand("setcenter").setExecutor(new SetCenterCommand(centerService, msgUtil));
        getCommand("grvrtp").setExecutor(new GrvRTPCommand(helpCommand));

        getLogger().info("GrvRTP habilitado con sistema de seguridad mejorado.");
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

            // Comando reload (mantener funcionalidad original)
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("grvrtp.admin")) {
                    sender.sendMessage("§cNo tienes permiso para recargar la configuración.");
                    return true;
                }

                reloadConfig();

                // Recargar configuración en los comandos
                if (rtpCommand != null) {
                    rtpCommand.reloadConfig();
                }

                sender.sendMessage("§aConfiguración de GrvRTP recargada exitosamente.");
                sender.sendMessage("§7- Lista de bloques prohibidos actualizada");
                sender.sendMessage("§7- Configuración de seguridad actualizada");
                return true;
            }

            // Comando para verificar configuración de seguridad
            if (args[0].equalsIgnoreCase("safety") || args[0].equalsIgnoreCase("seguridad")) {
                if (!sender.hasPermission("grvrtp.admin")) {
                    sender.sendMessage("§cNo tienes permiso para ver la configuración de seguridad.");
                    return true;
                }

                showSafetyInfo(sender);
                return true;
            }

            // Si el argumento no es reconocido, mostrar ayuda
            sender.sendMessage("§cComando desconocido. Usa §e/grvrtp help §cpara ver los comandos disponibles.");
            return true;
        }

        private void showSafetyInfo(CommandSender sender) {
            FileConfiguration config = getConfig();

            sender.sendMessage("§6§l▬▬▬▬▬▬ §e§lConfiguración de Seguridad §6§l▬▬▬▬▬▬");

            // Mostrar configuración de intentos
            int maxAttempts = config.getInt("teleport.max-attempts", 50);
            sender.sendMessage("§7Máximo de intentos: §e" + maxAttempts);

            // Mostrar límites de altura
            int minY = config.getInt("teleport.min-y", 64);
            int maxY = config.getInt("teleport.max-y", 256);
            sender.sendMessage("§7Altura mínima: §e" + minY + " §7| Altura máxima: §e" + maxY);

            // Mostrar configuración de seguridad
            boolean checkAir = config.getBoolean("teleport.safety.check-air-above", true);
            boolean checkSurrounding = config.getBoolean("teleport.safety.check-surrounding", true);
            boolean avoidCaves = config.getBoolean("teleport.safety.avoid-caves", true);
            int minLight = config.getInt("teleport.safety.min-light-level", -1);

            sender.sendMessage("§7Verificar aire arriba: " + (checkAir ? "§aActivado" : "§cDesactivado"));
            sender.sendMessage("§7Verificar alrededores: " + (checkSurrounding ? "§aActivado" : "§cDesactivado"));
            sender.sendMessage("§7Evitar cuevas: " + (avoidCaves ? "§aActivado" : "§cDesactivado"));
            sender.sendMessage("§7Luz mínima: " + (minLight >= 0 ? "§e" + minLight : "§cDesactivado"));

            // Mostrar bloques prohibidos
            var bannedBlocks = config.getStringList("banned-blocks");
            sender.sendMessage("§7Bloques prohibidos: §e" + bannedBlocks.size() + " §7tipos");

            // Mostrar bloques seguros
            var safeBlocks = config.getStringList("safe-blocks");
            if (!safeBlocks.isEmpty()) {
                sender.sendMessage("§7Bloques seguros: §e" + safeBlocks.size() + " §7tipos definidos");
            } else {
                sender.sendMessage("§7Bloques seguros: §7Todos los sólidos excepto prohibidos");
            }

            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }
    }
}
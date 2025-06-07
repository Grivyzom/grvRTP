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
    private RTPCommand rtpCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CenterService centerService = new CenterService(this);
        MessageUtil   msgUtil       = new MessageUtil(getConfig());
        HelpCommand   helpCommand   = new HelpCommand(msgUtil);

        //  Registro de comandos
        this.rtpCommand = new RTPCommand(this, centerService);
        getCommand("rtp").setExecutor(rtpCommand);
        getCommand("centro").setExecutor(new CenterCommand(this, centerService));
        getCommand("setcenter").setExecutor(new SetCenterCommand(centerService, msgUtil));
        getCommand("grvrtp").setExecutor(new GrvRTPCommand(helpCommand));

        getLogger().info("GrvRTP habilitado.");
        getLogger().info("Sistema de bloques prohibidos configurado con " +
                getConfig().getStringList("teleport.banned-blocks").size() + " materiales.");
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

                // Recargar también la configuración del servicio de teleporte
                if (rtpCommand != null) {
                    rtpCommand.reloadTeleportService();
                }

                sender.sendMessage("§aConfiguración de GrvRTP recargada exitosamente.");
                sender.sendMessage("§7Bloques prohibidos actualizados: " +
                        getConfig().getStringList("teleport.banned-blocks").size() + " materiales.");
                return true;
            }

            // Comando para listar bloques prohibidos (nuevo)
            if (args[0].equalsIgnoreCase("blocks")) {
                if (!sender.hasPermission("grvrtp.admin")) {
                    sender.sendMessage("§cNo tienes permiso para ver esta información.");
                    return true;
                }

                var bannedBlocks = getConfig().getStringList("teleport.banned-blocks");
                sender.sendMessage("§6§l▬▬▬ Bloques Prohibidos (" + bannedBlocks.size() + ") ▬▬▬");

                if (bannedBlocks.isEmpty()) {
                    sender.sendMessage("§7No hay bloques prohibidos configurados.");
                } else {
                    StringBuilder blocks = new StringBuilder("§e");
                    for (int i = 0; i < bannedBlocks.size(); i++) {
                        blocks.append(bannedBlocks.get(i));
                        if (i < bannedBlocks.size() - 1) {
                            blocks.append("§7, §e");
                        }
                        // Crear nueva línea cada 6 bloques para mejor legibilidad
                        if ((i + 1) % 6 == 0 && i < bannedBlocks.size() - 1) {
                            sender.sendMessage(blocks.toString());
                            blocks = new StringBuilder("§e");
                        }
                    }
                    if (blocks.length() > 2) {
                        sender.sendMessage(blocks.toString());
                    }
                }
                sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                return true;
            }

            // Si el argumento no es reconocido, mostrar ayuda
            sender.sendMessage("§cComando desconocido. Usa §e/grvrtp help §cpara ver los comandos disponibles.");
            return true;
        }
    }
}
package gc.grivyzom.commands;

import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand {

    private final MessageUtil msg;
    private final List<HelpEntry> helpEntries;
    private static final int COMMANDS_PER_PAGE = 6;

    public HelpCommand(MessageUtil msg) {
        this.msg = msg;
        this.helpEntries = new ArrayList<>();
        initializeHelpEntries();
    }

    private void initializeHelpEntries() {
        helpEntries.add(new HelpEntry(
                "/rtp",
                "Teletransporte aleatorio a una ubicación dentro del rango especificado",
                "grvrtp.use"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <rango>",
                "Teletransporte aleatorio con un rango personalizado de distancia",
                "grvrtp.use"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <jugador>",
                "Teletransporta a otro jugador aleatoriamente",
                "grvrtp.use.others"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <jugador> <rango>",
                "Teletransporta a otro jugador con rango personalizado",
                "grvrtp.use.others"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <rango> <mundo>",
                "Teletransporte aleatorio en un mundo específico",
                "grvrtp.use"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <jugador> <mundo>",
                "Teletransporta a otro jugador en un mundo específico",
                "grvrtp.use.others"
        ));

        helpEntries.add(new HelpEntry(
                "/rtp <jugador> <rango> <mundo>",
                "Teletransporte completo: jugador, rango y mundo específico",
                "grvrtp.use.others"
        ));

        helpEntries.add(new HelpEntry(
                "/centro",
                "Teletransporta al centro del mundo actual",
                "grvrtp.center"
        ));

        helpEntries.add(new HelpEntry(
                "/centro <mundo>",
                "Teletransporta al centro de un mundo específico",
                "grvrtp.center"
        ));

        helpEntries.add(new HelpEntry(
                "/centro <jugador>",
                "Teletransporta a otro jugador al centro de su mundo actual",
                "grvrtp.center.others"
        ));

        helpEntries.add(new HelpEntry(
                "/centro <mundo> <jugador>",
                "Teletransporta a otro jugador al centro de un mundo específico",
                "grvrtp.center.others"
        ));

        helpEntries.add(new HelpEntry(
                "/setcenter",
                "Establece el centro del mundo actual en tu ubicación",
                "grvrtp.setcenter"
        ));

        helpEntries.add(new HelpEntry(
                "/grvrtp reload",
                "Recarga la configuración del plugin",
                "grvrtp.admin"
        ));

        helpEntries.add(new HelpEntry(
                "/grvrtp help [página]",
                "Muestra esta ayuda con todos los comandos disponibles",
                "grvrtp.use"
        ));
    }

    public void showHelp(CommandSender sender, int page) {
        // Filtrar comandos según permisos del usuario
        List<HelpEntry> availableCommands = helpEntries.stream()
                .filter(entry -> sender.hasPermission(entry.permission))
                .toList();

        if (availableCommands.isEmpty()) {
            sender.sendMessage("§cNo tienes permiso para usar ningún comando de GrvRTP.");
            return;
        }

        int totalPages = (int) Math.ceil((double) availableCommands.size() / COMMANDS_PER_PAGE);

        // Validar página
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Calcular índices
        int startIndex = (page - 1) * COMMANDS_PER_PAGE;
        int endIndex = Math.min(startIndex + COMMANDS_PER_PAGE, availableCommands.size());

        // Enviar encabezado
        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬ §e§lGrvRTP - Ayuda §6§l▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§7Página §e" + page + "§7 de §e" + totalPages + " §7(§e" + availableCommands.size() + " §7comandos disponibles)");
        sender.sendMessage("");

        // Mostrar comandos de la página actual
        for (int i = startIndex; i < endIndex; i++) {
            HelpEntry entry = availableCommands.get(i);
            sender.sendMessage("§a" + entry.command);
            sender.sendMessage("  §7" + entry.description);
            if (i < endIndex - 1) {
                sender.sendMessage("");
            }
        }

        sender.sendMessage("");

        // Mostrar navegación si hay más de una página
        if (totalPages > 1) {
            StringBuilder navigation = new StringBuilder("§6");

            if (page > 1) {
                navigation.append("◀ Página anterior: §e/grvrtp help ").append(page - 1).append(" §6| ");
            }

            navigation.append("Página ").append(page).append("/").append(totalPages);

            if (page < totalPages) {
                navigation.append(" §6| Siguiente página: §e/grvrtp help ").append(page + 1).append(" §6▶");
            }

            sender.sendMessage(navigation.toString());
        }

        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private static class HelpEntry {
        final String command;
        final String description;
        final String permission;

        HelpEntry(String command, String description, String permission) {
            this.command = command;
            this.description = description;
            this.permission = permission;
        }
    }
}
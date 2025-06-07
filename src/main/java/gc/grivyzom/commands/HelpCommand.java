package gc.grivyzom.commands;

import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    private final MessageUtil msgUtil;
    private final String[][] helpPages = {
            {
                    "§6========== §eGrvRTP - Ayuda (Página 1/3) §6==========",
                    "§e/rtp §7- Teletransporte aleatorio básico",
                    "§e/rtp <rango> §7- RTP con rango específico",
                    "§e/rtp <jugador> §7- Teletransportar a otro jugador",
                    "§e/rtp <rango> <mundo> §7- RTP en mundo específico",
                    "§e/rtp <jugador> <rango> <mundo> §7- Comando completo",
                    "§7Usa §e/grvrtp help 2 §7para ver más comandos."
            },
            {
                    "§6========== §eGrvRTP - Ayuda (Página 2/3) §6==========",
                    "§e/centro §7- Ir al centro del mundo actual",
                    "§e/centro <mundo> §7- Ir al centro de un mundo específico",
                    "§e/centro <mundo> <jugador> §7- Enviar jugador al centro",
                    "§e/setcenter §7- Establecer centro del mundo actual",
                    "§7Usa §e/grvrtp help 3 §7para ver información de economía."
            },
            {
                    "§6========== §eGrvRTP - Ayuda (Página 3/3) §6==========",
                    "§e§lSistema de Economía:",
                    "§7• El coste depende de la distancia del teletransporte",
                    "§7• Mayor distancia = mayor coste",
                    "§7• Los administradores pueden configurar costes mínimos/máximos",
                    "§7• Se puede activar/desactivar en la configuración",
                    "§e/grvrtp reload §7- Recargar configuración (admin)",
                    "§7Usa §e/grvrtp help 1 §7para volver al inicio."
            }
    };

    public HelpCommand(MessageUtil msgUtil) {
        this.msgUtil = msgUtil;
    }

    public void showHelp(CommandSender sender, int page) {
        // Validar página
        if (page < 1 || page > helpPages.length) {
            sender.sendMessage("§cPágina inválida. Páginas disponibles: 1-" + helpPages.length);
            return;
        }

        // Mostrar la página solicitada
        String[] pageContent = helpPages[page - 1];
        for (String line : pageContent) {
            sender.sendMessage(line);
        }
    }
}
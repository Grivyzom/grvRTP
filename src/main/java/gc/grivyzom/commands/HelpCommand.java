package gc.grivyzom.commands;

import gc.grivyzom.util.MessageUtil;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    private final MessageUtil msgUtil;
    private final String[][] helpPages = {
            {
                    "§6========== §eGrvRTP - Ayuda (Página 1/4) §6==========",
                    "§e/rtp §7- Teletransporte aleatorio básico",
                    "§e/rtp <rango> §7- RTP con rango específico",
                    "§e/rtp <jugador> §7- Teletransportar a otro jugador",
                    "§e/rtp <rango> <mundo> §7- RTP en mundo específico",
                    "§e/rtp <jugador> <rango> <mundo> §7- Comando completo",
                    "§7Usa §e/grvrtp help 2 §7para ver más comandos."
            },
            {
                    "§6========== §eGrvRTP - Ayuda (Página 2/4) §6==========",
                    "§e/centro §7- Ir al centro del mundo actual",
                    "§e/centro <mundo> §7- Ir al centro de un mundo específico",
                    "§e/centro <mundo> <jugador> §7- Enviar jugador al centro",
                    "§e/setcenter §7- Establecer centro del mundo actual",
                    "§e/rtpgratis §7- Gestionar tu RTP gratis",
                    "§7Usa §e/grvrtp help 3 §7para ver información de economía."
            },
            {
                    "§6========== §eGrvRTP - Ayuda (Página 3/4) §6==========",
                    "§e§lSistema de Economía:",
                    "§7• El coste depende de la distancia del teletransporte",
                    "§7• Mayor distancia = mayor coste",
                    "§7• Los administradores pueden configurar costes mínimos/máximos",
                    "§7• Se puede activar/desactivar en la configuración",
                    "§7• Soporta dinero (Vault) y experiencia",
                    "§7Usa §e/grvrtp help 4 §7para ver comandos de administración."
            },
            {
                    "§6========== §eGrvRTP - Ayuda (Página 4/4) §6==========",
                    "§e§lComandos de Administración:",
                    "§e/grvrtp reload §7- Recargar configuración",
                    "§e/grvrtp safety check §7- Verificar ubicación actual",
                    "§e/grvrtp safety test <x> <y> <z> §7- Probar ubicación",
                    "§e/grvrtp safety info §7- Info del sistema de seguridad",
                    "§e§lSistema de Seguridad:",
                    "§7• Evita spawns en bloques peligrosos (lava, cactus, etc.)",
                    "§7• Configurable en config.yml bajo 'banned-blocks'",
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
package gc.grivyzom.commands;

import gc.grivyzom.center.CenterService;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SetCenterCommand implements CommandExecutor {

    private final CenterService centers;
    private final MessageUtil    msg;

    public SetCenterCommand(CenterService centers, MessageUtil msg){
        this.centers = centers;
        this.msg     = msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {

        if(!(sender instanceof Player p)){
            sender.sendMessage(msg.msg("player-only"));
            return true;
        }
        if(!p.hasPermission("grvrtp.setcenter")){
            p.sendMessage(msg.msg("no-permission"));
            return true;
        }

        World    w   = p.getWorld();
        Location loc = p.getLocation();

        centers.setCenter(w, loc);

        String locStr = loc.getBlockX() + ", " + loc.getBlockZ();
        p.sendMessage(msg.format("setcenter-success",
                new String[][]{{"%loc%", locStr}, {"%world%", w.getName()}}));
        return true;
    }
}

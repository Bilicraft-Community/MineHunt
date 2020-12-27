package net.mcxk.minehunt.listener;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.GameStatus;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerServerListener implements Listener {
    private MineHunt plugin;
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void join(PlayerJoinEvent event){
        if(plugin.getGame().getStatus() == GameStatus.WAITING_PLAYERS){
            if(plugin.getGame().playerJoining(event.getPlayer())){
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
            }else{
                event.getPlayer().setGameMode(GameMode.SPECTATOR);
                event.getPlayer().sendMessage("当前游戏已满人，您现在处于观战状态");
            }
        }else{
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            event.getPlayer().sendMessage("游戏已经开始，您现在处于观战状态");
        }
    }
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void join(PlayerQuitEvent event){
        if(!plugin.getGame().getInGamePlayers().contains(event.getPlayer())){
            return;
        }
        plugin.getGame().playerLeaving(event.getPlayer());
    }

}

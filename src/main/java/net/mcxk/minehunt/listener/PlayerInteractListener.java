package net.mcxk.minehunt.listener;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.GameStatus;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {
    private final MineHunt plugin = MineHunt.getInstance();
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void clickXJB(PlayerInteractEvent event){
        if(plugin.getGame().getStatus() != GameStatus.GAME_STARTED){
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void clickXJB(EntityDamageEvent event){
        if(plugin.getGame().getStatus() != GameStatus.GAME_STARTED){
            event.setCancelled(true);
        }
    }

}

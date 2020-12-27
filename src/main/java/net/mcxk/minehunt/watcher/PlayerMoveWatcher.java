package net.mcxk.minehunt.watcher;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.GameProgress;
import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerMoveWatcher {
    public PlayerMoveWatcher(){
        new BukkitRunnable(){
            @Override
            public void run() {
                MineHunt.getInstance().getGame().getInGamePlayers().forEach(player -> {
                    if(player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        Location location = player.getWorld().locateNearestStructure(player.getLocation(), StructureType.STRONGHOLD, 1, false);
                        if (location != null) {
                            MineHunt.getInstance().getGame().getProgressManager().unlockProgress(GameProgress.FIND_STRONG_HOLD);
                            MineHunt.getInstance().getGame().getGameEndingDataBuilder().strongHoldFinder(player.getName());
                            return;
                        }
                    }
                    if(player.getWorld().getEnvironment() == World.Environment.NETHER) {
                        Location location = player.getWorld().locateNearestStructure(player.getLocation(), StructureType.NETHER_FORTRESS, 1, false);
                        if (location != null) {
                            MineHunt.getInstance().getGame().getProgressManager().unlockProgress(GameProgress.FIND_NETHER_FORTRESS);
                            MineHunt.getInstance().getGame().getGameEndingDataBuilder().netherFortressFinder(player.getName());
                        }
                    }
                });
            }
        }.runTaskTimer(MineHunt.getInstance(),0,80);
    }
}

package net.mcxk.minehunt.watcher;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.GameProgress;
import net.mcxk.minehunt.game.PlayerRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class PlayerMoveWatcher {
    boolean runnerNether = false;
    boolean runnerTheEnd = false;
    public PlayerMoveWatcher(){
        new BukkitRunnable(){
            @Override
            public void run() {
                MineHunt.getInstance().getGame().getInGamePlayers().forEach(player -> {
                    World.Environment environment = player.getWorld().getEnvironment();
                    if(environment == World.Environment.NORMAL) {
                        Location location = player.getWorld().locateNearestStructure(player.getLocation(), StructureType.STRONGHOLD, 1, false);
                        if (location != null) {
                            MineHunt.getInstance().getGame().getProgressManager().unlockProgress(GameProgress.FIND_STRONG_HOLD);
                            MineHunt.getInstance().getGame().getGameEndingDataBuilder().strongHoldFinder(player.getName());
                        }
                    }
                    if(environment == World.Environment.NETHER) {
                        Location location = player.getWorld().locateNearestStructure(player.getLocation(), StructureType.NETHER_FORTRESS, 1, false);
                        if (location != null) {
                            MineHunt.getInstance().getGame().getProgressManager().unlockProgress(GameProgress.FIND_NETHER_FORTRESS);
                            MineHunt.getInstance().getGame().getGameEndingDataBuilder().netherFortressFinder(player.getName());
                        }
                    }
                    if(environment != World.Environment.NORMAL){
                        Optional<PlayerRole> role = MineHunt.getInstance().getGame().getPlayerRole(player);
                        if(role.isPresent()){
                            if(role.get() == PlayerRole.RUNNER){
                                if(!runnerNether && environment == World.Environment.NETHER){
                                    runnerNether = true;
                                    Bukkit.broadcastMessage("逃亡者已到达 下界 维度！");
                                }
                                if(!runnerTheEnd && environment == World.Environment.THE_END){
                                    runnerNether = true;
                                    Bukkit.broadcastMessage("逃亡者已到达 末地 维度！");
                                }
                            }
                        }
                    }

                });
            }
        }.runTaskTimer(MineHunt.getInstance(),0,80);
    }
}

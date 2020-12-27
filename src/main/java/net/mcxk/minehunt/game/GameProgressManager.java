package net.mcxk.minehunt.game;

import net.mcxk.minehunt.MineHunt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 处理新的进度事件
 */
public class GameProgressManager {
    private final MineHunt plugin = MineHunt.getInstance();
    private final Set<GameProgress> unlocked = new HashSet<>();
    public void unlockProgress(GameProgress progress){
        if(plugin.getGame().getStatus() != GameStatus.GAME_STARTED){
            return;
        }
        if(!unlocked.add(progress)){
            return;
        }
        processProgress(progress);
    }
    private void processProgress(GameProgress progress){
        switch (progress){
            case NOT_STARTED:
            case GAME_STARTING:
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.BREAD,5)));
                break;
            case STONE_AGE:
            case IRON_MINED:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包，药水效果已向所有人应用！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.IRON_ORE,8)));
                plugin.getGame().getInGamePlayers().forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,600,1)));
                break;
            case COMPASS_UNLOCKED:
            case ENTER_NETHER:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.OBSIDIAN,4)));
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.FLINT,1)));
                break;
            case GET_BLAZE_ROD:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励药水效果已向所有人应用！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,300,1)));
                break;
            case GET_ENDER_PERAL:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL,1)));
                break;
            case FIND_NETHER_FORTRESS:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包，药水效果已向所有人应用！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,400,1)));
                break;
            case FIND_STRONG_HOLD:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包，药水效果已向所有人应用！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.ENDER_EYE,1)));
                plugin.getGame().getInGamePlayers().forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,600,1)));
                break;
            case ENTER_END:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，奖励已发送至所有玩家背包！");
                plugin.getGame().getInGamePlayers().forEach(player -> player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET,1)));
                break;
            case KILLED_DRAGON:
                Bukkit.broadcastMessage("新的游戏阶段已解锁 ["+progress.name()+"]，末影龙已被击杀！");
                break;
        }
    }
}

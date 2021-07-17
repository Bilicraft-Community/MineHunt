package net.mcxk.minehunt.listener;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.GameStatus;
import net.mcxk.minehunt.game.PlayerRole;
import org.bukkit.*;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

public class GameWinnerListener implements Listener {
    private final MineHunt plugin = MineHunt.getInstance();
    //private String dragonKiller = "Magic";

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void playerDeath(PlayerDeathEvent event) {
        if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
            return;
        }
        Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getEntity());
        if (role.isPresent()) {
            if (role.get() == PlayerRole.RUNNER) {
                String finalKiller = getSource(event);
                plugin.getGame().getGameEndingData().setRunnerKiller(finalKiller);
                event.getEntity().setGameMode(GameMode.SPECTATOR);
                //Runner强制复活在其死亡位置，避免死亡复活的跨世界传送
                event.getEntity().setBedSpawnLocation(event.getEntity().getLocation(),true);
                if (plugin.getGame().getPlayersAsRole(PlayerRole.RUNNER).stream().allMatch(p -> p.getGameMode() == GameMode.SPECTATOR)) {
                    plugin.getGame().stop(PlayerRole.HUNTER, event.getEntity().getLocation().add(0, 3, 0));
                    event.getEntity().setHealth(20); //Prevent player dead
                }
            }
        }
    }

    /**
     * 追踪源
     * @param event
     * @author https://github.com/PlayPro/CoreProtect
     * @return
     */
    public String getSource(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String e = "未知";
        EntityDamageEvent damage = entity.getLastDamageCause();
            if (damage != null) {
                boolean skip = true;

                if (damage instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent attack = (EntityDamageByEntityEvent) damage;
                    Entity attacker = attack.getDamager();

                    if (attacker instanceof Player) {
                        Player player = (Player) attacker;
                        e = player.getName();
                    }
                    else if (attacker instanceof AbstractArrow) {
                        AbstractArrow arrow = (AbstractArrow) attacker;
                        ProjectileSource shooter = arrow.getShooter();

                        if (shooter instanceof Player) {
                            Player player = (Player) shooter;
                            e = player.getName();
                        }
                        else if (shooter instanceof LivingEntity) {
                            EntityType entityType = ((LivingEntity) shooter).getType();
                            // Check for MyPet plugin
                            e = entityType.name().toLowerCase(Locale.ROOT);
                        }
                    }
                    else if (attacker instanceof ThrownPotion) {
                        ThrownPotion potion = (ThrownPotion) attacker;
                        ProjectileSource shooter = potion.getShooter();

                        if (shooter instanceof Player) {
                            Player player = (Player) shooter;
                            e = player.getName();
                        }
                        else if (shooter instanceof LivingEntity) {
                            EntityType entityType = ((LivingEntity) shooter).getType();
                            e = entityType.name().toLowerCase(Locale.ROOT);
                        }
                    }
                    else {
                        attacker.getType();
                        e = attacker.getType().name().toLowerCase(Locale.ROOT);
                    }
                }
                else {
                    EntityDamageEvent.DamageCause cause = damage.getCause();
                    if (cause.equals(EntityDamageEvent.DamageCause.FIRE)) {
                        e = "火焰";
                    }
                    else if (cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)) {
                        if (!skip) {
                            e = "火焰";
                        }
                    }
                    else if (cause.equals(EntityDamageEvent.DamageCause.LAVA)) {
                        e = "岩浆";
                    }
                    else if (cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
                        e = "爆炸";
                    }
                    else if (cause.equals(EntityDamageEvent.DamageCause.MAGIC)) {
                        e = "药水";
                    }
                }

//                if (entity instanceof ArmorStand) {
//                    Location entityLocation = entity.getLocation();
//                    if (!Config.getConfig(entityLocation.getWorld()).ITEM_TRANSACTIONS) {
//                        entityLocation.setY(entityLocation.getY() + 0.99);
//                        Block block = entityLocation.getBlock();
//                        Queue.queueBlockBreak(e, block.getState(), Material.ARMOR_STAND, null, (int) entityLocation.getYaw());
//                    }
//                    return;
//                }

                EntityType entity_type = entity.getType();
                if (e.length() == 0) {
                    // assume killed self
                    if (!skip) {
                        if (!(entity instanceof Player) && entity_type.name() != null) {
                            // Player player = (Player)entity;
                            // e = player.getName();
                            e = entity_type.name().toLowerCase(Locale.ROOT);
                        }
                        else if (entity instanceof Player) {
                            e = entity.getName();
                        }
                    }
                }

                if (e.startsWith("wither")) {
                    e = "凋零";
                }

                if (e.startsWith("enderdragon")) {
                    e = "末影龙";
                }

                if (e.startsWith("primedtnt") || e.startsWith("tnt")) {
                    e = "TNT";
                }

                if (e.startsWith("lightning")) {
                    e = "闪电";
                }
        }
        return e;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
            return;
        }
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            if (event.getDamager() instanceof Player) {
                Optional<PlayerRole> role = MineHunt.getInstance().getGame().getPlayerRole(((Player) event.getDamager()));
                if (role.isPresent()) {
                    if (role.get() == PlayerRole.HUNTER) {
                        event.setCancelled(true);
                        event.getEntity().sendMessage(ChatColor.RED + "猎人是末影龙的好伙伴，你不可以对龙造成伤害！");
                        return;
                    }
                }
            }else if(event.getDamager() instanceof Projectile){
                Projectile projectile = (Projectile)event.getDamager();
                if(projectile.getShooter() instanceof Player){
                    Optional<PlayerRole> role = MineHunt.getInstance().getGame().getPlayerRole(((Player) projectile.getShooter()));
                    if (role.isPresent()) {
                        if (role.get() == PlayerRole.HUNTER) {
                            event.setCancelled(true);
                            event.getEntity().sendMessage(ChatColor.RED + "末影龙依靠末影水晶恢复生命，你不可以破坏末影水晶！");
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void entityDeath(EntityDeathEvent event) {
        if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
            return;
        }
        if (event.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }
        plugin.getGame().getGameEndingData().setDragonKiller(getSource(event));
        plugin.getGame().stop(PlayerRole.RUNNER, new Location(event.getEntity().getLocation().getWorld(), 0, 85, 0));
    }
}

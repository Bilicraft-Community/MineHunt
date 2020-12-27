package net.mcxk.minehunt.game;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.util.GameEndingData;
import net.mcxk.minehunt.util.Util;
import net.mcxk.minehunt.watcher.CountDownWatcher;
import net.mcxk.minehunt.watcher.PlayerMoveWatcher;
import net.mcxk.minehunt.watcher.RadarWatcher;
import net.mcxk.minehunt.watcher.ReconnectWatcher;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Game {
    private final MineHunt plugin = MineHunt.getInstance();
    @Getter
    private final Set<Player> inGamePlayers = Sets.newCopyOnWriteArraySet(); //线程安全
    @Getter
    @Setter
    private GameStatus status = GameStatus.WAITING_PLAYERS;
    private Map<Player, PlayerRole> roleMapping; //线程安全
    @Getter
    private int maxPlayers = plugin.getConfig().getInt("max-players");
    @Getter
    private int minPlayers = plugin.getConfig().getInt("min-players");
    @Getter
    private final int countdown = 30;
    private int runnerMax = plugin.getConfig().getInt("runner-max");
    @Getter
    private final Map<Player, Long> reconnectTimer = new HashMap<>();
    @Getter
    private boolean compassUnlocked = false;
    @Getter
    private final GameProgressManager progressManager = new GameProgressManager();

    @Getter
    private final GameEndingData.GameEndingDataBuilder gameEndingDataBuilder = GameEndingData.builder();

    public Game(){
        fixConfig();

    }

    public void switchCompass(boolean unlocked) {
        if (this.compassUnlocked == unlocked) {
            return;
        }
        this.compassUnlocked = unlocked;
        if (unlocked) {
            getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().addItem(new ItemStack(Material.COMPASS, 1)));
            Bukkit.broadcastMessage(ChatColor.YELLOW + "猎人已解锁追踪指南针！逃亡者的位置已经暴露！");
        } else {
            getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().remove(Material.COMPASS));
            Bukkit.broadcastMessage(ChatColor.YELLOW + "猎人的追踪指南针被破坏失效，需要重新解锁！");
        }
        getPlayersAsRole(PlayerRole.RUNNER).forEach(p -> p.getInventory().remove(Material.COMPASS)); //清除合成的指南针
    }

    /**
     * 获取玩家角色
     *
     * @param player 玩家
     * @return 可能是Empty（玩家不属于游戏中的玩家）否则返回玩家角色
     */
    public Optional<PlayerRole> getPlayerRole(Player player) {
        if (!this.roleMapping.containsKey(player)) {
            return Optional.empty();
        }
        return Optional.of(this.roleMapping.get(player));
    }

    public boolean playerJoining(Player player) {
        if (inGamePlayers.size() < maxPlayers) {
            inGamePlayers.add(player);
            return true;
        }
        return false;
    }
    public void fixConfig(){
//        if(runnerMax >minPlayers){
//            runnerAmount = minPlayers -1;
//        }
//        if(runnerAmount < 1){
//            runnerAmount = 1;
//            minPlayers = runnerAmount + 1;
//        }
//        if(maxPlayers < minPlayers){
//            maxPlayers = minPlayers + 1;
//        }
    }

    public void playerLeaving(Player player) {
        this.reconnectTimer.put(player, System.currentTimeMillis());
        if(status == GameStatus.WAITING_PLAYERS){
            this.inGamePlayers.remove(player);
        }
    }

    public void playerLeft(Player player) {
        this.roleMapping.remove(player);
        this.inGamePlayers.remove(player);

        if (getPlayersAsRole(PlayerRole.RUNNER).isEmpty() || getPlayersAsRole(PlayerRole.HUNTER).isEmpty()) {
            Bukkit.broadcastMessage("由于比赛的一方所有人因为长时间未能重新连接而被从列表中剔除，游戏被迫终止。");
            Bukkit.broadcastMessage("服务器将会在 10 秒钟后重新启动。");
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.shutdown();
                }
            }.runTaskLater(plugin, 200);
            return;
        }
        Bukkit.broadcastMessage("玩家：" + player.getName() + " 因长时间未能重新连接回对战而被从列表中剔除");
        Bukkit.broadcastMessage(ChatColor.RED + "猎人: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
        Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
    }

    public void start() {
        if (status != GameStatus.WAITING_PLAYERS) {
            return;
        }
        Bukkit.broadcastMessage("请稍后，系统正在随机分配玩家身份...");
        Random random = new Random();
        List<Player> noRolesPlayers = new ArrayList<>(inGamePlayers);
        Map<Player, PlayerRole> roleMapTemp = new HashMap<>();

        int runners = 1;
        if(inGamePlayers.size() == maxPlayers){
            runners = runnerMax;
        }

        for (int i = 0; i < runners; i++) {
            Player selected = noRolesPlayers.get(random.nextInt(noRolesPlayers.size()));
            roleMapTemp.put(selected, PlayerRole.RUNNER);
            noRolesPlayers.remove(selected);
        }
        noRolesPlayers.forEach(p -> roleMapTemp.put(p, PlayerRole.HUNTER));
        this.roleMapping = new ConcurrentHashMap<>(roleMapTemp);
        Bukkit.broadcastMessage("正在将逃亡者随机传送到远离猎人的位置...");
        getPlayersAsRole(PlayerRole.RUNNER).forEach(this::airDrop);
        Bukkit.broadcastMessage("设置游戏规则...");
        inGamePlayers.forEach(p->{
            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(40);
            p.setHealth(p.getMaxHealth());
            p.setExp(0.0f);
            p.setCompassTarget(p.getWorld().getSpawnLocation());
            p.getInventory().clear();
        });
        switchWorldRuleForReady(true);
        Bukkit.broadcastMessage("游戏开始！");
        Bukkit.broadcastMessage(ChatColor.AQUA + "欢迎来到 " + ChatColor.GREEN + "MineHunt " + ChatColor.AQUA + "!");
        Bukkit.broadcastMessage(ChatColor.AQUA + "在本游戏中，将会有 " + ChatColor.YELLOW + runners + ChatColor.AQUA + " 名玩家扮演逃亡者，其余玩家扮演猎人");
        Bukkit.broadcastMessage(ChatColor.RED + "猎人需要阻止逃亡者击杀末影龙或击杀逃亡者以取得胜利。");
        Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者需要在猎人的追杀下击败末影龙以取得胜利。逃亡者无法复活且由于任何原因死亡均会导致猎人胜利。");
        Bukkit.broadcastMessage(ChatColor.AQUA + "在游戏过程中，当你解锁特定的游戏阶段时，全体玩家将会获得阶段奖励，可能是特定物品也可能是增益效果。");
        Bukkit.broadcastMessage(ChatColor.AQUA + "猎人可以通过合成指南针来定位逃亡者的方向；逃亡者可以通过合成指南针摧毁猎人的指南针。");
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "祝君好运，末地见！");
        Bukkit.broadcastMessage(ChatColor.RED + "猎人: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
        Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
        status = GameStatus.GAME_STARTED;
        this.registerWatchers();
        plugin.getGame().getProgressManager().unlockProgress(GameProgress.GAME_STARTING);

    }
    private final Map<World, Difficulty> difficultyMap = new HashMap<>();
    public void switchWorldRuleForReady(boolean ready){
        if(ready){
            Bukkit.getWorlds().forEach(world->{
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
                world.setGameRule(GameRule.DO_MOB_SPAWNING,false);
                world.setGameRule(GameRule.DO_FIRE_TICK,false);
                world.setGameRule(GameRule.MOB_GRIEFING,false);
                difficultyMap.put(world,world.getDifficulty());
                world.setDifficulty(Difficulty.PEACEFUL);
            });
        }else{
            Bukkit.getWorlds().forEach(world->{
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,true);
                world.setGameRule(GameRule.DO_MOB_SPAWNING,true);
                world.setGameRule(GameRule.DO_FIRE_TICK,true);
                world.setGameRule(GameRule.MOB_GRIEFING,true);
                world.setDifficulty(difficultyMap.getOrDefault(world,Difficulty.NORMAL));
            });
        }
    }

    public void stop(PlayerRole winner, Location location) {
        this.inGamePlayers.stream().filter(Player::isOnline).forEach(player -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(location);
        });
        Bukkit.broadcastMessage(ChatColor.YELLOW + "游戏结束! 服务器将在30秒后重新启动！");
        String runnerNames = Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList()));
        String hunterNames = Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList()));

        if (winner == PlayerRole.HUNTER) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "胜利者：猎人");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "恭喜你们：" + hunterNames);
            getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功击败了逃亡者", 0, 200, 0));
            getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "不幸阵亡", 0, 200, 0));

        } else {
            Bukkit.broadcastMessage(ChatColor.GOLD + "胜利者：逃亡者");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "恭喜你们：" + runnerNames);
            getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功战胜了末影龙", 0, 200, 0));
            getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "未能阻止末影龙死亡", 0, 200, 0));
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                //开始结算阶段

                //计算输出最多的玩家
                Player mostOutput = null;
                double damage = 0.0d;
                for (Player player : getInGamePlayers()) {
                    if (mostOutput == null) {
                        mostOutput = player;
                        damage = player.getStatistic(Statistic.DAMAGE_DEALT);
                        continue;
                    }
                    double newDamage = player.getStatistic(Statistic.DAMAGE_DEALT);
                    if (newDamage > damage) {
                        mostOutput = player;
                        damage = newDamage;
                    }
                }
                if (mostOutput != null) {
                    gameEndingDataBuilder.damageOutput(mostOutput.getName() + " - " + damage);
                } else {
                    gameEndingDataBuilder.damageOutput("Error");
                }


                //计算受到伤害最多的玩家
                Player mostDamageReceiver = null;
                double received = 0.0d;
                for (Player player : getInGamePlayers()) {
                    if (mostDamageReceiver == null) {
                        mostDamageReceiver = player;
                        received = player.getStatistic(Statistic.DAMAGE_TAKEN);
                        continue;
                    }
                    double newReceived = player.getStatistic(Statistic.DAMAGE_TAKEN);
                    if (newReceived > received) {
                        mostDamageReceiver = player;
                        received = newReceived;
                    }
                }
                if (mostDamageReceiver != null) {
                    gameEndingDataBuilder.damageOutput(mostDamageReceiver.getName() + " - " + received);
                } else {
                    gameEndingDataBuilder.damageOutput("Error");
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendEndingAnimation();
                    }
                }.runTaskLaterAsynchronously(plugin, 20 * 10);
            }
        }.runTaskLater(MineHunt.getInstance(), 20 * 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.shutdown();
            }
        }.runTaskLater(MineHunt.getInstance(), 20 * 30);
    }

    @SneakyThrows
    private void sendEndingAnimation() {
        double maxCanCost = 20000d;
        GameEndingData data = gameEndingDataBuilder.build();
        int needShows = 0;
        if (StringUtils.isNotBlank(data.getDamageOutput())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(data.getDragonKiller())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(data.getDamageReceive())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(data.getStoneAgePassed())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(data.getRunnerKiller())) {
            needShows++;
        }
        maxCanCost /= needShows;

        int sleep = (int) maxCanCost;


        if (StringUtils.isNotBlank(data.getDragonKiller())) {
            inGamePlayers.forEach(p->p.sendTitle("屠龙勇者", data.getDragonKiller(),0 ,20000 ,0 ));
            Thread.sleep(sleep);
        }

        if (StringUtils.isNotBlank(data.getRunnerKiller())) {
            inGamePlayers.forEach(p->p.sendTitle("逃亡者的噩梦", data.getRunnerKiller(),0 ,20000 ,0 ));
            Thread.sleep(sleep);
        }

        if (StringUtils.isNotBlank(data.getDamageOutput())) {
            inGamePlayers.forEach(p->p.sendTitle("最佳伤害输出", data.getDamageOutput(),0 ,20000 ,0 ));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(data.getDamageReceive())) {
            inGamePlayers.forEach(p->p.sendTitle("最惨怪物标靶", data.getDamageReceive(),0 ,20000 ,0 ));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(data.getStoneAgePassed())) {
            inGamePlayers.forEach(p->p.sendTitle("文明的第一步", data.getStoneAgePassed(),0 ,20000 ,0 ));
            Thread.sleep(sleep);
        }

    }

    private void registerWatchers() {
        new RadarWatcher();
        new ReconnectWatcher();
        new PlayerMoveWatcher();
    }

    public List<Player> getPlayersAsRole(PlayerRole role) {
        return this.roleMapping.entrySet().stream().filter(playerPlayerRoleEntry -> playerPlayerRoleEntry.getValue() == role).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    //Code from ManHunt
    private void airDrop(Player runner) {
        Location loc = runner.getLocation();
        loc = new Location(loc.getWorld(), loc.getBlockX(), 0, loc.getBlockZ());
        Random random = new Random();
        loc.add(random.nextInt(200) + 100, 0, random.nextInt(200) + 100);
        loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
        loc.getBlock().setType(Material.GLASS);
        loc.setY(loc.getY() + 1);
        runner.teleport(loc);
    }
}

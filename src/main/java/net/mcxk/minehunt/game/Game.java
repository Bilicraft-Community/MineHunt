package net.mcxk.minehunt.game;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.MrGraycat.eGlow.API.Enum.EGlowColor;
import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.util.GameEndingData;
import net.mcxk.minehunt.util.MusicPlayer;
import net.mcxk.minehunt.util.StatisticsBaker;
import net.mcxk.minehunt.util.Util;
import net.mcxk.minehunt.watcher.PlayerMoveWatcher;
import net.mcxk.minehunt.watcher.RadarWatcher;
import net.mcxk.minehunt.watcher.ReconnectWatcher;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Game {
    @Getter
    final Map<Player, Double> teamDamageData = new HashMap<>();
    private final MineHunt plugin = MineHunt.getInstance();
    @Getter
    private final Set<Player> inGamePlayers = Sets.newCopyOnWriteArraySet(); //线程安全
    @Getter
    private final int countdown = 60;
    @Getter
    private final Map<Player, Long> reconnectTimer = new HashMap<>();
    @Getter
    private final GameProgressManager progressManager = new GameProgressManager();
    @Getter
    private final GameEndingData gameEndingData = new GameEndingData();
    private final Map<World, Difficulty> difficultyMap = new HashMap<>();
    @Getter
    @Setter
    private GameStatus status = GameStatus.WAITING_PLAYERS;
    @Getter
    private Map<Player, PlayerRole> roleMapping; //线程安全
    @Getter
    private final int maxPlayers = plugin.getConfig().getInt("max-players");
    @Getter
    private final int minPlayers = plugin.getConfig().getInt("min-players");
    private final int runnerMax = plugin.getConfig().getInt("runner-max");
    @Getter
    private boolean compassUnlocked = false;

    public Game() {
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
        reconnectTimer.remove(player);
        if (inGamePlayers.size() < maxPlayers) {
            inGamePlayers.add(player);
            return true;
        }
        return false;
    }

    public void fixConfig() {
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
        if (status == GameStatus.WAITING_PLAYERS) {
            this.inGamePlayers.remove(player);
        } else {
            this.reconnectTimer.put(player, System.currentTimeMillis());
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
        setupGlow();
    }

    public void setupGlow(){
        if(plugin.getEGlowAPI() == null) return;
        setupGlowForce();
    }
    public void setupGlowForce(){
        plugin.getLogger().info("Rendering players glowing...");
        getPlayersAsRole(PlayerRole.RUNNER).forEach(player->{
            if(player == null || !player.isOnline()) return;
            plugin.getEGlowAPI().enableGlow(player, EGlowColor.DARK_GREEN);
            plugin.getEGlowAPI().setCustomGlowReceivers(player,getPlayersAsRole(PlayerRole.RUNNER));
        });
        getPlayersAsRole(PlayerRole.HUNTER).forEach(player->{
            if(player == null || !player.isOnline()) return;
            plugin.getEGlowAPI().enableGlow(player, EGlowColor.DARK_RED);
            plugin.getEGlowAPI().setCustomGlowReceivers(player,getPlayersAsRole(PlayerRole.HUNTER));
        });
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
        if (inGamePlayers.size() == maxPlayers) {
            runners = runnerMax;
        }
        if (noRolesPlayers.size() == 0){
             Bukkit.shutdown(); //出错，重启服务器
        }
        for (int i = 0; i < runners; i++) {
            Player selected = noRolesPlayers.get(random.nextInt(noRolesPlayers.size()));
            roleMapTemp.put(selected, PlayerRole.RUNNER);
            noRolesPlayers.remove(selected);
        }
        noRolesPlayers.forEach(p -> roleMapTemp.put(p, PlayerRole.HUNTER));
        this.roleMapping = new ConcurrentHashMap<>(roleMapTemp);


        if(plugin.getConfig().getBoolean("runner-airdrop",true)) {
            Bukkit.broadcastMessage("正在将逃亡者随机传送到远离猎人的位置...");
            Location airDropLoc = airDrop(getPlayersAsRole(PlayerRole.RUNNER).get(0).getWorld().getSpawnLocation());
            getPlayersAsRole(PlayerRole.RUNNER).forEach(runner -> runner.teleport(airDropLoc));
            getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.teleport(p.getWorld().getSpawnLocation()));
        }else{
            getInGamePlayers().forEach(p->p.teleport(p.getWorld().getSpawnLocation()));
        }

        if(getPlayersAsRole(PlayerRole.RUNNER).size() != 2 && getInGamePlayers().size() > 2){
            getPlayersAsRole(PlayerRole.RUNNER).forEach(runner->{
                runner.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0d);
                runner.setHealth(40.0d);
            });
        }

        Bukkit.broadcastMessage("设置游戏规则...");
        inGamePlayers.forEach(p -> {
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
        Bukkit.broadcastMessage(ChatColor.AQUA + "猎人可以通过合成指南针来定位逃亡者的方向。");
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "祝君好运，末地见！");
        Bukkit.broadcastMessage(ChatColor.RED + "猎人: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
        Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
        status = GameStatus.GAME_STARTED;
        this.registerWatchers();
        plugin.getGame().getProgressManager().unlockProgress(GameProgress.GAME_STARTING);
        inGamePlayers.forEach(player -> player.sendTitle(ChatColor.GREEN+"游戏开始","GO GO GO", 0,80,0));
        setupGlow();
    }

    public void switchWorldRuleForReady(boolean ready) {
        if (!ready) {
            Bukkit.getWorlds().forEach(world -> {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.DO_FIRE_TICK, false);
                world.setGameRule(GameRule.MOB_GRIEFING, false);
                difficultyMap.put(world, world.getDifficulty());
                world.setDifficulty(Difficulty.PEACEFUL);
            });
        } else {
            Bukkit.getWorlds().forEach(world -> {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
                world.setGameRule(GameRule.DO_FIRE_TICK, true);
                world.setGameRule(GameRule.MOB_GRIEFING, true);
                world.setDifficulty(difficultyMap.getOrDefault(world, Difficulty.NORMAL));
            });
        }
    }

    public void stop(PlayerRole winner, Location location) {
        this.inGamePlayers.stream().filter(Player::isOnline).forEach(player -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(location.clone().add(0, 3, 0));
            player.teleport(Util.lookAt(player.getEyeLocation(), location));
        });
        this.status = GameStatus.ENDED;
        Bukkit.broadcastMessage(ChatColor.YELLOW + "游戏结束! 服务器将在30秒后重新启动！");
        String runnerNames = Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList()));
        String hunterNames = Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList()));

        if (winner == PlayerRole.HUNTER) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "胜利者：猎人");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "恭喜：" + hunterNames);
            getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功击败了逃亡者", 0, 2000, 0));
            getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "不幸阵亡", 0, 2000, 0));
        } else {
            Bukkit.broadcastMessage(ChatColor.GOLD + "胜利者：逃亡者");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "恭喜：" + runnerNames);
            getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功战胜了末影龙", 0, 2000, 0));
            getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "未能阻止末影龙死亡", 0, 2000, 0));
        }
        new MusicPlayer().playEnding();
        Bukkit.getOnlinePlayers().stream().filter(p -> !inGamePlayers.contains(p)).forEach(p -> p.sendTitle(ChatColor.RED + "游戏结束", "The End", 0, 2000, 0));
        new BukkitRunnable() {
            @Override
            public void run() {
                //开始结算阶段
                StatisticsBaker baker = new StatisticsBaker();
                //计算输出最多的玩家
                getGameEndingData().setDamageOutput(baker.getDamageMaster());
                getGameEndingData().setDamageReceive(baker.getDamageTakenMaster());
                getGameEndingData().setWalkMaster(baker.getWalkingMaster());
                getGameEndingData().setJumpMaster(baker.getJumpMaster());
                getGameEndingData().setTeamKiller(baker.getTeamBadGuy());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendEndingAnimation();
                    }
                }.runTaskLaterAsynchronously(plugin, 20 * 10);
            }
        }.runTaskLater(MineHunt.getInstance(), 20 * 10);
    }

    @SneakyThrows
    private void sendEndingAnimation() {
        double maxCanCost = 20000d;
        int needShows = 0;
        if (StringUtils.isNotBlank(gameEndingData.getDamageOutput())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getDragonKiller())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getDamageReceive())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getStoneAgePassed())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getRunnerKiller())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getWalkMaster())) {
            needShows++;
        }
        if (StringUtils.isNotBlank(gameEndingData.getJumpMaster())) {
            needShows++;
        }
        maxCanCost /= needShows;

        int sleep = (int) maxCanCost;

        if (StringUtils.isNotBlank(gameEndingData.getDragonKiller())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GOLD + "屠龙勇士", gameEndingData.getDragonKiller(), 0, 20000, 0));
            Thread.sleep(sleep);
        }

        if (StringUtils.isNotBlank(gameEndingData.getRunnerKiller())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.RED + "逃亡者的噩梦", gameEndingData.getRunnerKiller(), 0, 20000, 0));
            Thread.sleep(sleep);
        }

        if (StringUtils.isNotBlank(gameEndingData.getDamageOutput())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.AQUA + "最佳伤害输出", gameEndingData.getDamageOutput(), 0, 20000, 0));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(gameEndingData.getDamageReceive())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.LIGHT_PURPLE + "最惨怪物标靶", gameEndingData.getDamageReceive(), 0, 20000, 0));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(gameEndingData.getTeamKiller())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.DARK_RED + "队友杀手", gameEndingData.getTeamKiller(), 0, 20000, 0));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(gameEndingData.getWalkMaster())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.YELLOW + "大探险家", gameEndingData.getWalkMaster(), 0, 20000, 0));
            Thread.sleep(sleep);
        }
        if (StringUtils.isNotBlank(gameEndingData.getJumpMaster())) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GRAY + "CS:GO玩家", gameEndingData.getJumpMaster(), 0, 20000, 0));
            Thread.sleep(sleep);
        }

        Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + "感谢游玩", "Thanks for playing!", 0, 20000, 0));
        Thread.sleep(sleep);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + plugin.getConfig().getString("server-name"), "友尽大逃杀-MineHunt", 0, 20000, 0));
        Thread.sleep(sleep);
        Bukkit.getOnlinePlayers().forEach(Player::resetTitle);
        Bukkit.shutdown();
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

    private Location airDrop(Location spawnpoint) {
        Location loc = spawnpoint.clone();
        loc = new Location(loc.getWorld(), loc.getBlockX(), 0, loc.getBlockZ());
        Random random = new Random();
        loc.add(random.nextInt(200) + 100, 0, random.nextInt(200) + 100);
        loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
        loc.getBlock().setType(Material.GLASS);
        loc.setY(loc.getY() + 1);
        return loc;
    }
}

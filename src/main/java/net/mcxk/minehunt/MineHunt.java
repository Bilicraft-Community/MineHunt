package net.mcxk.minehunt;

import lombok.Getter;
import net.mcxk.minehunt.game.Game;
import net.mcxk.minehunt.game.GameStatus;
import net.mcxk.minehunt.game.PlayerRole;
import net.mcxk.minehunt.listener.*;
import net.mcxk.minehunt.util.Util;
import net.mcxk.minehunt.watcher.CountDownWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public final class MineHunt extends JavaPlugin {
    @Getter
    private static MineHunt instance;
    @Getter
    private Game game;

    @Getter
    private CountDownWatcher countDownWatcher;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        instance = this;
        game = new Game();
        countDownWatcher = new CountDownWatcher();
        game.switchWorldRuleForReady(false);
        Bukkit.getPluginManager().registerEvents(new PlayerServerListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCompassListener(), this);
        Bukkit.getPluginManager().registerEvents(new ProgressDetectingListener(), this);
        Bukkit.getPluginManager().registerEvents(new GameWinnerListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        //禁止删除本行版权声明
        if (args[0].equalsIgnoreCase("copyright")) {
            sender.sendMessage("Copyright - Minecraft of gamerteam. 版权所有.");
            return true;
        }



        //不安全命令 完全没做检查，确认你会用再执行
        if (args[0].equalsIgnoreCase("hunter") || args[0].equalsIgnoreCase("runner")) {
            if(!sender.hasPermission("minehunt.admin")){
                return false;
            }
            Player player = (Player) sender;
            this.getGame().getInGamePlayers().add(player);
            if (args[0].equalsIgnoreCase("hunter")) {
                this.getGame().getRoleMapping().put(player, PlayerRole.HUNTER);
            } else {
                this.getGame().getRoleMapping().put(player, PlayerRole.RUNNER);
            }
            player.setGameMode(GameMode.SURVIVAL);
            Bukkit.broadcastMessage("玩家 " + sender.getName() + " 强制加入了游戏！ 身份：" + args[0]);
            return true;
        }
        if (args[0].equalsIgnoreCase("resetcountdown") && this.getGame().getStatus() == GameStatus.WAITING_PLAYERS) {
            if(!sender.hasPermission("minehunt.admin")){
                return false;
            }
            this.getCountDownWatcher().resetCountdown();
            return true;
        }
        if (args[0].equalsIgnoreCase("forcestart") && this.getGame().getStatus() == GameStatus.WAITING_PLAYERS) {
            if(!sender.hasPermission("minehunt.admin")){
                return false;
            }
            if (this.getGame().getInGamePlayers().size() < 2) {
                sender.sendMessage("错误：至少有2名玩家才可以强制开始游戏");
                return true;
            }
            this.game.start();
            return true;
        }
        if (args[0].equalsIgnoreCase("teams") && this.getGame().getStatus() == GameStatus.GAME_STARTED) {
            sender.sendMessage(ChatColor.RED + "猎人: " + Util.list2String(getGame().getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
            sender.sendMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(getGame().getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
            return true;
        }

        if (args[0].equalsIgnoreCase("ob")) {
            if(!(sender instanceof Player)){
                return false;
            }
            Player player = (Player)sender;
            if (this.getGame().getStatus() != GameStatus.GAME_STARTED || player.getGameMode() != GameMode.SPECTATOR) {
                sender.sendMessage("错误：您的身份不是观察者");
                return true;
            }
            if(args.length < 2){
                sender.sendMessage("错误：请输入要观察的玩家的游戏ID");
                return true;
            }
            Player obPlayer =  Bukkit.getPlayer(args[1]);
            if(obPlayer == null){
                sender.sendMessage("错误：指定玩家不存在");
                return true;
            }
            player.teleport(obPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return true;
        }

        return false;
    }

}

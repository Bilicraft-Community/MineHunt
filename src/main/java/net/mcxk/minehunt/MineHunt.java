package net.mcxk.minehunt;

import lombok.Getter;
import net.mcxk.minehunt.game.Game;
import net.mcxk.minehunt.listener.*;
import net.mcxk.minehunt.watcher.CountDownWatcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineHunt extends JavaPlugin {
    @Getter
    private static MineHunt instance;
    @Getter
    private Game game;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        game = new Game();
        new CountDownWatcher();
        game.switchWorldRuleForReady(false);
        Bukkit.getPluginManager().registerEvents(new PlayerServerListener(),this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(),this);
        Bukkit.getPluginManager().registerEvents(new PlayerCompassListener(),this);
        Bukkit.getPluginManager().registerEvents(new ProgressDetectingListener(),this);
        Bukkit.getPluginManager().registerEvents(new GameWinnerListener(),this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


}

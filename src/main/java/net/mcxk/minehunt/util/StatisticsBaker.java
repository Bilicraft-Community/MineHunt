package net.mcxk.minehunt.util;

import net.mcxk.minehunt.MineHunt;
import net.mcxk.minehunt.game.Game;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.Map;

public class StatisticsBaker {
    private final Game game = MineHunt.getInstance().getGame();

    public String getDamageMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.DAMAGE_DEALT);
        return result.getKey() + " 对其他生物造成了 " + result.getValue().intValue() + " 点伤害";
    }

    public String getDamageTakenMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.DAMAGE_TAKEN);
        return result.getKey() + " 共受到了 " + result.getValue().intValue() + " 点伤害";
    }

    public String getWalkingMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.WALK_ONE_CM);
        return result.getKey() + " 旅行了 " + result.getValue().intValue() + " 米";
    }

    public String getCraftingMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.CRAFT_ITEM);
        return result.getKey() + " 合成了 " + result.getValue().intValue() + " 个物品";
    }

    public String getWidgetMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.USE_ITEM);
        return result.getKey() + " 使用了 " + result.getValue().intValue() + " 次各种物品";
    }

    public String getKillingMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.KILL_ENTITY);
        return result.getValue().intValue() + " 只怪物倒在了 " + result.getKey() + " 的手下";
    }

    public String getJumpMaster() {
        Map.Entry<String, Double> result = getHighest(Statistic.JUMP);
        return "生命不息，空格不停 " + result.getKey() + " 共跳跃了 " + result.getValue().intValue() + " 次";
    }

    public Map.Entry<String, Double> getHighest(Statistic statistic) {
        Player playerMax = null;
        double dataMax = 0.0d;
        for (Player filtering : game.getInGamePlayers()) {
            if (playerMax == null) {
                playerMax = filtering;
                dataMax = filtering.getStatistic(statistic);
                continue;
            }
            double data = filtering.getStatistic(statistic);
            if (dataMax < data) {
                playerMax = filtering;
                dataMax = data;
            }
        }
        if (playerMax == null) {
            return new AbstractMap.SimpleEntry<>("Null", 0.0d);
        }
        return new AbstractMap.SimpleEntry<>(playerMax.getName(), dataMax);
    }
}

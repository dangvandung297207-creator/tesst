package com.megafishing.ui;

import com.megafishing.fishing.FishingSession;
import com.megafishing.fishing.FishingState;
import com.megafishing.util.MathUtil;
import com.megafishing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public void update(Player player, FishingSession session, double safeMax, double powerMax) {
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), id -> {
            BossBar created = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            created.addPlayer(player);
            return created;
        });
        double healthPercent = session.getFish() == null ? 0.0D : session.getFish().getHealth() / session.getFish().getMaxHealth();
        bar.setProgress(MathUtil.clamp(healthPercent, 0.0D, 1.0D));
        if (session.getState() == FishingState.EXHAUSTED) {
            bar.setColor(BarColor.GREEN);
        } else if (session.getTension() >= powerMax) {
            bar.setColor(BarColor.RED);
        } else if (session.getTension() >= safeMax) {
            bar.setColor(BarColor.YELLOW);
        } else {
            bar.setColor(BarColor.BLUE);
        }
        if (session.getFish() != null) {
            bar.setTitle(Text.color(session.getFish().getDefinition().getDisplayName() + " &7" + Text.number(session.getFish().getHealth()) + " / " + Text.number(session.getFish().getMaxHealth()) + " HP"));
        }
    }

    public void clear(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    public int activeBars() {
        return bars.size();
    }
}

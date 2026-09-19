package com.megafishing.ui;

import com.megafishing.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ActionBarManager {
    public void send(Player player, String legacy) {
        player.sendActionBar(Text.component(legacy));
    }

    public void clear(Player player) {
        player.sendActionBar(Component.empty());
    }
}

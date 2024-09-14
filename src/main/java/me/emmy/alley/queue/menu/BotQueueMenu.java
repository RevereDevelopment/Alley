package me.emmy.alley.queue.menu;

import lombok.AllArgsConstructor;
import me.emmy.alley.queue.Queue;
import me.emmy.alley.utils.menu.Button;
import me.emmy.alley.utils.menu.Menu;
import me.emmy.alley.utils.menu.button.BackButton;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmy
 * @project Alley
 * @date 14/09/2024 - 23:58
 */
@AllArgsConstructor
public class BotQueueMenu extends Menu {

    private final Queue queue;

    @Override
    public String getTitle(Player player) {
        return "Bot Queue";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(0, new BackButton(new QueuesMenu()));

        addBorder(buttons, (byte) 15, 5);

        return buttons;
    }

    @Override
    public int getSize() {
        return 9 * 5;
    }
}
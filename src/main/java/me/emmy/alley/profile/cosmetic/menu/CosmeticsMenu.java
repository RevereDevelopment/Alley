package me.emmy.alley.profile.cosmetic.menu;

import lombok.AllArgsConstructor;
import me.emmy.alley.Alley;
import me.emmy.alley.profile.cosmetic.menu.button.SoundEffectButton;
import me.emmy.alley.profile.cosmetic.menu.button.CosmeticButton;
import me.emmy.alley.profile.cosmetic.menu.button.KillEffectButton;
import me.emmy.alley.utils.menu.Button;
import me.emmy.alley.utils.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Remi
 * @project Alley
 * @date 6/1/2024
 */
@AllArgsConstructor
public class CosmeticsMenu extends Menu {

    private final String cosmeticType;

    @Override
    public String getTitle(Player player) {
        return "&8Cosmetics Menu - " + cosmeticType + "s";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        final Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(3, new KillEffectButton());
        buttons.put(5, new SoundEffectButton());

        Alley.getInstance().getCosmeticRepository().getCosmeticRepositories().get(cosmeticType).getCosmetics().stream()
                .filter(cosmetic -> cosmetic.getIcon() != null)
                .forEach(cosmetic -> buttons.put(cosmetic.getSlot(), new CosmeticButton(cosmetic)));

        addBorder(buttons, (byte) 6, 5);
        return buttons;
    }
}

package dev.revere.alley.kit.command.impl.data.inventory;

import dev.revere.alley.Alley;
import dev.revere.alley.kit.Kit;
import dev.revere.alley.locale.Locale;
import dev.revere.alley.util.chat.CC;
import dev.revere.alley.api.command.BaseCommand;
import dev.revere.alley.api.command.Command;
import dev.revere.alley.api.command.CommandArgs;
import org.bukkit.entity.Player;

/**
 * @author Emmy
 * @project Practice
 * @date 28/04/2024 - 22:25
 */
public class KitGetInvCommand extends BaseCommand {
    @Command(name = "kit.getinventory", aliases = "kit.getinv",permission = "alley.admin")
    @Override
    public void onCommand(CommandArgs command) {
        Player sender = command.getPlayer();
        String[] args = command.getArgs();

        if (command.length() < 1) {
            sender.sendMessage(CC.translate("&6Usage: &e/kit getinventory &b<kitName>"));
            return;
        }

        String kitName = args[0];
        Kit kit = Alley.getInstance().getKitRepository().getKit(kitName);

        if (kit == null) {
            sender.sendMessage(CC.translate(Locale.KIT_NOT_FOUND.getMessage()));
            return;
        }

        sender.getInventory().setContents(kit.getInventory());
        sender.getInventory().setArmorContents(kit.getArmor());
        sender.sendMessage(CC.translate(Locale.KIT_INVENTORY_GIVEN.getMessage().replace("{kit-name}", kitName)));
    }
}
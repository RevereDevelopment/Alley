package me.emmy.alley.commands.admin.kit.impl;

import me.emmy.alley.Alley;
import me.emmy.alley.utils.chat.CC;
import me.emmy.alley.utils.command.BaseCommand;
import me.emmy.alley.utils.command.Command;
import me.emmy.alley.utils.command.CommandArgs;
import org.bukkit.command.CommandSender;

/**
 * Created by Emmy
 * Project: Practice
 * Date: 28/04/2024 - 22:07
 */

public class KitListCommand extends BaseCommand {
    @Override
    @Command(name = "kit.list", permission = "alley.admin")
    public void onCommand(CommandArgs command) {
        CommandSender sender = command.getSender();

        sender.sendMessage("");
        sender.sendMessage(CC.FLOWER_BAR);
        sender.sendMessage(CC.translate("     &d&lKit list &f(" + Alley.getInstance().getKitManager().getKits().size() + "&f)"));
        if (Alley.getInstance().getKitManager().getKits().isEmpty()) {
            sender.sendMessage(CC.translate("      &f● &cNo Kits available."));
        }
        Alley.getInstance().getKitManager().getKits().forEach(kit -> sender.sendMessage(CC.translate("      &f● &d" + kit.getDisplayName())));
        sender.sendMessage(CC.FLOWER_BAR);
        sender.sendMessage("");
    }
}
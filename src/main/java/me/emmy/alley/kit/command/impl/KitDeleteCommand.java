package me.emmy.alley.kit.command.impl;

import me.emmy.alley.Alley;
import me.emmy.alley.kit.Kit;
import me.emmy.alley.locale.Locale;
import me.emmy.alley.utils.chat.CC;
import me.emmy.alley.utils.command.BaseCommand;
import me.emmy.alley.utils.command.Command;
import me.emmy.alley.utils.command.CommandArgs;
import org.bukkit.entity.Player;

public class KitDeleteCommand extends BaseCommand {
    @Override
    @Command(name = "kit.delete", permission = "alley.admin")
    public void onCommand(CommandArgs command) {
        Player player = command.getPlayer();
        String[] args = command.getArgs();

        if (args.length < 1) {
            player.sendMessage(CC.translate("&cUsage: /kit delete (kit-name)"));
            return;
        }

        String kitName = args[0];
        Kit kit = Alley.getInstance().getKitRepository().getKit(kitName);

        if (kit == null) {
            player.sendMessage(CC.translate(Locale.KIT_NOT_FOUND.getMessage()));
            return;
        }

        Alley.getInstance().getKitRepository().deleteKit(kit);
        player.sendMessage(CC.translate(Locale.KIT_DELETED.getMessage().replace("{kit-name}", kitName)));
    }
}
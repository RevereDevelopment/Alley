package me.emmy.alley.essential.command;

import me.emmy.alley.api.command.BaseCommand;
import me.emmy.alley.api.command.Command;
import me.emmy.alley.api.command.CommandArgs;
import me.emmy.alley.util.PotionUtil;
import me.emmy.alley.util.chat.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author Emmy
 * @project Alley
 * @date 03/11/2024 - 20:28
 */
public class PotionDurationCommand extends BaseCommand {
    @Command(name = "potionduration", permission = "alley.command.potionduration")
    @Override
    public void onCommand(CommandArgs command) {
        Player player = command.getPlayer();
        String[] args = command.getArgs();

        if (args.length < 1) {
            player.sendMessage(CC.translate("&6Usage: &e/potionduration &b<duration/infinite>"));
            return;
        }

        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getType() != Material.POTION) {
            player.sendMessage(CC.translate("&cYou must be holding a potion."));
            return;
        }

        PotionMeta potionMeta = (PotionMeta) itemInHand.getItemMeta();
        if (potionMeta == null) {
            player.sendMessage(CC.translate("&cThe potion has no effects to modify."));
            return;
        }

        if (args[0].equalsIgnoreCase("infinite")) {
            PotionEffectType effectType = PotionUtil.getPotionEffectType(itemInHand);
            if (effectType == null) {
                player.sendMessage(CC.translate("&cThe potion has no effects to modify."));
                return;
            }

            PotionEffect newEffect = new PotionEffect(effectType, Integer.MAX_VALUE, PotionUtil.getPotionEffectAmplifier(itemInHand));
            potionMeta.clearCustomEffects();
            potionMeta.addCustomEffect(newEffect, true);
            itemInHand.setItemMeta(potionMeta);

            player.sendMessage(CC.translate("&aPotion duration updated to &binfinite&a."));
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            player.sendMessage(CC.translate("&cIncorrect duration format. Please enter a number."));
            return;
        }

        if (duration < 1) {
            player.sendMessage(CC.translate("&cInvalid duration."));
            return;
        }

        PotionEffectType effectType = PotionUtil.getPotionEffectType(itemInHand);
        if (effectType == null) {
            player.sendMessage(CC.translate("&cThe potion has no effects to modify."));
            return;
        }

        PotionEffect newEffect = new PotionEffect(effectType, duration * 20, PotionUtil.getPotionEffectAmplifier(itemInHand));
        potionMeta.clearCustomEffects();
        potionMeta.addCustomEffect(newEffect, true);
        itemInHand.setItemMeta(potionMeta);

        String seconds = duration == 1 ? "second" : "seconds";
        player.sendMessage(CC.translate("&aPotion duration updated to &b" + duration + " &a" + seconds + "."));
    }
}
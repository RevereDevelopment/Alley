package me.emmy.alley.game.match.listener;

import me.emmy.alley.Alley;
import me.emmy.alley.arena.Arena;
import me.emmy.alley.config.ConfigHandler;
import me.emmy.alley.cooldown.Cooldown;
import me.emmy.alley.cooldown.CooldownRepository;
import me.emmy.alley.cooldown.enums.EnumCooldownType;
import me.emmy.alley.game.match.AbstractMatch;
import me.emmy.alley.game.match.MatchUtility;
import me.emmy.alley.game.match.enums.EnumMatchState;
import me.emmy.alley.game.match.player.GameParticipant;
import me.emmy.alley.game.match.player.impl.MatchGamePlayerImpl;
import me.emmy.alley.kit.settings.impl.*;
import me.emmy.alley.locale.ErrorMessage;
import me.emmy.alley.profile.Profile;
import me.emmy.alley.profile.enums.EnumProfileState;
import me.emmy.alley.util.ActionBarUtil;
import me.emmy.alley.util.PlayerUtil;
import me.emmy.alley.util.chat.CC;
import me.emmy.alley.util.chat.Logger;
import me.emmy.alley.util.location.RayTracerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * @author Remi
 * @project Alley
 * @date 5/21/2024
 */
public class MatchListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        if (profile.getState() == EnumProfileState.SPECTATING) event.setCancelled(true);
        if (profile.getState() == EnumProfileState.PLAYING) {
            if (profile.getMatch().getState() != EnumMatchState.RUNNING) {
                event.setCancelled(true);
                return;
            }

            if (profile.getMatch().getGamePlayer(player).isDead()) {
                event.setCancelled(true);
                return;
            }

            if (profile.getMatch().getKit().isSettingEnabled(KitSettingBoxingImpl.class)
                    || profile.getMatch().getKit().isSettingEnabled(KitSettingSumoImpl.class)
                    || profile.getMatch().getKit().isSettingEnabled(KitSettingSpleefImpl.class)
                    || profile.getMatch().getKit().isSettingEnabled(KitSettingNoDamageImpl.class)) {
                event.setDamage(0);
                player.setHealth(20.0);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();

        } else if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) event.getDamager()).getShooter();
            } else {
                return;
            }
        } else {
            return;
        }

        if (attacker != null && event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Profile damagedprofile = Alley.getInstance().getProfileRepository().getProfile(damaged.getUniqueId());
            Profile attackerProfile = Alley.getInstance().getProfileRepository().getProfile(attacker.getUniqueId());

            if (damagedprofile.getState() == EnumProfileState.SPECTATING) {
                event.setCancelled(true);
                return;
            }

            if (attackerProfile.getState() == EnumProfileState.SPECTATING) {
                event.setCancelled(true);
                return;
            }

            if (damagedprofile.getState() == EnumProfileState.PLAYING) {
                AbstractMatch match = attackerProfile.getMatch();
                if (damagedprofile.getMatch().getState() != EnumMatchState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }

                if (match.getGamePlayer(damaged).isDead()) {
                    event.setCancelled(true);
                    return;
                }

                if (match.getGamePlayer(attacker).isDead()) {
                    event.setCancelled(true);
                    return;
                }

                attackerProfile.getMatch().getGamePlayer(attacker).getData().handleAttack();
                damagedprofile.getMatch().getGamePlayer(damaged).getData().resetCombo();

                if (match.getKit().isSettingEnabled(KitSettingBoxingImpl.class)) {
                    if (match.getGamePlayer(attacker).getData().getHits() >= 100) {
                        match.handleDeath(damaged);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.SNOWBALL && event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());

            if (profile.getState() == EnumProfileState.PLAYING && profile.getMatch().getState() == EnumMatchState.RUNNING &&
                    profile.getMatch().getKit().isSettingEnabled(KitSettingSpleefImpl.class)) {

                Snowball snowball = (Snowball) event.getEntity();
                Location hitLocation = RayTracerUtil.rayTrace(snowball.getLocation(), snowball.getVelocity().normalize());

                if (hitLocation.getBlock().getType() == Material.SNOW || hitLocation.getBlock().getType() == Material.SNOW_BLOCK) {
                    hitLocation.getBlock().setType(Material.AIR);
                    player.sendMessage(CC.translate(ErrorMessage.DEBUG));
                }
            }
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        switch (profile.getState()) {
            case PLAYING:
                if (profile.getMatch().getKit().isSettingEnabled(KitSettingBuildImpl.class)) {
                    BlockState blockState = event.getBlock().getState();

                    if (profile.getMatch().getPlacedBlocks().containsKey(blockState)) {
                        profile.getMatch().removeBlockFromPlacedBlocksMap(blockState);
                    } else {
                        Logger.debug("Cannot break block.");
                        event.setCancelled(true);
                    }
                    return;
                }

                if (profile.getMatch().getKit().isSettingEnabled(KitSettingSpleefImpl.class)) {
                    Block block = event.getBlock();
                    if (block.getType() != Material.SNOW_BLOCK) {
                        event.setCancelled(true);
                        return;
                    }

                    event.setCancelled(false);
                    event.getBlock().setType(Material.AIR);

                    int amount = random.nextInt(100) < 50 ? random.nextInt(3) + 2 : 0;
                    if (amount > 0) {
                        ItemStack snowballs = new ItemStack(Material.SNOW_BALL, amount);
                        player.getInventory().addItem(snowballs);
                    }
                    return;
                }

                event.setCancelled(true);
                break;
            case SPECTATING:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        switch (profile.getState()) {
            case PLAYING:
                if (profile.getMatch().getState() == EnumMatchState.STARTING) event.setCancelled(true);
                if (profile.getMatch().getState() == EnumMatchState.ENDING_MATCH) return;

                if (profile.getMatch().getKit().isSettingEnabled(KitSettingBuildImpl.class)) {
                    profile.getMatch().addBlockToPlacedBlocksMap(event.getBlock().getState(), event.getBlockPlaced().getLocation());
                    return;
                }
                event.setCancelled(true);
                break;
            case SPECTATING:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        if (profile.getState() == EnumProfileState.SPECTATING || profile.getState() == EnumProfileState.PLAYING) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                Arena arena = profile.getMatch().getArena();
                Location corner1 = arena.getMinimum();
                Location corner2 = arena.getMaximum();

                double minX = Math.min(corner1.getX(), corner2.getX());
                double maxX = Math.max(corner1.getX(), corner2.getX());
                double minY = Math.min(corner1.getY(), corner2.getY());
                double maxY = Math.max(corner1.getY(), corner2.getY());
                double minZ = Math.min(corner1.getZ(), corner2.getZ());
                double maxZ = Math.max(corner1.getZ(), corner2.getZ());

                Location to = event.getTo();

                boolean withinBounds = to.getX() >= minX && to.getX() <= maxX && to.getY() >= minY && to.getY() <= maxY && to.getZ() >= minZ && to.getZ() <= maxZ;
                if (!withinBounds) {
                    event.setCancelled(true);
                    player.sendMessage(CC.translate("&cYou cannot leave the arena."));
                }
            }
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        AbstractMatch match = profile.getMatch();

        if (profile != null && profile.getMatch() != null) {
            if (profile.getState() == EnumProfileState.PLAYING && profile.getMatch().getState() == EnumMatchState.RUNNING) {
                if (profile.getMatch().getKit().isSettingEnabled(KitSettingSumoImpl.class) || profile.getMatch().getKit().isSettingEnabled(KitSettingSpleefImpl.class)) {
                    if (player.getLocation().getBlock().getType() == Material.WATER || player.getLocation().getBlock().getType() == Material.STATIONARY_WATER) {
                        player.setHealth(0);
                    }
                }

                if (profile.getMatch().getKit().isSettingEnabled(KitSettingLivesImpl.class)) {
                    if (player.getLocation().getY() <= ConfigHandler.getInstance().getSettingsConfig().getInt("game.death-y-level")) {
                        if (player.getGameMode() == GameMode.SPECTATOR) return;
                        if (player.getGameMode() == GameMode.CREATIVE) return;
                        player.setHealth(0);
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                }
            }

            if (profile.getState() == EnumProfileState.PLAYING) {
                if (match.getState() == EnumMatchState.STARTING) {
                    if (match.getKit().isSettingEnabled(KitSettingDenyMovementImpl.class)) {
                        List<GameParticipant<MatchGamePlayerImpl>> participants = match.getParticipants();
                        MatchUtility.denyPlayerMovement(participants, player, match);
                    }
                }
            }

            if (profile.getState() == EnumProfileState.SPECTATING || profile.getState() == EnumProfileState.PLAYING) {
                if (profile.getMatch() == null) return;
                Arena arena = profile.getMatch().getArena();
                Location corner1 = arena.getMinimum();
                Location corner2 = arena.getMaximum();

                double minX = Math.min(corner1.getX(), corner2.getX());
                double maxX = Math.max(corner1.getX(), corner2.getX());
                double minY = Math.min(corner1.getY(), corner2.getY());
                double maxY = Math.max(corner1.getY(), corner2.getY());
                double minZ = Math.min(corner1.getZ(), corner2.getZ());
                double maxZ = Math.max(corner1.getZ(), corner2.getZ());

                Location to = event.getTo();

                //boolean withinBounds = to.getX() >= minX && to.getX() <= maxX && to.getY() >= minY && to.getY() <= maxY && to.getZ() >= minZ && to.getZ() <= maxZ;

                boolean withinBounds;
                if (profile.getMatch().getState() == EnumMatchState.ENDING_MATCH) {
                    withinBounds = to.getX() >= minX && to.getX() <= maxX && to.getZ() >= minZ && to.getZ() <= maxZ;
                } else {
                    withinBounds = to.getX() >= minX && to.getX() <= maxX && to.getY() >= minY && to.getY() <= maxY && to.getZ() >= minZ && to.getZ() <= maxZ;
                }

                if (!withinBounds) {
                    player.teleport(event.getFrom());
                    player.sendMessage(CC.translate("&cYou cannot leave the arena."));
                }
            }
        }
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        if (profile.getState() == EnumProfileState.PLAYING) {
            event.setRespawnLocation(player.getLocation());
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        Player killer = PlayerUtil.getLastAttacker(player);
        if (killer != null) {
            profile.getMatch().getParticipants()
                    .forEach(participant -> participant.getPlayer().getPlayer().sendMessage(CC.translate("&c" + player.getName() + " &fwas killed by &c" + killer.getName() + "&f.")));
        }

        ActionBarUtil.sendMessage(killer, "&c&lKILL! &f" + player.getName(), 3);

        if (profile.getState() == EnumProfileState.PLAYING) {
            event.setDeathMessage(null);

            List<Item> droppedItems = new ArrayList<>();
            for (ItemStack drop : event.getDrops()) {
                if (drop != null && drop.getType() != Material.AIR) {
                    droppedItems.add(player.getWorld().dropItemNaturally(player.getLocation(), drop));
                }
            }
            event.getDrops().clear();

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Item item : droppedItems) {
                        if (item != null && item.isValid()) {
                            item.remove();
                        }
                    }
                }
            }.runTaskLater(Alley.getInstance(), 100L);

            Alley.getInstance().getServer().getScheduler().runTaskLater(Alley.getInstance(), () -> player.spigot().respawn(), 1L);
            profile.getMatch().createSnapshot(player, killer);
            profile.getMatch().handleDeath(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Profile profile = Alley.getInstance().getProfileRepository().getProfile(event.getEntity().getUniqueId());
            if (profile.getState() == EnumProfileState.SPECTATING) {
                event.setCancelled(true);
                return;
            }

            if (profile.getState() == EnumProfileState.PLAYING) {
                Player player = (Player) event.getEntity();
                Player damager = (Player) event.getDamager();
                PlayerUtil.setLastAttacker(player, damager);
            }
        }
    }

    @EventHandler
    private void handleParkourInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        if (profile.getState() != EnumProfileState.PLAYING) {
            return;
        }

        if (!profile.getMatch().getKit().isSettingEnabled(KitSettingParkourImpl.class)) {
            return;
        }

        if (event.getAction() != Action.PHYSICAL || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.STONE_PLATE) {
            return;
        }

        if (profile.getState() == EnumProfileState.PLAYING) {
            profile.getMatch().getParticipants().forEach(participant -> {
                Player participantPlayer = participant.getPlayer().getPlayer();
                if (participantPlayer != null && !participantPlayer.getUniqueId().equals(player.getUniqueId())) {
                    profile.getMatch().handleDeath(participantPlayer);
                }
            });
        }
    }

    @EventHandler
    private void onPlayerPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        ItemStack item = event.getItem();

        if (profile.getMatch() == null) {
            return;
        }

        if (profile.getMatch().getState() == EnumMatchState.STARTING && item != null && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            player.updateInventory();
            player.sendMessage(CC.translate("&cYou cannot use ender pearls during the starting phase."));
            return;
        }

        if (profile.getMatch().getKit().isSettingEnabled(KitSettingLivesImpl.class)) {
            return;
        }

        if (profile.getState() == EnumProfileState.PLAYING && item != null && item.getType() == Material.ENDER_PEARL) {
            CooldownRepository cooldownRepository = Alley.getInstance().getCooldownRepository();
            Optional<Cooldown> optionalCooldown = Optional.ofNullable(cooldownRepository.getCooldown(player.getUniqueId(), EnumCooldownType.ENDER_PEARL));

            if (optionalCooldown.isPresent() && optionalCooldown.get().isActive()) {
                event.setCancelled(true);
                player.updateInventory();
                player.sendMessage(CC.translate("&cYou must wait " + optionalCooldown.get().remainingTime() + " seconds before using another ender pearl."));
                return;
            }

            Cooldown cooldown = optionalCooldown.orElseGet(() -> {
                Cooldown newCooldown = new Cooldown(EnumCooldownType.ENDER_PEARL, () -> player.sendMessage(CC.translate("&aYou can now use pearls again!")));
                cooldownRepository.addCooldown(player.getUniqueId(), EnumCooldownType.ENDER_PEARL, newCooldown);
                return newCooldown;
            });

            cooldown.resetCooldown();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null) return;

        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());
        if (profile.getState() == EnumProfileState.SPECTATING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());

        if (profile.getState() == EnumProfileState.PLAYING) {
            profile.setState(EnumProfileState.LOBBY);
            AbstractMatch match = profile.getMatch();

            if (match.getState() == EnumMatchState.STARTING || match.getState() == EnumMatchState.RUNNING) {
                if (profile.getMatch().getKit().isSettingEnabled(KitSettingLivesImpl.class)) {
                    profile.getMatch().getGamePlayer(player).getData().setLives(0);
                    profile.getMatch().handleDeath(player);
                    return;
                }
                match.handleDisconnect(player);
            }
        }
    }

    @EventHandler
    private void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());

        if (profile.getState() == EnumProfileState.PLAYING) {
            AbstractMatch match = profile.getMatch();

            if (match.getState() == EnumMatchState.STARTING || match.getState() == EnumMatchState.RUNNING) {
                if (profile.getMatch().getKit().isSettingEnabled(KitSettingLivesImpl.class)) {
                    profile.getMatch().getGamePlayer(player).getData().setLives(0);
                    profile.getMatch().handleDeath(player);
                    return;
                }
                match.handleDisconnect(player);
            }
        }
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());

        if (profile.getState() == EnumProfileState.SPECTATING) {
            event.setCancelled(true);
            return;
        }

        if (profile.getState() == EnumProfileState.PLAYING) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    private void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Profile profile = Alley.getInstance().getProfileRepository().getProfile(player.getUniqueId());

            if (profile.getState() != EnumProfileState.PLAYING) {
                return;
            }

            if (profile.getMatch().getKit().isSettingEnabled(KitSettingNoHungerImpl.class)) {
                event.setCancelled(true);
            }
        }
    }
}

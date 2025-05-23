package com.cosmoquests;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class QuestListener implements Listener {

    private final QuestManager questManager;

    public QuestListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void updateQuestCookies(Player player, Quest quest) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            if (item.getType() != Material.COOKIE) continue;
            if (!item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            if (!meta.hasDisplayName()) continue;
            if (!meta.getDisplayName().contains("CosmoQuest")) continue;

            // Build updated lore based on quest tasks
            List<String> updatedLore = quest.getTasks().stream()
                    .map(task -> {
                        String prefix = task.isComplete() ? "§m" : "";
                        int index = quest.getTasks().indexOf(task) + 1;
                        return index + ". " + prefix + task.getType().name() + " " + task.getAmount() + " " + task.getTarget() +
                                " (§7" + task.getProgress() + "/" + task.getAmount() + "§r)";
                    })
                    .collect(Collectors.toList());

            meta.setLore(updatedLore);
            item.setItemMeta(meta);
            player.getInventory().setItem(i, item);
        }

        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        if (offhandItem != null && offhandItem.getType() == Material.COOKIE && offhandItem.hasItemMeta()) {
            ItemMeta meta = offhandItem.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("CosmoQuest")) {
                List<String> updatedLore = quest.getTasks().stream()
                        .map(task -> {
                            String prefix = task.isComplete() ? "§m" : "";
                            int index = quest.getTasks().indexOf(task) + 1;
                            return index + ". " + prefix + task.getType().name() + " " + task.getAmount() + " " + task.getTarget() +
                                    " (§7" + task.getProgress() + "/" + task.getAmount() + "§r)";
                        })
                        .collect(Collectors.toList());
                meta.setLore(updatedLore);
                offhandItem.setItemMeta(meta);
                player.getInventory().setItemInOffHand(offhandItem);
            }
        }
    }



    private void tryProgress(Player player, TaskType type, String target) {
        Quest quest = questManager.getQuest(player.getUniqueId());
        if (quest == null) return;

        boolean updated = false;
        boolean anyCompleted = false;

        for (Task task : quest.getTasks()) {
            if (task.getType() == type && task.getTarget().equalsIgnoreCase(target)) {
                int before = task.getProgress();
                task.incrementProgress(1);
                if (task.isComplete() && before < task.getAmount()) {
                    player.sendMessage("§aTask complete: " + type.name() + " " + target);
                    anyCompleted = true;
                }
                updated = true;
            }
        }
        updateQuestCookies(player, quest);
        checkAndRewardCompletedCookies(player, quest);

    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        tryProgress(event.getPlayer(), TaskType.MINE, event.getBlock().getType().toString());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            tryProgress(player, TaskType.KILL, event.getEntityType().toString());
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            tryProgress(player, TaskType.CRAFT, event.getRecipe().getResult().getType().toString());
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        tryProgress(player, TaskType.COLLECT, event.getItem().getItemStack().getType().toString());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().getBiome() == event.getTo().getBlock().getBiome()) return;
        Player player = event.getPlayer();
        tryProgress(player, TaskType.EXPLORE, event.getTo().getBlock().getBiome().toString());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getType() == Material.COOKIE) {
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("CosmoQuest")) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCookieEat(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.COOKIE && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("CosmoQuest")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou can't eat the CosmoQuest cookie!");
            }
        }
    }



    public void checkAndRewardCompletedCookies(Player player, Quest quest) {
        PlayerInventory inventory = player.getInventory();
        Location playerLoc = player.getLocation();

        // We'll collect which slots to clear after rewarding
        List<Integer> completedSlots = new ArrayList<>();
        boolean offhandCompleted = false;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) continue;
            if (item.getType() != Material.COOKIE) continue;
            if (!item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            if (!meta.hasDisplayName()) continue;
            if (!meta.getDisplayName().contains("CosmoQuest")) continue;

            // Check if this cookie's tasks are all complete
            if (quest.getTasks().stream().allMatch(Task::isComplete)) {
                // Reward the player once per completed cookie:
                player.sendMessage("§bQuest complete! You’ve earned a reward.");
                player.playSound(playerLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                Firework firework = player.getWorld().spawn(playerLoc, Firework.class);
                FireworkMeta fwMeta = firework.getFireworkMeta();
                fwMeta.addEffect(FireworkEffect.builder()
                    .withColor(Color.AQUA)
                    .withFade(Color.WHITE)
                    .with(Type.BALL_LARGE)
                    .trail(true)
                    .flicker(true)
                    .build());
                fwMeta.setPower(1);
                firework.setFireworkMeta(fwMeta);

                // Remove the cookie from inventory
                completedSlots.add(i);

                // Give reward chest or drop it if inventory full
                ItemStack reward = new ItemStack(Material.CHEST);
                HashMap<Integer, ItemStack> leftovers = inventory.addItem(reward);
                if (!leftovers.isEmpty()) {
                    // Inventory full, drop item on ground
                    player.getWorld().dropItemNaturally(playerLoc, reward);
                }
            }
        }

        // Remove completed cookies from inventory
        for (int slot : completedSlots) {
            inventory.setItem(slot, null);
        }

        // Check offhand separately (slot 40)
        ItemStack offhandItem = inventory.getItemInOffHand();
        if (offhandItem != null && offhandItem.getType() == Material.COOKIE && offhandItem.hasItemMeta()) {
            ItemMeta meta = offhandItem.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("CosmoQuest")) {
                if (quest.getTasks().stream().allMatch(Task::isComplete)) {
                    player.sendMessage("§bQuest complete! You’ve earned a reward.");
                    player.playSound(playerLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                    Firework firework = player.getWorld().spawn(playerLoc, Firework.class);
                    FireworkMeta fwMeta = firework.getFireworkMeta();
                    fwMeta.addEffect(FireworkEffect.builder()
                        .withColor(Color.AQUA)
                        .withFade(Color.WHITE)
                        .with(Type.BALL_LARGE)
                        .trail(true)
                        .flicker(true)
                        .build());
                    fwMeta.setPower(1);
                    firework.setFireworkMeta(fwMeta);

                    // Remove offhand cookie
                    inventory.setItemInOffHand(null);

                    ItemStack reward = new ItemStack(Material.CHEST);
                    HashMap<Integer, ItemStack> leftovers = inventory.addItem(reward);
                    if (!leftovers.isEmpty()) {
                        player.getWorld().dropItemNaturally(playerLoc, reward);
                    }
                }
            }
        }
    }

    public void checkAndRewardCompletedCookiesAnywhere(Player player, Quest quest) {
        PlayerInventory inventory = player.getInventory();
        Location playerLoc = player.getLocation();

        List<Integer> completedSlots = new ArrayList<>();

        // Iterate main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isCosmoQuestCookie(item)) {
                // Check if quest is completed
                if (quest.getTasks().stream().allMatch(Task::isComplete)) {
                    rewardAndRemoveCookie(player, item, i, false, quest);
                    completedSlots.add(i);
                }
            }
        }

        // Remove completed cookies from main inventory after rewarding
        for (int slot : completedSlots) {
            inventory.setItem(slot, null);
        }

        // Check offhand cookie
        ItemStack offhand = inventory.getItemInOffHand();
        if (isCosmoQuestCookie(offhand)) {
            if (quest.getTasks().stream().allMatch(Task::isComplete)) {
                rewardAndRemoveCookie(player, offhand, -1, true, quest);
                inventory.setItemInOffHand(null);
            }
        }
    }

    // Helper method for reward + firework + sound + drop if needed
    private void rewardAndRemoveCookie(Player player, ItemStack cookie, int slot, boolean offhand, Quest quest) {
        Location loc = player.getLocation();

        player.sendMessage("§bQuest complete! You’ve earned a reward.");
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Firework firework = player.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwMeta = firework.getFireworkMeta();
        fwMeta.addEffect(FireworkEffect.builder()
            .withColor(Color.AQUA)
            .withFade(Color.WHITE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .trail(true)
            .flicker(true)
            .build());
        fwMeta.setPower(1);
        firework.setFireworkMeta(fwMeta);

        // Give reward chest, drop if full
        ItemStack reward = new ItemStack(Material.CHEST);
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        if (!leftovers.isEmpty()) {
            player.getWorld().dropItemNaturally(loc, reward);
        }
    }

    // Helper method to check if item is a CosmoQuest cookie
    private boolean isCosmoQuestCookie(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.COOKIE) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("CosmoQuest");
    }

}
package com.cosmoquests;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;

public class QuestListener implements Listener {
    private final QuestManager questManager;

    public QuestListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    private boolean isQuestItem(ItemStack item) {
        return item != null && 
               item.getType() == Material.COOKIE && 
               item.hasItemMeta() && 
               item.getItemMeta().hasDisplayName() && 
               item.getItemMeta().getDisplayName().contains("CosmoQuest");
    }

    public void updateQuestCookie(Player player, ItemStack cookie, Quest quest) {
        if (cookie == null || !cookie.hasItemMeta()) return;
        ItemMeta meta = cookie.getItemMeta();
        if (meta == null) return;

        List<String> updatedLore = quest.getTasks().stream()
            .map(task -> {
                String prefix = task.isComplete() ? "§m" : "";
                int index = quest.getTasks().indexOf(task) + 1;
                return index + ". " + prefix + task.getType().name() + " " + task.getAmount() + " " + task.getTarget() +
                    " (§7" + task.getProgress() + "/" + task.getAmount() + "§r)";
            })
            .collect(Collectors.toList());

        meta.setLore(updatedLore);
        cookie.setItemMeta(meta);
    }

    private void tryProgress(Player player, TaskType type, String target) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isQuestItem(item)) continue;

            String questId = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(CosmoQuestsPlugin.getPlugin(CosmoQuestsPlugin.class), "quest_id"), PersistentDataType.STRING);
            if (questId == null) continue;

            Quest quest = questManager.getQuest(UUID.fromString(questId));
            if (quest == null) continue;

            boolean updated = false;

            for (Task task : quest.getTasks()) {
                if (task.getType() == type && task.getTarget().equalsIgnoreCase(target)) {
                    int before = task.getProgress();
                    task.incrementProgress(1);
                    if (task.isComplete() && before < task.getAmount()) {
                        player.sendMessage("§aTask complete: " + type.name() + " " + target);
                    }
                    updated = true;
                }
            }

            if (updated) {
                updateQuestCookie(player, item, quest);
                checkAndRewardCompletedCookiesAnywhere(player, quest);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        tryProgress(event.getPlayer(), TaskType.MINE, event.getBlock().getType().toString());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            tryProgress(event.getEntity().getKiller(), TaskType.KILL, event.getEntityType().toString());
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            tryProgress((Player) event.getWhoClicked(), TaskType.CRAFT, event.getRecipe().getResult().getType().toString());
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        tryProgress(event.getPlayer(), TaskType.COLLECT, event.getItem().getItemStack().getType().toString());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().getBiome() != event.getTo().getBlock().getBiome()) {
            tryProgress(event.getPlayer(), TaskType.EXPLORE, event.getTo().getBlock().getBiome().toString());
        }
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

    private boolean isCosmoQuestCookie(ItemStack item) {
        return item != null && 
               item.getType() == Material.COOKIE && 
               item.hasItemMeta() && 
               item.getItemMeta().hasDisplayName() && 
               item.getItemMeta().getDisplayName().contains("CosmoQuest");
    }

    private void rewardAndRemoveCookie(Player player, ItemStack cookie, Quest quest) {
        Location loc = player.getLocation();

        player.sendMessage("§bQuest complete! You've earned a reward.");
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

    public void checkAndRewardCompletedCookiesAnywhere(Player player, Quest quest) {
        if (!quest.getTasks().stream().allMatch(Task::isComplete)) return;

        PlayerInventory inventory = player.getInventory();

        // Check main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isCosmoQuestCookie(item)) {
                String itemQuestId = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(CosmoQuestsPlugin.getPlugin(CosmoQuestsPlugin.class), "quest_id"), PersistentDataType.STRING);
                if (itemQuestId != null && itemQuestId.equals(quest.getId().toString())) {
                    rewardAndRemoveCookie(player, item, quest);
                    inventory.setItem(i, null);
                }
            }
        }

        // Check offhand
        ItemStack offhand = inventory.getItemInOffHand();
        if (isCosmoQuestCookie(offhand)) {
            String offhandQuestId = offhand.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(CosmoQuestsPlugin.getPlugin(CosmoQuestsPlugin.class), "quest_id"), PersistentDataType.STRING);
            if (offhandQuestId != null && offhandQuestId.equals(quest.getId().toString())) {
                rewardAndRemoveCookie(player, offhand, quest);
                inventory.setItemInOffHand(null);
            }
        }
    }
}
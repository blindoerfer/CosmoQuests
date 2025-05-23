package com.cosmoquests;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestItemFactory {

    private static final String QUEST_ITEM_NAME = ChatColor.LIGHT_PURPLE + "CosmoQuest Scroll";

    public static ItemStack createQuestItem(Quest quest) {
        ItemStack item = new ItemStack(Material.COOKIE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        // Name with rarity color
        ChatColor rarityColor = getRarityColor(quest.getRarity());
        meta.setDisplayName(rarityColor + "CosmoQuest Scroll [" + quest.getRarity().name() + "]");

        // Lore showing summary
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to view your quests.");
        lore.add(ChatColor.GRAY + "Complete tasks to earn rewards!");
        lore.add("");
        lore.add(ChatColor.GOLD + "Tasks:");

        for (Task task : quest.getTasks()) {
            lore.add(ChatColor.AQUA + "- " + task.getType().name() + ": " + task.getTarget() + " (" + task.getAmount() + ")");
        }

        meta.setLore(lore);

        // Enchant with Unbreaking 1 to give a shiny effect
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isQuestItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (!meta.hasDisplayName()) return false;

        return meta.getDisplayName().contains("CosmoQuest Scroll");
    }

    private static ChatColor getRarityColor(Rarity rarity) {
        switch (rarity) {
            case METEORITE: return ChatColor.GRAY;
            case ASTEROID: return ChatColor.GREEN;
            case COMET: return ChatColor.BLUE;
            case PLANETARY: return ChatColor.DARK_PURPLE;
            case GALACTIC: return ChatColor.GOLD;
            default: return ChatColor.WHITE;
        }
    }
}

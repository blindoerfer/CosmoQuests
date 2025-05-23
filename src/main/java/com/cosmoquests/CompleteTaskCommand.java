package com.cosmoquests;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import java.util.List;

public class CompleteTaskCommand implements CommandExecutor {

    private final QuestManager questManager;
    private final QuestListener questListener;

    public CompleteTaskCommand(QuestManager questManager, QuestListener questListener) {
        this.questManager = questManager;
        this.questListener = questListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /completetask <player> <task#|all>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType() != org.bukkit.Material.COOKIE || !item.hasItemMeta()) {
            sender.sendMessage(ChatColor.RED + "That player is not holding a CosmoQuest cookie.");
            return true;
        }

        String questId = item.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(CosmoQuestsPlugin.getPlugin(CosmoQuestsPlugin.class), "quest_id"), PersistentDataType.STRING);
        if (questId == null) {
            sender.sendMessage(ChatColor.RED + "Invalid quest cookie.");
            return true;
        }

        Quest quest = questManager.getQuest(UUID.fromString(questId), target.getUniqueId());
        if (quest == null) {
            sender.sendMessage(ChatColor.RED + "No active quest found for " + target.getName());
            return true;
        }

        if (args[1].equalsIgnoreCase("all")) {
            for (Task task : quest.getTasks()) {
                task.setProgress(task.getAmount());
            }
            sender.sendMessage(ChatColor.GREEN + "All tasks completed for " + target.getName());
        } else {
            try {
                int taskIndex = Integer.parseInt(args[1]) - 1;
                if (taskIndex < 0 || taskIndex >= quest.getTasks().size()) {
                    sender.sendMessage(ChatColor.RED + "Invalid task number.");
                    return true;
                }
                Task task = quest.getTasks().get(taskIndex);
                task.setProgress(task.getAmount());
                sender.sendMessage(ChatColor.GREEN + "Task " + (taskIndex + 1) + " completed for " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid task number.");
                return true;
            }
        }

        questListener.updateQuestCookie(target, item, quest);
        questListener.checkAndRewardCompletedCookiesAnywhere(target, quest);
        return true;
    }

    
}

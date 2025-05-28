package com.cosmoquests;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QuestCommand implements CommandExecutor {
    private final QuestManager questManager;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Generate quest and give quest item
        Quest quest = questManager.generateQuest();
        ItemStack questItem = QuestItemFactory.createQuestItem(quest);

        player.getInventory().addItem(questItem);
        player.sendMessage(ChatColor.AQUA + "A new CosmoQuest has been assigned to you! Check your inventory for your quest scroll.");

        return true;
    }
}

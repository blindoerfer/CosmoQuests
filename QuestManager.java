package com.cosmoquests;

import org.bukkit.plugin.java.JavaPlugin;

public class CosmoQuestsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CosmoQuests has launched into orbit!");

        // Register commands
        this.getCommand("cosmoquest").setExecutor(new QuestCommand());

        // Register event listeners
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("CosmoQuests re-entering atmosphere...");
    }
} 

// QuestCommand.java
package com.cosmoquests;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        // Here you'd generate a quest and give it to the player
        player.sendMessage(ChatColor.AQUA + "A new CosmoQuest has been assigned to you!");

        // TODO: Generate quest and track it

        return true;
    }
}

// Rarity.java
package com.cosmoquests;

public enum Rarity {
    METEORITE,
    ASTEROID,
    COMET,
    PLANETARY,
    GALACTIC
}

// TaskType.java
package com.cosmoquests;

public enum TaskType {
    MINE,
    KILL,
    CRAFT,
    COLLECT,
    EXPLORE
}

// Task.java
package com.cosmoquests;

public class Task {
    private final TaskType type;
    private final String target;
    private final int amount;
    private int progress;

    public Task(TaskType type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.progress = 0;
    }

    public boolean isComplete() {
        return progress >= amount;
    }

    public void incrementProgress(int value) {
        this.progress += value;
    }

    public TaskType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public int getProgress() { return progress; }
}

// Quest.java
package com.cosmoquests;

import java.util.List;
import java.util.UUID;

public class Quest {
    private final UUID playerId;
    private final Rarity rarity;
    private final List<Task> tasks;

    public Quest(UUID playerId, Rarity rarity, List<Task> tasks) {
        this.playerId = playerId;
        this.rarity = rarity;
        this.tasks = tasks;
    }

    public boolean isComplete() {
        return tasks.stream().allMatch(Task::isComplete);
    }

    public UUID getPlayerId() { return playerId; }
    public Rarity getRarity() { return rarity; }
    public List<Task> getTasks() { return tasks; }
}

// QuestManager.java
package com.cosmoquests;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class QuestManager {

    private final Map<UUID, Quest> activeQuests = new HashMap<>();

    public Quest generateQuest(UUID playerId) {
        Rarity rarity = getRandomRarity();
        int taskCount = ThreadLocalRandom.current().nextInt(8, 16);
        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            TaskType type = TaskType.values()[ThreadLocalRandom.current().nextInt(TaskType.values().length)];
            String target = getRandomTargetFor(type);
            int amount = getRandomAmountFor(type, rarity);
            tasks.add(new Task(type, target, amount));
        }

        Quest quest = new Quest(playerId, rarity, tasks);
        activeQuests.put(playerId, quest);
        return quest;
    }

    public Quest getQuest(UUID playerId) {
        return activeQuests.get(playerId);
    }

    private Rarity getRandomRarity() {
        Rarity[] rarities = Rarity.values();
        return rarities[ThreadLocalRandom.current().nextInt(rarities.length)];
    }

    private String getRandomTargetFor(TaskType type) {
        switch (type) {
            case MINE: return "iron_ore";
            case KILL: return "zombie";
            case CRAFT: return "crafting_table";
            case COLLECT: return "sugar_cane";
            case EXPLORE: return "desert";
            default: return "unknown";
        }
    }

    private int getRandomAmountFor(TaskType type, Rarity rarity) {
        int base = 5;
        int modifier = rarity.ordinal() + 1;
        return base * modifier + ThreadLocalRandom.current().nextInt(1, 6);
    }
}

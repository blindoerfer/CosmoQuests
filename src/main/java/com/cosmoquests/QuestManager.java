package com.cosmoquests;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class QuestManager {
    // Store quests by questId only
    private final Map<UUID, Quest> activeQuests = new HashMap<>();

    public Quest generateQuest() {
        Rarity rarity = getRandomRarity();
        int taskCount = ThreadLocalRandom.current().nextInt(8, 16);
        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            TaskType type = TaskType.values()[ThreadLocalRandom.current().nextInt(TaskType.values().length)];
            String target = getRandomTargetFor(type);
            int amount = getRandomAmountFor(type, rarity);
            tasks.add(new Task(type, target, amount));
        }

        UUID questId = UUID.randomUUID();
        Quest quest = new Quest(questId, rarity, tasks);
        activeQuests.put(questId, quest);
        return quest;
    }

    public Quest getQuest(UUID questId) {
        return activeQuests.get(questId);
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

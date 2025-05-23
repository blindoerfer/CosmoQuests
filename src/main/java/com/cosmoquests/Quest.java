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

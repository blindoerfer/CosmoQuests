package com.cosmoquests;

import java.util.List;
import java.util.UUID;

public class Quest {
    private final UUID id;
    private final Rarity rarity;
    private final List<Task> tasks;

    public Quest(UUID id, Rarity rarity, List<Task> tasks) {
        this.id = id;
        this.rarity = rarity;
        this.tasks = tasks;
    }

    public UUID getId() {
        return id;
    }

    public boolean isComplete() {
        return tasks.stream().allMatch(Task::isComplete);
    }

    public Rarity getRarity() { return rarity; }
    public List<Task> getTasks() { return tasks; }
}

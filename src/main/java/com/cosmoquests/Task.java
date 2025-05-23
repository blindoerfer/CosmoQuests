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

    public int getAmount() {
        return amount;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        if (progress < 0) {
            this.progress = 0;
        } else if (progress > amount) {
            this.progress = amount;
        } else {
            this.progress = progress;
        }
    }

    public void incrementProgress(int increment) {
        setProgress(this.progress + increment);
    }

    public boolean isComplete() {
        return progress >= amount;
    }

    public TaskType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }
}


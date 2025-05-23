package com.cosmoquests;

import org.bukkit.plugin.java.JavaPlugin;

public class CosmoQuestsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CosmoQuests has launched into orbit!");

        QuestManager questManager = new QuestManager();
        QuestListener questListener = new QuestListener(questManager);

        // Register commands
        this.getCommand("cosmoquest").setExecutor(new QuestCommand(questManager));
        this.getCommand("completetask").setExecutor(new CompleteTaskCommand(questManager, questListener));

        // Register event listeners
        getServer().getPluginManager().registerEvents(questListener, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("CosmoQuests re-entering atmosphere...");
    }
}
package com.squeakyllama.playpenChestShops;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlaypenChestShops extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getLogger().info("PlaypenChestShops has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlaypenChestShops has been disabled!");
    }
}

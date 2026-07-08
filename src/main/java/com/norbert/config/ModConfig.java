package com.norbert.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModConfig {
    @SerializedName("minecraft_version")
    private String minecraftVersion;

    @SerializedName("mod_loader")
    private String modLoader;

    @SerializedName("mods_directory")
    private String modsDirectory;

    private List<ModConfigEntry> mods;

    public ModConfig(String minecraftVersion, String modLoader, String modsDirectory, List<ModConfigEntry> mods) {
        this.minecraftVersion = minecraftVersion;
        this.modLoader = modLoader;
        this.modsDirectory = modsDirectory;
        this.mods = mods;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getModLoader() {
        return modLoader;
    }

    public String getModsDirectory() {
        return modsDirectory;
    }

    public List<ModConfigEntry> getMods() {
        return mods;
    }
}

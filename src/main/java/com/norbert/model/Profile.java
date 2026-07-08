package com.norbert.model;

public class Profile {
    private String name;
    private String minecraftVersion;
    private String modLoader;
    private String modsDirectory;

    public Profile() {
    }

    public Profile(String name, String minecraftVersion, String modLoader, String modsDirectory) {
        this.name = name;
        this.minecraftVersion = minecraftVersion;
        this.modLoader = modLoader;
        this.modsDirectory = modsDirectory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public String getModLoader() {
        return modLoader;
    }

    public void setModLoader(String modLoader) {
        this.modLoader = modLoader;
    }

    public String getModsDirectory() {
        return modsDirectory;
    }

    public void setModsDirectory(String modsDirectory) {
        this.modsDirectory = modsDirectory;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}

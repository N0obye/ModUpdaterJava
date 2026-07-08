package com.norbert.config;

import com.google.gson.annotations.SerializedName;
import com.norbert.model.ModInfo;

public class ModConfigEntry {
    private String name;

    @SerializedName("id_or_slug")
    private String idOrSlug;

    private String source;

    @SerializedName("file_pattern")
    private String filePattern;

    public ModConfigEntry(ModInfo modInfo) {
        this.name = modInfo.getName();
        this.idOrSlug = modInfo.getIdOrSlug();
        this.source = modInfo.getSource();
        this.filePattern = modInfo.getFilePattern();
    }

    public String getName() {
        return name;
    }

    public String getIdOrSlug() {
        return idOrSlug;
    }

    public String getSource() {
        return source;
    }

    public String getFilePattern() {
        return filePattern;
    }
}

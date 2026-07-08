package com.norbert.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.norbert.model.Profile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileService {
    private static final String DEFAULT_MINECRAFT_VERSION = "1.21.10";
    private static final String DEFAULT_MOD_LOADER = "fabric";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File profilesFile;

    public ProfileService() {
        File appDirectory = new File(System.getProperty("user.home"),
                "AppData" + File.separator + "Roaming" + File.separator + ".minecraft" + File.separator + "MinecraftModUpdater");
        this.profilesFile = new File(appDirectory, "profiles.json");
    }

    public List<Profile> loadProfiles() {
        if (!profilesFile.isFile()) {
            List<Profile> profiles = new ArrayList<>();
            profiles.add(createDefaultProfile());
            saveProfiles(profiles);
            return profiles;
        }

        try (Reader reader = new FileReader(profilesFile)) {
            Type profileListType = new TypeToken<List<Profile>>() {
            }.getType();
            List<Profile> profiles = gson.fromJson(reader, profileListType);

            if (profiles == null || profiles.isEmpty()) {
                profiles = new ArrayList<>();
                profiles.add(createDefaultProfile());
            }

            return profiles;
        } catch (IOException | RuntimeException e) {
            List<Profile> profiles = new ArrayList<>();
            profiles.add(createDefaultProfile());
            return profiles;
        }
    }

    public void saveProfile(List<Profile> profiles, Profile profile) {
        int existingIndex = findProfileIndex(profiles, profile.getName());

        if (existingIndex >= 0) {
            profiles.set(existingIndex, profile);
        } else {
            profiles.add(profile);
        }

        saveProfiles(profiles);
    }

    public void saveProfiles(List<Profile> profiles) {
        File parentDirectory = profilesFile.getParentFile();
        if (!parentDirectory.isDirectory()) {
            parentDirectory.mkdirs();
        }

        try (Writer writer = new FileWriter(profilesFile)) {
            gson.toJson(profiles, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save profiles: " + e.getMessage(), e);
        }
    }

    public boolean profileExists(List<Profile> profiles, String profileName) {
        return findProfileIndex(profiles, profileName) >= 0;
    }

    public Profile createDefaultProfile() {
        return new Profile(
                "Default 1.21.10 Fabric",
                DEFAULT_MINECRAFT_VERSION,
                DEFAULT_MOD_LOADER,
                getDefaultModsDirectory()
        );
    }

    public String getDefaultModsDirectory() {
        return System.getProperty("user.home")
                + File.separator + "AppData"
                + File.separator + "Roaming"
                + File.separator + ".minecraft"
                + File.separator + "mods";
    }

    private int findProfileIndex(List<Profile> profiles, String profileName) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getName().equalsIgnoreCase(profileName)) {
                return i;
            }
        }

        return -1;
    }
}

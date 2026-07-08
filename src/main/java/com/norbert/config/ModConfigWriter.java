package com.norbert.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.norbert.api.ModrinthClient;
import com.norbert.model.ModInfo;
import com.norbert.model.ModrinthProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ModConfigWriter {
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final ModrinthClient modrinthClient = new ModrinthClient();

    public void writeConfig(File modsDir, List<ModInfo> mods, String minecraftVersion, String modLoader) throws IOException {
        writeConfig(modsDir, mods, minecraftVersion, modLoader, null);
    }

    public void writeConfig(
            File modsDir,
            List<ModInfo> mods,
            String minecraftVersion,
            String modLoader,
            Consumer<Integer> progressCallback
    ) throws IOException {
        if (!modsDir.isDirectory()) {
            throw new IOException("Mods folder not found: " + modsDir.getAbsolutePath());
        }

        enrichModrinthProjectData(mods, progressCallback);

        List<ModConfigEntry> entries = mods.stream()
                .map(mod -> new ModConfigEntry(mod))
                .collect(Collectors.toList());

        String modsDirectory = modsDir.getAbsolutePath().replace(File.separatorChar, '/');

        File configFile = new File(modsDir, "config.json");
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(new ModConfig(minecraftVersion, modLoader, modsDirectory, entries), writer);
        }
    }

    private void enrichModrinthProjectData(List<ModInfo> mods, Consumer<Integer> progressCallback) {
        for (int i = 0; i < mods.size(); i++) {
            ModInfo mod = mods.get(i);
            try {
                modrinthClient.getProjectFromFile(mod.getPath())
                        .ifPresent(project -> applyProjectData(mod, project));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException | RuntimeException ignored) {
                // Keep the metadata/file-name fallback if Modrinth cannot identify this local jar.
            }

            if (progressCallback != null) {
                progressCallback.accept(i + 1);
            }
        }
    }

    private void applyProjectData(ModInfo mod, ModrinthProject project) {
        mod.setIdOrSlug(project.getSlug());

        String title = project.getTitle();
        if (title != null && !title.isEmpty()) {
            mod.setName(title);
        }
    }
}

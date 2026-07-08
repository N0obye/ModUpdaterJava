package com.norbert.service;

import com.norbert.api.ModrinthClient;
import com.norbert.model.ModInfo;
import com.norbert.model.ModUpdateResult;
import com.norbert.model.ModrinthProject;
import com.norbert.model.ModrinthVersion;
import com.norbert.util.VersionComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ModrinthUpdateService implements ModUpdateService {
    private final ModrinthClient modrinthClient = new ModrinthClient();
    private final VersionComparator versionComparator = new VersionComparator();

    @Override
    public List<ModUpdateResult> checkUpdates(
            List<ModInfo> mods,
            String minecraftVersion,
            String modLoader,
            Consumer<Integer> progressCallback) {

        List<ModUpdateResult> results = new ArrayList<>();

        for (int i = 0; i < mods.size(); i++) {

            ModInfo mod = mods.get(i);

            results.add(
                    checkMod(mod, minecraftVersion, modLoader)
            );

            progressCallback.accept(i + 1);
        }

        return results;
    }

    private ModUpdateResult checkMod(ModInfo mod, String minecraftVersion, String modLoader) {
        try {
            Optional<ModrinthVersion> latestVersion = findLatestVersion(mod, minecraftVersion, modLoader);

            if (!latestVersion.isPresent()) {
                return new ModUpdateResult(mod, "-", "Not found on Modrinth");
            }

            ModrinthVersion modrinthVersion = latestVersion.get();
            String latestVersionNumber = modrinthVersion.getVersionNumber();
            return new ModUpdateResult(
                    mod,
                    latestVersionNumber,
                    createStatus(mod, modrinthVersion, minecraftVersion, modLoader),
                    modrinthVersion.getDownloadUrl(),
                    modrinthVersion.getFileName()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ModUpdateResult(mod, "-", "Cancelled");
        } catch (IOException | RuntimeException e) {
            return new ModUpdateResult(mod, "-", "API error");
        }
    }

    private Optional<ModrinthVersion> findLatestVersion(ModInfo mod, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        Optional<ModrinthVersion> latestVersion = findLatestVersionByFileHash(mod, minecraftVersion, modLoader);
        if (latestVersion.isPresent()) {
            return latestVersion;
        }

        Optional<ModrinthProject> project = findProjectByFileHash(mod);
        if (project.isPresent()) {
            applyProjectData(mod, project.get());
        }

        latestVersion = findLatestVersionByProjectIdOrSlug(
                mod.getIdOrSlug(),
                minecraftVersion,
                modLoader
        );
        if (latestVersion.isPresent()) {
            return latestVersion;
        }

        latestVersion = findLatestVersionByProjectIdOrSlug(
                mod.getFilePattern(),
                minecraftVersion,
                modLoader
        );
        if (latestVersion.isPresent()) {
            return latestVersion;
        }

        return findLatestVersionBySearch(mod, minecraftVersion, modLoader);
    }

    private Optional<ModrinthVersion> findLatestVersionByFileHash(ModInfo mod, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        return modrinthClient.getLatestVersionFromFile(mod.getPath(), minecraftVersion, modLoader);
    }

    private Optional<ModrinthProject> findProjectByFileHash(ModInfo mod) throws IOException, InterruptedException {
        return modrinthClient.getProjectFromFile(mod.getPath());
    }

    private void applyProjectData(ModInfo mod, ModrinthProject project) {
        mod.setIdOrSlug(project.getSlug());

        String title = project.getTitle();
        if (title != null && !title.isEmpty()) {
            mod.setName(title);
        }
    }

    private Optional<ModrinthVersion> findLatestVersionByProjectIdOrSlug(
            String projectIdOrSlug,
            String minecraftVersion,
            String modLoader
    ) throws IOException, InterruptedException {
        return modrinthClient.getLatestVersion(
                projectIdOrSlug,
                minecraftVersion,
                modLoader
        );
    }

    private Optional<ModrinthVersion> findLatestVersionBySearch(ModInfo mod, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        Optional<ModrinthVersion> latestVersion = searchAndGetLatestVersion(
                mod.getName(),
                minecraftVersion,
                modLoader
        );

        if (latestVersion.isPresent()) {
            return latestVersion;
        }

        latestVersion = searchAndGetLatestVersion(
                mod.getFilePattern(),
                minecraftVersion,
                modLoader
        );

        if (latestVersion.isPresent()) {
            return latestVersion;
        }

        return searchAndGetLatestVersion(
                mod.getIdOrSlug(),
                minecraftVersion,
                modLoader
        );
    }

    private Optional<ModrinthVersion> searchAndGetLatestVersion(String query, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        if (query == null || query.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<String> slug = modrinthClient.searchProjectSlug(query, minecraftVersion, modLoader);
        if (!slug.isPresent()) {
            return Optional.empty();
        }

        return findLatestVersionByProjectIdOrSlug(
                slug.get(),
                minecraftVersion,
                modLoader
        );
    }

    private String createStatus(ModInfo mod, ModrinthVersion modrinthVersion, String minecraftVersion, String modLoader) {
        String currentVersion = mod.getVersion();
        String latestVersion = modrinthVersion.getVersionNumber();
        String currentFileName = mod.getFileName();
        String latestFileName = modrinthVersion.getFileName();
        if (isSameFileName(currentFileName, latestFileName)) {
            return "Up to date";
        }
        if (versionComparator.isSameVersionFromFileNames(currentFileName, latestFileName, minecraftVersion, modLoader)) {
            return "Up to date";
        }
        if (currentVersion == null || currentVersion.isEmpty() || "Unknown".equals(currentVersion)) {
            return "Local version unknown";
        }
        int comparison = versionComparator.compare(currentVersion, latestVersion, minecraftVersion, modLoader);
        if (comparison < 0) {
            return "Update available";
        }
        if (comparison > 0) {
            return "Local version is newer";
        }
        return "Up to date";
    }

    private boolean isSameFileName(String currentFileName, String latestFileName) {
        return currentFileName != null
                && latestFileName != null
                && currentFileName.equalsIgnoreCase(latestFileName);
    }
}

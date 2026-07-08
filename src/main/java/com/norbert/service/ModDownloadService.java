package com.norbert.service;

import com.norbert.model.ModInfo;
import com.norbert.model.ModUpdateResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class ModDownloadService {
    private static final String USER_AGENT = "Norbi/MinecraftModUpdater/1.0";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public int downloadUpdates(File modsDir,
                               List<ModUpdateResult> updateResults,
                               Consumer<String> statusCallback)
            throws IOException, InterruptedException {
        if (!modsDir.isDirectory()) {
            throw new IOException("Mods folder not found: " + modsDir.getAbsolutePath());
        }

        int downloadedCount = 0;
        for (ModUpdateResult result : updateResults) {

            if (!result.canDownloadUpdate()) {
                continue;
            }

            String modName =
                    result.getModInfo().getName();

            statusCallback.accept(
                    "Downloading " + modName + "..."
            );

            downloadUpdate(modsDir.toPath(), result);

            statusCallback.accept(
                    modName + " downloaded successfully"
            );

            downloadedCount++;
        }
        statusCallback.accept(
                "Download completed. "
                        + downloadedCount
                        + " mods updated."
        );
        return downloadedCount;
    }

    private void downloadUpdate(Path modsDir, ModUpdateResult result) throws IOException, InterruptedException {
        String downloadFileName = sanitizeFileName(result.getDownloadFileName());
        Path targetFile = modsDir.resolve(downloadFileName);
        Path tempFile = modsDir.resolve(downloadFileName + ".download");

        HttpRequest request = HttpRequest.newBuilder(URI.create(result.getDownloadUrl()))
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download error: HTTP " + response.statusCode());
        }

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        backupOldFile(modsDir, result.getModInfo());
        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void backupOldFile(Path modsDir, ModInfo modInfo) throws IOException {
        Path oldFile = modInfo.getPath();
        if (oldFile == null || !Files.exists(oldFile)) {
            return;
        }

        Path backupDir = modsDir.resolve("backup");
        Files.createDirectories(backupDir);

        Path backupFile = backupDir.resolve(oldFile.getFileName().toString());
        if (Files.exists(backupFile)) {
            backupFile = backupDir.resolve(System.currentTimeMillis() + "-" + oldFile.getFileName());
        }

        Files.move(oldFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private String sanitizeFileName(String fileName) {
        return new File(fileName).getName();
    }
}

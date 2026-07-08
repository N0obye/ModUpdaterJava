package com.norbert.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.norbert.model.ModrinthProject;
import com.norbert.model.ModrinthVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

public class ModrinthClient {
    private static final String BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "Norbi/MinecraftModUpdater/1.0";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Optional<ModrinthVersion> getLatestVersion(String projectIdOrSlug, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        URI uri = buildProjectVersionsUri(projectIdOrSlug, minecraftVersion, modLoader);
        String responseBody;

        try {
            responseBody = sendGetRequest(uri);
        } catch (NotFoundException e) {
            return Optional.empty();
        }

        return parseLatestVersionList(responseBody);
    }

    public Optional<ModrinthVersion> getLatestVersionFromFile(Path filePath, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        String sha1 = calculateSha1(filePath);
        URI uri = buildVersionFileUpdateUri(sha1, minecraftVersion, modLoader);
        String responseBody;

        try {
            responseBody = sendGetRequest(uri);
        } catch (NotFoundException e) {
            return Optional.empty();
        }

        return parseVersionObject(responseBody);
    }

    public Optional<String> searchProjectSlug(String query, String minecraftVersion, String modLoader)
            throws IOException, InterruptedException {
        URI uri = buildSearchUri(query, minecraftVersion, modLoader);
        String responseBody;

        try {
            responseBody = sendGetRequest(uri);
        } catch (NotFoundException e) {
            return Optional.empty();
        }

        JsonElement root = JsonParser.parseString(responseBody);
        if (!root.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject searchResult = root.getAsJsonObject();
        if (!searchResult.has("hits") || !searchResult.get("hits").isJsonArray()) {
            return Optional.empty();
        }

        JsonArray hits = searchResult.getAsJsonArray("hits");
        if (hits.size() == 0 || !hits.get(0).isJsonObject()) {
            return Optional.empty();
        }

        String slug = getString(hits.get(0).getAsJsonObject(), "slug");
        return slug == null || slug.isEmpty() ? Optional.empty() : Optional.of(slug);
    }

    public Optional<ModrinthProject> getProjectFromFile(Path filePath) throws IOException, InterruptedException {
        String sha1 = calculateSha1(filePath);
        URI versionUri = buildVersionFileUri(sha1);
        String versionResponseBody;

        try {
            versionResponseBody = sendGetRequest(versionUri);
        } catch (NotFoundException e) {
            return Optional.empty();
        }

        JsonElement versionRoot = JsonParser.parseString(versionResponseBody);
        if (!versionRoot.isJsonObject()) {
            return Optional.empty();
        }

        String projectId = getString(versionRoot.getAsJsonObject(), "project_id");
        if (projectId == null || projectId.isEmpty()) {
            return Optional.empty();
        }

        URI projectUri = buildProjectUri(projectId);
        String projectResponseBody;

        try {
            projectResponseBody = sendGetRequest(projectUri);
        } catch (NotFoundException e) {
            return Optional.empty();
        }

        JsonElement projectRoot = JsonParser.parseString(projectResponseBody);
        if (!projectRoot.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject project = projectRoot.getAsJsonObject();
        String slug = getString(project, "slug");
        String title = getString(project, "title");

        if (slug == null || slug.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ModrinthProject(slug, title));
    }

    private String sendGetRequest(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new NotFoundException();
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Modrinth API error: HTTP " + response.statusCode());
        }

        return response.body();
    }

    private URI buildProjectVersionsUri(String projectIdOrSlug, String minecraftVersion, String modLoader) {
        String encodedProject = encodePath(projectIdOrSlug);
        String loaders = encodeQueryValue("[\"" + modLoader + "\"]");
        String gameVersions = encodeQueryValue("[\"" + minecraftVersion + "\"]");

        return URI.create(BASE_URL
                + "/project/" + encodedProject + "/version"
                + "?loaders=" + loaders
                + "&game_versions=" + gameVersions
                + "&include_changelog=false");
    }

    private URI buildVersionFileUpdateUri(String sha1, String minecraftVersion, String modLoader) {
        String loaders = encodeQueryValue("[\"" + modLoader + "\"]");
        String gameVersions = encodeQueryValue("[\"" + minecraftVersion + "\"]");

        return URI.create(BASE_URL
                + "/version_file/" + sha1 + "/update"
                + "?algorithm=sha1"
                + "&loaders=" + loaders
                + "&game_versions=" + gameVersions);
    }

    private URI buildVersionFileUri(String sha1) {
        return URI.create(BASE_URL
                + "/version_file/" + sha1
                + "?algorithm=sha1");
    }

    private URI buildProjectUri(String projectIdOrSlug) {
        return URI.create(BASE_URL + "/project/" + encodePath(projectIdOrSlug));
    }

    private URI buildSearchUri(String query, String minecraftVersion, String modLoader) {
        String facets = "[[\"project_type:mod\"],[\"categories:" + modLoader + "\"],[\"versions:" + minecraftVersion + "\"]]";

        return URI.create(BASE_URL
                + "/search"
                + "?query=" + encodeQueryValue(query)
                + "&facets=" + encodeQueryValue(facets)
                + "&index=relevance"
                + "&limit=5");
    }

    private Optional<ModrinthVersion> parseLatestVersionList(String responseBody) {
        JsonElement root = JsonParser.parseString(responseBody);
        if (!root.isJsonArray()) {
            return Optional.empty();
        }

        JsonArray versions = root.getAsJsonArray();
        if (versions.size() == 0 || !versions.get(0).isJsonObject()) {
            return Optional.empty();
        }

        return parseVersionObject(versions.get(0).getAsJsonObject());
    }

    private Optional<ModrinthVersion> parseVersionObject(String responseBody) {
        JsonElement root = JsonParser.parseString(responseBody);
        return root.isJsonObject() ? parseVersionObject(root.getAsJsonObject()) : Optional.empty();
    }

    private Optional<ModrinthVersion> parseVersionObject(JsonObject latestVersion) {
        String versionNumber = getString(latestVersion, "version_number");
        String downloadUrl = null;
        String fileName = null;

        if (latestVersion.has("files") && latestVersion.get("files").isJsonArray()) {
            JsonObject primaryFile = findPrimaryFile(latestVersion.getAsJsonArray("files"));
            if (primaryFile != null) {
                downloadUrl = getString(primaryFile, "url");
                fileName = getString(primaryFile, "filename");
            }
        }

        if (versionNumber == null || versionNumber.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ModrinthVersion(versionNumber, downloadUrl, fileName));
    }

    private String calculateSha1(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream inputStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) {
                hash.append(String.format("%02x", b));
            }

            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm is unavailable.", e);
        }
    }

    private JsonObject findPrimaryFile(JsonArray files) {
        if (files.size() == 0) {
            return null;
        }

        for (JsonElement file : files) {
            if (file.isJsonObject()) {
                JsonObject fileObject = file.getAsJsonObject();
                if (fileObject.has("primary") && fileObject.get("primary").getAsBoolean()) {
                    return fileObject;
                }
            }
        }

        JsonElement firstFile = files.get(0);
        return firstFile.isJsonObject() ? firstFile.getAsJsonObject() : null;
    }

    private String getString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }

        return object.get(key).getAsString();
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static class NotFoundException extends IOException {
    }
}

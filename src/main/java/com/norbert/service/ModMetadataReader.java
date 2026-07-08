package com.norbert.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.norbert.model.ModInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModMetadataReader {

    public void readMetadata(ModInfo modInfo) {
        try (JarFile jarFile = new JarFile(modInfo.getPath().toFile())) {
            if (readFabricMetadata(jarFile, modInfo)) {
                return;
            }

            if (readQuiltMetadata(jarFile, modInfo)) {
                return;
            }

            if (readForgeMetadata(jarFile, modInfo)) {
                return;
            }

            readLegacyMetadata(jarFile, modInfo);
        } catch (IOException | RuntimeException ignored) {
            modInfo.setVersion("Unknown");
        }
    }

    private boolean readFabricMetadata(JarFile jarFile, ModInfo modInfo) throws IOException {
        JsonObject json = readJsonObject(jarFile, "fabric.mod.json");
        if (json == null) {
            return false;
        }

        setIfPresent(json, "name", modInfo::setName);
        setIfPresent(json, "id", modInfo::setIdOrSlug);
        setIfPresent(json, "version", modInfo::setVersion);
        return true;
    }

    private boolean readQuiltMetadata(JarFile jarFile, ModInfo modInfo) throws IOException {
        JsonObject json = readJsonObject(jarFile, "quilt.mod.json");
        if (json == null || !json.has("quilt_loader") || !json.get("quilt_loader").isJsonObject()) {
            return false;
        }

        JsonObject loader = json.getAsJsonObject("quilt_loader");
        setIfPresent(loader, "id", modInfo::setIdOrSlug);
        setIfPresent(loader, "version", modInfo::setVersion);

        if (loader.has("metadata") && loader.get("metadata").isJsonObject()) {
            setIfPresent(loader.getAsJsonObject("metadata"), "name", modInfo::setName);
        }

        return true;
    }

    private boolean readForgeMetadata(JarFile jarFile, ModInfo modInfo) throws IOException {
        String toml = readEntry(jarFile, "META-INF/mods.toml");
        if (toml == null) {
            return false;
        }

        setTomlValue(toml, "displayName", modInfo::setName);
        setTomlValue(toml, "modId", modInfo::setIdOrSlug);

        String version = getTomlValue(toml, "version");
        if (version != null && version.contains("${file.jarVersion}")) {
            version = readManifestVersion(jarFile);
        }

        if (version != null && !version.isEmpty()) {
            modInfo.setVersion(version);
        }

        return true;
    }

    private boolean readLegacyMetadata(JarFile jarFile, ModInfo modInfo) throws IOException {
        String jsonText = readEntry(jarFile, "mcmod.info");
        if (jsonText == null) {
            String version = readManifestVersion(jarFile);
            if (version != null) {
                modInfo.setVersion(version);
                return true;
            }
            return false;
        }

        JsonElement root = JsonParser.parseString(jsonText);
        JsonObject modObject = null;

        if (root.isJsonArray()) {
            JsonArray mods = root.getAsJsonArray();
            if (mods.size() > 0 && mods.get(0).isJsonObject()) {
                modObject = mods.get(0).getAsJsonObject();
            }
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("modList") && object.get("modList").isJsonArray()) {
                JsonArray mods = object.getAsJsonArray("modList");
                if (mods.size() > 0 && mods.get(0).isJsonObject()) {
                    modObject = mods.get(0).getAsJsonObject();
                }
            } else {
                modObject = object;
            }
        }

        if (modObject == null) {
            return false;
        }

        setIfPresent(modObject, "name", modInfo::setName);
        setIfPresent(modObject, "modid", modInfo::setIdOrSlug);
        setIfPresent(modObject, "version", modInfo::setVersion);
        return true;
    }

    private JsonObject readJsonObject(JarFile jarFile, String entryName) throws IOException {
        String jsonText = readEntry(jarFile, entryName);
        if (jsonText == null) {
            return null;
        }

        JsonElement json = JsonParser.parseString(jsonText);
        return json.isJsonObject() ? json.getAsJsonObject() : null;
    }

    private String readEntry(JarFile jarFile, String entryName) throws IOException {
        JarEntry entry = jarFile.getJarEntry(entryName);
        if (entry == null) {
            return null;
        }

        try (InputStream inputStream = jarFile.getInputStream(entry);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            return content.toString();
        }
    }

    private String readManifestVersion(JarFile jarFile) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }

        String implementationVersion = manifest.getMainAttributes().getValue("Implementation-Version");
        if (implementationVersion != null && !implementationVersion.isEmpty()) {
            return implementationVersion;
        }

        return manifest.getMainAttributes().getValue("Specification-Version");
    }

    private void setIfPresent(JsonObject json, String key, StringSetter setter) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            String value = json.get(key).getAsString();
            if (!value.isEmpty()) {
                setter.set(value);
            }
        }
    }

    private void setTomlValue(String toml, String key, StringSetter setter) {
        String value = getTomlValue(toml, key);
        if (value != null && !value.isEmpty()) {
            setter.set(value);
        }
    }

    private String getTomlValue(String toml, String key) {
        Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(toml);
        return matcher.find() ? matcher.group(1) : null;
    }

    private interface StringSetter {
        void set(String value);
    }
}

package com.norbert.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?i)v?\\d+(?:\\.\\d+)+(?:[-+._][a-z0-9]+)*");

    public int compare(String currentVersion, String latestVersion) {
        return compare(currentVersion, latestVersion, null, null);
    }

    public int compare(String currentVersion, String latestVersion, String minecraftVersion, String modLoader) {
        String normalizedCurrentVersion = normalizeVersion(currentVersion, minecraftVersion, modLoader);
        String normalizedLatestVersion = normalizeVersion(latestVersion, minecraftVersion, modLoader);

        if (normalizedCurrentVersion.equalsIgnoreCase(normalizedLatestVersion)) {
            return 0;
        }

        if (containsSameNumberGroups(normalizedCurrentVersion, normalizedLatestVersion)) {
            return 0;
        }

        return compareNumberGroups(normalizedCurrentVersion, normalizedLatestVersion);
    }

    public boolean isSameVersionFromFileNames(
            String currentFileName,
            String latestFileName,
            String minecraftVersion,
            String modLoader
    ) {
        String currentVersion = extractVersionFromFileName(currentFileName, minecraftVersion, modLoader);
        String latestVersion = extractVersionFromFileName(latestFileName, minecraftVersion, modLoader);

        return !currentVersion.isEmpty()
                && !latestVersion.isEmpty()
                && compare(currentVersion, latestVersion, minecraftVersion, modLoader) == 0;
    }

    private int compareNumberGroups(String currentVersion, String latestVersion) {
        List<Integer> currentNumbers = extractNumbers(currentVersion);
        List<Integer> latestNumbers = extractNumbers(latestVersion);

        if (currentNumbers.isEmpty() || latestNumbers.isEmpty()) {
            return safeValue(currentVersion).compareToIgnoreCase(safeValue(latestVersion));
        }

        int maxSize = Math.max(currentNumbers.size(), latestNumbers.size());
        for (int i = 0; i < maxSize; i++) {
            int current = i < currentNumbers.size() ? currentNumbers.get(i) : 0;
            int latest = i < latestNumbers.size() ? latestNumbers.get(i) : 0;

            if (current != latest) {
                return Integer.compare(current, latest);
            }
        }

        return 0;
    }

    private String normalizeVersion(String version, String minecraftVersion, String modLoader) {
        String normalized = safeValue(version).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("(?i)\\.jar$", "");

        if (!safeValue(minecraftVersion).isEmpty()) {
            String quotedMinecraftVersion = Pattern.quote(minecraftVersion.toLowerCase(Locale.ROOT));
            normalized = normalized.replaceAll("(?i)(mc|minecraft)[-_+ ]*" + quotedMinecraftVersion, " ");
            normalized = normalized.replaceAll("(?i)" + quotedMinecraftVersion, " ");
        }

        if (!safeValue(modLoader).isEmpty()) {
            normalized = normalized.replaceAll("(?i)\\b" + Pattern.quote(modLoader.toLowerCase(Locale.ROOT)) + "\\b", " ");
        }

        normalized = normalized.replaceAll("(?i)\\b(mc|minecraft|fabric|forge|neoforge|quilt)\\b", " ");
        normalized = normalized.replaceAll("(?i)^v(?=\\d)", "");
        normalized = normalized.replaceAll("[_+\\- ]+", ".");
        normalized = normalized.replaceAll("[^a-z0-9.]", "");
        normalized = normalized.replaceAll("\\.+", ".");
        normalized = normalized.replaceAll("^\\.|\\.$", "");

        return normalized;
    }

    private String extractVersionFromFileName(String fileName, String minecraftVersion, String modLoader) {
        String normalizedFileName = normalizeVersion(fileName, minecraftVersion, modLoader);
        Matcher matcher = VERSION_PATTERN.matcher(normalizedFileName);
        String version = "";

        while (matcher.find()) {
            version = matcher.group();
        }

        return version;
    }

    private boolean containsSameNumberGroups(String currentVersion, String latestVersion) {
        List<Integer> currentNumbers = extractNumbers(currentVersion);
        List<Integer> latestNumbers = extractNumbers(latestVersion);

        return !currentNumbers.isEmpty()
                && currentNumbers.size() == latestNumbers.size()
                && currentNumbers.equals(latestNumbers);
    }

    private List<Integer> extractNumbers(String version) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(safeValue(version));

        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }

        return numbers;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}

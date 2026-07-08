package com.norbert.model;

import java.nio.file.Path;
import java.util.Locale;

public class ModInfo {
    private String name;
    private String version;
    private String fileName;
    private String idOrSlug;
    private String source;
    private String filePattern;
    private Path path;

    public ModInfo(String fileName, Path path) {
        this.fileName = fileName;
        this.filePattern = createFilePattern(fileName);
        this.idOrSlug = createIdOrSlug(filePattern);
        this.name = createDisplayName(idOrSlug);
        this.version = "Unknown";
        this.source = "modrinth";
        this.path = path;
    }

    private String createFilePattern(String fileName) {
        //mod neve kinyerese a file nevebol
        String baseName = fileName.replaceFirst("(?i)\\.jar$", "");
        String[] parts = baseName.split("-");
        StringBuilder pattern = new StringBuilder();

        for (String part : parts) {
            String lowerPart = part.toLowerCase(Locale.ROOT);
            if (lowerPart.matches("v?\\d+(\\.\\d+)*.*") || lowerPart.matches("mc\\d+.*")) {
                break;
            }

            if (pattern.length() > 0) {
                pattern.append("-");
            }
            pattern.append(lowerPart);
        }

        return pattern.length() == 0 ? baseName.toLowerCase(Locale.ROOT) : pattern.toString();
    }

    private String createIdOrSlug(String filePattern) {
        return filePattern
                .replace("-fabric", "")
                .replace("-forge", "")
                .replace("-neoforge", "");
    }

    private String createDisplayName(String idOrSlug) {
        String[] parts = idOrSlug.split("-");
        StringBuilder displayName = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (displayName.length() > 0) {
                displayName.append(" ");
            }
            displayName.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1));
        }

        return displayName.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getIdOrSlug() {
        return idOrSlug;
    }

    public void setIdOrSlug(String idOrSlug) {
        this.idOrSlug = idOrSlug;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}

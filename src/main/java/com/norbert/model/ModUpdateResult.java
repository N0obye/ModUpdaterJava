package com.norbert.model;

public class ModUpdateResult {
    private ModInfo modInfo;
    private String latestVersion;
    private String status;
    private String downloadUrl;
    private String downloadFileName;

    public ModUpdateResult(ModInfo modInfo, String latestVersion, String status) {
        this(modInfo, latestVersion, status, null, null);
    }

    public ModUpdateResult(ModInfo modInfo, String latestVersion, String status, String downloadUrl, String downloadFileName) {
        this.modInfo = modInfo;
        this.latestVersion = latestVersion;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.downloadFileName = downloadFileName;
    }

    public ModInfo getModInfo() {
        return modInfo;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getStatus() {
        return status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public boolean canDownloadUpdate() {
        return "Update available".equals(status)
                && downloadUrl != null
                && !downloadUrl.isEmpty()
                && downloadFileName != null
                && !downloadFileName.isEmpty();
    }
}

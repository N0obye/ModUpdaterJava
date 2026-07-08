package com.norbert.model;

public class ModrinthVersion {
    private String versionNumber;
    private String downloadUrl;
    private String fileName;

    public ModrinthVersion(String versionNumber, String downloadUrl, String fileName) {
        this.versionNumber = versionNumber;
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }
}

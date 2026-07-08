package com.norbert.model;

public class ModrinthProject {
    private final String slug;
    private final String title;

    public ModrinthProject(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }
}

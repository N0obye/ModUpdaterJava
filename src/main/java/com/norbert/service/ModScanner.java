package com.norbert.service;

import com.norbert.model.ModInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModScanner {
    private final ModMetadataReader metadataReader = new ModMetadataReader();

    public List<ModInfo> scanMods(File modsDir) {
        List<ModInfo> mods = new ArrayList<>();

        File[] files = modsDir.listFiles(); //konyvtar tart. lekerese
        if (files == null) {
            return mods;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) { //csak a .jar vegzettsegu fajlok feldolgozasa
                Path path = file.toPath();
                ModInfo modInfo = new ModInfo(file.getName(), path); //itt hozom letre a mod objektumot
                metadataReader.readMetadata(modInfo);
                mods.add(modInfo); //itt hozzaadom a listahoz
            }
        }

        return mods;
    }
}

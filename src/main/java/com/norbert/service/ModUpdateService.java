package com.norbert.service;

import com.norbert.model.ModInfo;
import com.norbert.model.ModUpdateResult;

import java.util.List;
import java.util.function.Consumer;

public interface ModUpdateService {
    List<ModUpdateResult> checkUpdates(
            List<ModInfo> mods,
            String minecraftVersion,
            String modLoader,
            Consumer<Integer> progressCallback
    );
}

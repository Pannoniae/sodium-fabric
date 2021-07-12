package me.jellysquid.mods.sodium.client.world.cloned.palette;

import net.minecraft.util.ObjectIntIdentityMap;

public class ClonedPaletteFallback<K> implements ClonedPalette<K> {
    private final ObjectIntIdentityMap<K> idList;

    public ClonedPaletteFallback(ObjectIntIdentityMap<K> idList) {
        this.idList = idList;
    }

    @Override
    public K get(int id) {
        return this.idList.getByValue(id);
    }
}

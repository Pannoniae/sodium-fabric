package me.jellysquid.mods.sodium.common.util;

/**
 * Global class used to restrict number of translucent rebuilds at a time to help minimize lag.
 */
public class TranslucentPoolUtil {

    private static int translucentRebuilds = 0;

    public synchronized static int getTranslucentRebuilds() {
        return translucentRebuilds;
    }

    public synchronized static void incrementTranslucentRebuilds() {
        translucentRebuilds++;
    }

    public synchronized static void decrementTranslucentRebuilds() {
        if (translucentRebuilds > 0)
            translucentRebuilds--;
    }

    public synchronized static void resetTranslucentRebuilds() {
        translucentRebuilds = 0;
    }
}

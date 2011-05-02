package com.bc.ceres.core.runtime.internal;

import java.util.HashSet;

/**
 * Helper class used to install platform dependent module entries.
 *
 * @author Norman Fomferra
 * @since Ceres 0.12
 */
public class Platform {

    public enum ID {
        aix("aix"),
        hpux("hp ux"),
        linux("linux"),
        macosx("mac os x"),
        solaris("solaris"),
        win("win");

        private final String osNamePattern;

        ID(String osNamePattern) {
            this.osNamePattern = osNamePattern;
        }
    }

    private static final String LIB_DIR = "lib";


    // System properties that indicate architecure of the current Java VM executable
    // (note that we don't want the actual underlying OS architecture)
    private static final String[] VM_PLATFORM_ARCH_KEYS = {
            "sun.arch.data.model",
            "com.ibm.vm.bitmode",
            "os.arch",
    };

    private static final HashSet<String> PLATFORM_DIRS = new HashSet<String>();

    static {
        for (ID platformId : ID.values()) {
            PLATFORM_DIRS.add(getSourcePathPrefix(platformId, 32));
            PLATFORM_DIRS.add(getSourcePathPrefix(platformId, 64));
        }
    }

    private final ID id;
    private final int bitCount;
    private final String sourcePathPrefix;

    public Platform(ID id, int bitCount) {
        this.id = id;
        this.bitCount = bitCount;
        this.sourcePathPrefix = getSourcePathPrefix(id, bitCount);
    }

    public ID getId() {
        return id;
    }

    public int getBitCount() {
        return bitCount;
    }

    public boolean isPlatformDir(String entryName) {
        return entryName.startsWith(sourcePathPrefix);
    }

    public String truncatePlatformDir(String entryName) {
        if (isPlatformDir(entryName)) {
            return LIB_DIR + "/" + entryName.substring(sourcePathPrefix.length());
        } else {
            return entryName;
        }
    }

    public static boolean isAnyPlatformDir(String entryName) {
        for (String platformDir : PLATFORM_DIRS) {
            if (entryName.startsWith(platformDir)) {
                return true;
            }
        }
        return false;
    }

    public static Platform getCurrentPlatform() {
        ID platformId = getCurrentPlatformId();
        if (platformId != null) {
            return new Platform(platformId, getCurrentPlatformBitCount());
        }
        return null;
    }

    static ID getCurrentPlatformId() {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            return getPlatformId(osName);
        } else {
            return null;
        }
    }

    static int getCurrentPlatformBitCount() {
        for (String key : VM_PLATFORM_ARCH_KEYS) {
            String property = System.getProperty(key);
            if (property != null && property.indexOf("64") > 0) {
                return 64;
            }
        }
        return 32;
    }

    static ID getPlatformId(String osName) {
        String osNameLC = osName.toLowerCase();
        for (ID platformId : ID.values()) {
            // todo - better use regex pattern matching here (nf, 02.05.2011)
            if (osNameLC.startsWith(platformId.osNamePattern)) {
                return platformId;
            }
        }
        return null;
    }

    static String getSourcePathPrefix(ID id, int numBits) {
        return String.format("%s/%s%d/", LIB_DIR, id, numBits);
    }

}

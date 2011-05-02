package com.bc.ceres.core.runtime.internal;

/**
 * Helper class providing system-level functions.
 *
 * @author Norman Fomferra
 */
public class SysHelper {

    public enum PlatformId {
        aix,
        hpux,
        linux,
        macosx,
        solaris,
        win,
    }

    // System properties that indicate architecure of the current Java VM executable
    // (note that we don't want the actual underlying OS architecture)
    private static final String VM_PLATFORM_ARCH_KEYS[] = {
            "sun.arch.data.model",
            "com.ibm.vm.bitmode",
            "os.arch",
    };

    static boolean isNativeFileName(String entryName) {
        String ext = getExt(entryName);
        if (ext == null) {
            return false;
        }
        ext = ext.toLowerCase();
        for (Platform platform : PLATFORM_MAPPINGS) {
            if (platform.fileExt.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    static String getExt(String entryName) {
        int dotPos = entryName.lastIndexOf('.');
        if (dotPos > 0) {
            int sepPos = entryName.lastIndexOf('/');
            if (sepPos == -1 || sepPos < dotPos - 1) {
                return entryName.substring(dotPos);
            }
        }
        return null;
    }


    // Important, place only lower case strings here
    private static final Platform[] PLATFORM_MAPPINGS = new Platform[]{
            new Platform(PlatformId.win, "win", ".dll"),
            new Platform(PlatformId.macosx, "mac os x", ".jnilib"),
            new Platform(PlatformId.linux, "linux", ".so"),
            new Platform(PlatformId.solaris, "solaris", ".so"),
            new Platform(PlatformId.aix, "aix", ".so"),
            new Platform(PlatformId.hpux, "hp ux", ".so"),
    };


    public static PlatformId getPlatformId() {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            return toPlatformId(osName);
        } else {
            return null;
        }
    }

    public static int getPlatformBits() {
        for (String key : VM_PLATFORM_ARCH_KEYS) {
            String property = System.getProperty(key);
            if (property != null && property.indexOf("64") > 0) {
                return 64;
            }
        }
        return 32;
    }

    static PlatformId toPlatformId(String osName) {
        String osNameLC = osName.toLowerCase();
        for (Platform platform : PLATFORM_MAPPINGS) {
            // todo - better use regex pattern matching here (nf, 02.05.2011)
            if (osNameLC.startsWith(platform.osNamePattern)) {
                return platform.id;
            }
        }
        return null;
    }

    private static class Platform {
        private final PlatformId id;
        private final String osNamePattern;
        private final String fileExt;

        public Platform(PlatformId id, String osNamePattern, String fileExt) {
            this.id = id;
            this.osNamePattern = osNamePattern;
            this.fileExt = fileExt;
        }
    }

}

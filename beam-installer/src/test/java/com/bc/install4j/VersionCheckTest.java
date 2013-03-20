package com.bc.install4j;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Norman Fomferra
 */
public class VersionCheckTest {

    @Test
    public void testVersionComparison() throws Exception {
        //String intendedVersion = context.getCompilerVariable("sys.version");
        //String existingVersion = context.getVariable("preinstalledVersion").toString();
        int diff = compareVersion("4.11", "4.10.3");

    }

    private Integer compareVersion(String intendedVersion, String existingVersion) {
        if (existingVersion == null) {
            return null;
        }
        existingVersion = existingVersion.substring("VERSION ".length());

        //Util.showMessage("old=" + existingVersion + ", new=" + intendedVersion);

        String[] intendedVersionParts = intendedVersion.split("\\.");
        String[] existingVersionParts = existingVersion.split("\\.");
        int vDiff = 0;
        for (int i = 0; i < Math.min(intendedVersionParts.length, existingVersionParts.length); i++) {
            Pattern p = Pattern.compile("([0-9]*)(.*)");
            Matcher m1 = p.matcher(intendedVersionParts[i]);
            Matcher m2 = p.matcher(existingVersionParts[i]);
            String v1Num = m1.group(0);
            String v1Qual = m1.group(1);
            String v2Num = m2.group(0);
            String v2Qual = m2.group(1);
            //Util.showMessage(v1Num + " " + v1Qual + " " + v2Num + " " + v2Qual);
            vDiff = Integer.parseInt(v1Num) - Integer.parseInt(v2Num);
            if (vDiff == 0) {
                vDiff = v1Qual.isEmpty() ? +1 : v2Qual.isEmpty() ? -1 : v1Qual.compareTo(v2Qual);
            }
            if (vDiff > 0) {
                break;
            }
        }

        if (vDiff == 0) {
            vDiff = intendedVersionParts.length - existingVersionParts.length;
        }

        if (vDiff > 0) {
            //Util.showErrorMessage("Are you crazy?");
            //screen.previous();
        }
        //Util.showMessage("Argh: " + t.getMessage());
        return vDiff;
    }
}

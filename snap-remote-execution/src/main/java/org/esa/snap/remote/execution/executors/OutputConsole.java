package org.esa.snap.remote.execution.executors;

import org.apache.commons.lang.StringUtils;

/**
 * Created by jcoravu on 22/1/2019.
 */
public class OutputConsole {

    public static final String MESSAGE_SEPARATOR = "\n";

    private final StringBuilder normalStreamMessages;
    private final StringBuilder errorStreamMessages;

    public OutputConsole() {
        this.normalStreamMessages = new StringBuilder();
        this.errorStreamMessages = new StringBuilder();
    }

    public void appendNormalMessage(String message) {
        if (this.normalStreamMessages.length() > 0) {
            this.normalStreamMessages.append(MESSAGE_SEPARATOR);
        }
        this.normalStreamMessages.append(message);
    }

    public void appendErrorMessage(String message) {
        if (this.errorStreamMessages.length() > 0) {
            this.errorStreamMessages.append(MESSAGE_SEPARATOR);
        }
        this.errorStreamMessages.append(message);
    }

    public boolean containsIgnoreCaseOnNormalStream(String valuesToCheck[]) {
        if (containsIgnoreCase(this.normalStreamMessages.toString(), valuesToCheck)) {
            return true;
        }
        return false;
    }

    public boolean containsIgnoreCase(String valuesToCheck[]) {
        if (containsIgnoreCase(this.normalStreamMessages.toString(), valuesToCheck) || containsIgnoreCase(this.errorStreamMessages.toString(), valuesToCheck)) {
            return true;
        }
        return false;
    }

    public String getErrorStreamMessages() {
        return errorStreamMessages.toString();
    }

    public String getNormalStreamMessages() {
        return normalStreamMessages.toString();
    }

    private static boolean containsIgnoreCase(String value, String valuesToCheck[]) {
        for (int i=0; i<valuesToCheck.length; i++) {
            if (StringUtils.containsIgnoreCase(value, valuesToCheck[i])) {
                return true;
            }
        }
        return false;
    }
}

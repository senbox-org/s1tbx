package com.bc.ceres.jai.js;

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

public class JsJaiShell {

    public static void main(String args[]) {
        final Global global = Main.getGlobal();
        global.defineProperty("jai", new JsJai(), ScriptableObject.READONLY);
        Main.main(args);
    }
}
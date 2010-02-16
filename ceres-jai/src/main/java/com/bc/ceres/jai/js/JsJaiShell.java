package com.bc.ceres.jai.js;

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

public class JsJaiShell {

    public static void main(String args[]) {
        final Global global = Main.getGlobal();
        global.defineProperty("jai", new JsJai(), ScriptableObject.READONLY);
        System.out.println("JAI shell, (c) 2009, Brockmann Conult GmbH.");
        System.out.println("Start by typing in 'help()' or 'jai.help()'!");
        Main.main(args);
    }
}
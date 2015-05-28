package org.esa.snap.runtime;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;

public class TestApp {
    static String[] args;

    public static void main(String[] args) {
        TestApp.args = args.clone();
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        System.out.println("name = " + mxBean.getName());
        System.out.println("vmName = " + mxBean.getVmName());
        List<String> inputArguments = mxBean.getInputArguments();
        System.out.println("inputArguments = " + Arrays.toString(inputArguments.toArray(new String[inputArguments.size()])));
        System.out.println("args = " + Arrays.toString(args));
        System.out.flush();
    }
}

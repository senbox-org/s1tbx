package org.esa.s1tbx.benchmark;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public abstract class Benchmark {

    private final static boolean DISABLE_BENCHMARKS = true;
    private final static int iterations = 3;
    private final String name;
    private final File resultsFile = new File("e:\\out\\results.json");

    public Benchmark(final String name) {
        this.name = name;
    }

    public void run() throws Exception {
        if(DISABLE_BENCHMARKS) {
            System.out.println("Benchmark " + name + " disabled");
            return;
        }
        SystemUtils.LOG.info("Initial cold start run");
        StopWatch coldStartTimer = new StopWatch();
        this.execute();
        coldStartTimer.stop();
        SystemUtils.LOG.info("Cold start time " + coldStartTimer.getTimeDiffString());
        long totalTime = 0L;

        for(int i = 1; i <= iterations; ++i) {
            SystemUtils.LOG.info("Run " + i + " of " + iterations +" stated");
            StopWatch timer = new StopWatch();
            this.execute();
            timer.stop();
            totalTime += timer.getTimeDiff();
            SystemUtils.LOG.info("Run " + i + " of " + iterations + " end time " + timer.getTimeDiffString());

            SystemUtils.freeAllMemory();
        }

        String avgTime = StopWatch.getTimeString(totalTime / (long)iterations);
        SystemUtils.LOG.warning(name + " average time " + avgTime);

        final JSONObject json = readJSON(resultsFile);
        json.put(name, avgTime);
        writeJSON(json, resultsFile);
    }

    private JSONObject readJSON(final File file) throws Exception {
        if(file.exists()) {
            final BufferedReader streamReader = new BufferedReader(new FileReader(file.getPath()));
            final JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(streamReader);
        }
        return new JSONObject();
    }

    public static void writeJSON(final JSONObject json, final File file) throws Exception {
        try (FileWriter fileWriter = new FileWriter(file)) {
            final File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            fileWriter.write(json.toJSONString());
            fileWriter.flush();
        }
    }

    protected abstract void execute() throws Exception;
}

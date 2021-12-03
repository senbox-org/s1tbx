/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.benchmark;

import org.esa.s1tbx.cloud.json.JSON;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.nio.file.Files;

public abstract class Benchmark {

    private final static boolean DISABLE_BENCHMARKS = true;
    private final static int iterations = 5;
    private final String name;
    private final File resultsFile = new File("e:\\out\\results.json");
    protected File outputFolder;

    public Benchmark(final String name) {
        this.name = FileUtils.createValidFilename(name);
    }

    public void run() throws Exception {
        if(DISABLE_BENCHMARKS) {
            System.out.println("Benchmark " + name + " disabled");
            return;
        }
        SystemUtils.LOG.info("Initial cold start run");
        StopWatch coldStartTimer = new StopWatch();
        outputFolder = Files.createTempDirectory(name).toFile();
        this.execute();
        coldStartTimer.stop();
        SystemUtils.LOG.info("Cold start time " + coldStartTimer.getTimeDiffString());
        SystemUtils.freeAllMemory();
        outputFolder.delete();
        long totalTime = 0L;

        for(int i = 1; i <= iterations; ++i) {
            SystemUtils.LOG.info("Run " + i + " of " + iterations +" stated");
            StopWatch timer = new StopWatch();
            outputFolder = Files.createTempDirectory(name+i).toFile();
            this.execute();
            timer.stop();
            totalTime += timer.getTimeDiff();
            SystemUtils.LOG.info("Run " + i + " of " + iterations + " end time " + timer.getTimeDiffString());

            SystemUtils.freeAllMemory();
            outputFolder.delete();
        }

        String avgTime = StopWatch.getTimeString(totalTime / (long)iterations);
        SystemUtils.LOG.warning(name + " average time " + avgTime);

        final JSONObject json = readJSON(resultsFile);
        json.put(name, avgTime);
        JSON.write(json, resultsFile);
    }

    private JSONObject readJSON(final File file) throws Exception {
        if(file.exists()) {
            return (JSONObject) JSON.loadJSON(file);
        }
        return new JSONObject();
    }

    protected abstract void execute() throws Exception;
}

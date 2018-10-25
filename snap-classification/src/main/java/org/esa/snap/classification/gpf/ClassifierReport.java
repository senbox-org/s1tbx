/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf;

import org.esa.snap.core.util.SystemUtils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Output statistics on the classification
 */
public class ClassifierReport {

    private final String classifierType;
    private final String classifierName;
    private String topClassifier;

    private final List<String> classifierEvaluations = new ArrayList<>();
    private final List<String> featureEvaluations = new ArrayList<>();
    private final List<String> powerSetEvaluations = new ArrayList<>();

    private static final String REPORT_FILE_EXTENSION = ".txt";

    public ClassifierReport(final String classifierType, final String classifierName) {
        this.classifierType = classifierType;
        this.classifierName = classifierName;
    }

    public void addClassifierEvaluation(final String classifierEvaluation) {
        this.classifierEvaluations.add(classifierEvaluation);
    }

    public void addFeatureEvaluation(final String featureEvaluation) {
        this.featureEvaluations.add(featureEvaluation);
    }

    public void addPowerSetEvaluation(final String powerSetEvaluation) {
        this.powerSetEvaluations.add(powerSetEvaluation);
    }


    public void setTopClassifier(final String statement) {
        topClassifier = statement;
    }

    public Path getReportFilePath() throws IOException {
        final Path classifierDir = SystemUtils.getAuxDataPath().
                resolve(BaseClassifier.CLASSIFIER_ROOT_FOLDER).resolve(classifierType);
        if (Files.notExists(classifierDir)) {
            Files.createDirectories(classifierDir);
        }
        return classifierDir.resolve(classifierName + REPORT_FILE_EXTENSION);
    }

    public void writeReport() throws IOException {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(getReportFilePath().toFile()))) {
            printWriter.println(classifierType + " classifier " + classifierName);
            printWriter.println();

            for(String classifierEvaluation : classifierEvaluations) {
                printWriter.println(classifierEvaluation);
            }

            for(String featureEvaluation : featureEvaluations) {
                printWriter.println(featureEvaluation);
            }

            if(powerSetEvaluations.size() > 1) {
                printWriter.println("Power set evaluation:");

                for (String powerSetEvaluation : powerSetEvaluations) {
                    printWriter.println(powerSetEvaluation);
                }

                printWriter.println();
                printWriter.println(topClassifier);
            }
        }
    }

    public void openClassifierReport() throws IOException {
        final File reportFile = getReportFilePath().toFile();
        if (Desktop.isDesktopSupported() && reportFile.exists()) {
            try {
                Desktop.getDesktop().open(reportFile);
            } catch (Exception e) {
                SystemUtils.LOG.warning("Error opening report file " + e.getMessage());
                // do nothing
            }
        }
    }
}

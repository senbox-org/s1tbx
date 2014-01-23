package org.esa.beam.opendap.utils;

import com.bc.io.FileDownloader;
import org.esa.beam.opendap.ui.DownloadProgressBarPM;
import org.esa.beam.util.StringUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.util.EscapeStrings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public class DAPDownloader {

    private static final int MAX_FILENAME_DISPLAY_LENGTH = 15;

    final Map<String, Boolean> dapUris;
    final List<String> fileURIs;
    private final DownloadContext downloadContext;
    private final DownloadProgressBarPM pm;

    public DAPDownloader(Map<String, Boolean> dapUris, List<String> fileURIs, DownloadContext downloadContext,
                         DownloadProgressBarPM pm) {
        this.dapUris = dapUris;
        this.fileURIs = fileURIs;
        this.downloadContext = downloadContext;
        this.pm = pm;
    }

    public void saveProducts(File targetDir) throws IOException {
        if (targetDir != null && targetDir.isDirectory()) {
            downloadFilesWithDapAccess(targetDir);
            downloadFilesWithFileAccess(targetDir);
        } else {
            throw new IOException("No target directory specified.");
        }
    }

    private void downloadFilesWithDapAccess(File targetDir) throws IOException {
        for (Map.Entry<String, Boolean> entry : dapUris.entrySet()) {
            if (pm.isCanceled()) {
                break;
            }
            downloadDapFile(targetDir, entry.getKey(), entry.getValue());
        }
    }

    private void downloadDapFile(File targetDir, String dapURI, boolean isLargeFile) throws IOException {
        String[] uriComponents = dapURI.split("\\?");
        String constraintExpression = "";
        String fileName = dapURI.substring(uriComponents[0].lastIndexOf("/") + 1);
        if (uriComponents.length > 1) {
            constraintExpression = uriComponents[1];
        }
        updateProgressBar(fileName, 0);
        DODSNetcdfFile netcdfFile = new DODSNetcdfFile(dapURI);
        writeNetcdfFile(targetDir, fileName, constraintExpression, netcdfFile, isLargeFile);
    }

    void writeNetcdfFile(File targetDir, String fileName, String constraintExpression, final DODSNetcdfFile sourceNetcdfFile, final boolean isLargeFile) throws IOException {
        final File file = new File(targetDir, fileName);

        if (file.exists() && !downloadContext.mayOverwrite(fileName)) {
            downloadContext.notifyFileDownloaded(file);
            updateProgressBar(fileName, (int) (file.length() / 1024));
            return;
        }

        if (StringUtils.isNullOrEmpty(constraintExpression)) {
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileWriter2 fileWriter = new FileWriter2(sourceNetcdfFile, file.getAbsolutePath(), NetcdfFileWriter.Version.netcdf3, null);
                            fileWriter.write();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                thread.start();
                int downloadedBefore = 0;
                while (thread.isAlive()) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignore) {
                        // ignore
                    }
                    int downloaded = (int) (file.length() / 1024);
                    int delta = downloaded - downloadedBefore;
                    if (delta > 0) {
                        updateProgressBar(fileName, delta);
                        downloadedBefore = downloaded;
                    }
                }
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else {
                    throw e;
                }
            }

            if (!pm.isCanceled()) {
                downloadContext.notifyFileDownloaded(file);
            }
            return;
        }
        /**
         * algorithm:
         *   - get all variableNames vN from constraintExpression
         *   - get all dimensions d from sourceNetcdfFile
         *   - filter dimensions and variables
         *   - in new NetcdfFileWritable: create global attributes, dimensions and variables
         *   - create();
         *   - for all variables in new file:
         *      - get corresponding CE
         *      - array = sourceNetcdfFile.readWithCE();
         *      - variable.setData(array)
         *   - close();
         */

        final List<Variable> variables = sourceNetcdfFile.getVariables();
        final List<String> variableNames = new ArrayList<String>();
        for (Variable variable : variables) {
            variableNames.add(variable.getFullName());
        }
        final List<String> filteredVariables = filterVariables(variableNames, constraintExpression);
        final List<Dimension> filteredDimensions = filterDimensions(filteredVariables, sourceNetcdfFile);

        NetcdfFileWriter targetNetCDF = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, file.getAbsolutePath());
        for (Dimension filteredDimension : filteredDimensions) {
            targetNetCDF.addDimension(null, filteredDimension.getFullName(), filteredDimension.getLength(),
                                      filteredDimension.isShared(), filteredDimension.isUnlimited(),
                                      filteredDimension.isVariableLength());
        }
        for (String filteredVariable : filteredVariables) {
            String varName = EscapeStrings.backslashEscape(filteredVariable, NetcdfFile.reservedSectionSpec);
            final Variable variable = sourceNetcdfFile.findVariable(varName);
            final Variable targetVariable = targetNetCDF.addVariable(null, variable.getFullName(), variable.getDataType(),
                                                                     variable.getDimensions());
            for (Attribute attribute : variable.getAttributes()) {
                targetVariable.addAttribute(attribute);
            }
        }
        for (Attribute attribute : sourceNetcdfFile.getGlobalAttributes()) {
            targetNetCDF.addGroupAttribute(null, attribute);
        }
        targetNetCDF.create();

        for (String filteredVariable : filteredVariables) {
            String varName = EscapeStrings.backslashEscape(filteredVariable, NetcdfFile.reservedSectionSpec);
            final Variable sourceVariable = sourceNetcdfFile.findVariable(varName);
            String ceForVariable = getConstraintExpression(filteredVariable, constraintExpression);
            final Array values = sourceNetcdfFile.readWithCE(sourceVariable, ceForVariable);
            final int[] origin = getOrigin(filteredVariable, constraintExpression,
                                           sourceVariable.getDimensions().size());
            try {
                targetNetCDF.write(sourceVariable, origin, values);
            } catch (InvalidRangeException e) {
                throw new IOException(MessageFormat.format("Unable to download variable ''{0}'' into file ''{1}''.",
                                                           filteredVariable, fileName), e);
            }
        }
        targetNetCDF.close();
        downloadContext.notifyFileDownloaded(file);
    }

    private void updateProgressBar(String fileName, int work) {
        pm.worked(work);
        StringBuilder preMessageBuilder = new StringBuilder(fileName);
        int currentWork = pm.getCurrentWork();
        preMessageBuilder.append(" (")
                .append(downloadContext.getAllDownloadedFilesCount() + 1)
                .append("/")
                .append(downloadContext.getAllFilesCount())
                .append(")");
        if (currentWork != 0) {
            final long currentTime = new GregorianCalendar().getTimeInMillis();
            final long durationInMillis = currentTime - pm.getStartTime();
            double downloadSpeed = getDownloadSpeed(durationInMillis, currentWork);
            char sizeIdentifier = downloadSpeed < 1000 ? 'k' : 'M';
            downloadSpeed = downloadSpeed < 1000 ? downloadSpeed : downloadSpeed / 1024;
            String speedString = OpendapUtils.format(downloadSpeed);
            preMessageBuilder.append(" @ ").append(speedString).append(" ").append(sizeIdentifier).append("B/s");
        }
        int totalWork = pm.getTotalWork();
        double percentage = ((double) currentWork / totalWork) * 100.0;
        String workDone = OpendapUtils.format(currentWork / 1024.0);
        String totalWorkString = OpendapUtils.format(totalWork / 1024.0);
        pm.setPostMessage(workDone + " MB/" + totalWorkString + " MB (" + OpendapUtils.format(percentage) + "%)");
        String preMessageString = preMessageBuilder.toString();
        pm.setTooltip("Downloading " + preMessageString);
        final String shortenedFilename = fileName.substring(0, Math.min(fileName.length(), MAX_FILENAME_DISPLAY_LENGTH));
        pm.setPreMessage("Downloading " + preMessageString.replace(fileName, shortenedFilename + "..."));
    }

    static double getDownloadSpeed(long durationInMillis, int kilobyteCount) {
        return kilobyteCount / (durationInMillis / 1000.0);
    }

    static String getConstraintExpression(String sourceVariable, String constraintExpression) {
        final String[] constraintExpressions = constraintExpression.split(",");
        for (String expression : constraintExpressions) {
            if (expression.startsWith(sourceVariable + "[")) {
                return expression;
            }
        }
        throw new IllegalArgumentException(
                MessageFormat.format("Source variable ''{0}'' must be included in expression ''{1}''.",
                                     sourceVariable, constraintExpression));
    }

    static int[] getOrigin(String variableName, String constraintExpression, int dimensionCount) {
        int[] origin = new int[dimensionCount];
        Arrays.fill(origin, 0);
        if (StringUtils.isNullOrEmpty(constraintExpression)) {
            return origin;
        }
        final String[] variableConstraints = constraintExpression.split(",");
        for (String variableConstraint : variableConstraints) {
            if (variableConstraint.contains(variableName)) {
                if (!variableConstraint.contains("[")) {
                    return origin;
                }

                variableConstraint = variableConstraint.replace("]", "");
                String[] rangeConstraints = variableConstraint.split("\\[");
                if (rangeConstraints.length - 1 > dimensionCount) {
                    throw new IllegalArgumentException(
                            MessageFormat.format("Illegal expression: ''{0}'' for variable ''{1}''.",
                                                 constraintExpression, variableName));
                }
                for (int i = 1; i < rangeConstraints.length; i++) {
                    String rangeConstraint = rangeConstraints[i];
                    String[] rangeComponents = rangeConstraint.split(":");
                    origin[i - 1] = Integer.parseInt(rangeComponents[0]);
                }

            }
        }
        return origin;
    }

    static List<String> filterVariables(List<String> variableNames, String constraintExpression) {
        final List<String> filteredVariables = new ArrayList<String>();
        final List<String> constrainedVariableNames = getVariableNames(constraintExpression);
        if (constrainedVariableNames == null || constrainedVariableNames.isEmpty()) {
            return variableNames;
        }
        for (String variableName : variableNames) {
            if (constrainedVariableNames.contains(variableName)) {
                filteredVariables.add(variableName);
            }
        }
        if (filteredVariables.isEmpty()) {
            return variableNames;
        }
        return filteredVariables;
    }

    static List<Dimension> filterDimensions(List<String> variables, NetcdfFile netcdfFile) {
        final List<Dimension> filteredDimensions = new ArrayList<Dimension>();

        for (String variableName : variables) {
            final Variable variable = netcdfFile.findVariable(variableName);
            for (Dimension dimension : variable.getDimensions()) {
                if (!filteredDimensions.contains(dimension)) {
                    filteredDimensions.add(dimension);
                }
            }
        }

        return filteredDimensions;
    }

    static List<String> getVariableNames(String constraintExpression) {
        if (StringUtils.isNullOrEmpty(constraintExpression)) {
            return null;
        }
        List<String> variableNames = new ArrayList<String>();
        String[] constraints = constraintExpression.split(",");
        for (String constraint : constraints) {
            if (constraint.contains("[")) {
                variableNames.add(constraint.substring(0, constraint.indexOf("[")));
            } else {
                variableNames.add(constraint);
            }
        }
        return variableNames;
    }

    private void downloadFilesWithFileAccess(File targetDir) throws IOException {
        for (String fileURI : fileURIs) {
            if (pm.isCanceled()) {
                break;
            }
            try {
                downloadFile(targetDir, fileURI);
            } catch (Exception e) {
                throw new IOException("Unable to download file '" + fileURI + "'.", e);
            }
        }
    }

    void downloadFile(File targetDir, String fileURI) throws URISyntaxException, IOException {
        final URL fileUrl = new URI(fileURI).toURL();
        updateProgressBar(fileUrl.getFile(), 0);
        final File file = FileDownloader.downloadFile(fileUrl, targetDir, null);
        downloadContext.notifyFileDownloaded(file);
    }

    public interface DownloadContext {

        int getAllFilesCount();

        int getAllDownloadedFilesCount();

        void notifyFileDownloaded(File downloadedFile);

        boolean mayOverwrite(String filename);
    }
}

package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.esa.beam.dataio.ExpectedContent;
import org.esa.beam.dataio.ExpectedDataset;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;

/**
 * @author Marco Peters
 */
public class CreateExpectedJsonCodeCommand extends ExecCommand {

    public static final String LF = System.getProperty("line.separator");
    private final Clipboard clipboard;


    public CreateExpectedJsonCodeCommand() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard());
    }

    CreateExpectedJsonCodeCommand(Clipboard clipboard) {
        if (clipboard == null) {
            final String msg = "Clipboard instance is <null>. Not able to create JSON code.";
            BeamLogManager.getSystemLogger().severe(msg);
            VisatApp.getApp().showErrorDialog(msg);
        }
        this.clipboard = clipboard;
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        if (VisatApp.getApp().getSelectedProduct() != null) {
            run(VisatApp.getApp().getSelectedProduct());
        }
    }

    private void run(final Product product) {
        final Window window = VisatApp.getApp().getApplicationWindow();
        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(window, "Extracting Expected Content") {
            @Override
            protected Void doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Collecting data...", ProgressMonitor.UNKNOWN);
                try {
                    fillClipboardWithJsonCode(product, new Random(123546));
                } catch (Exception e) {
                    e.printStackTrace();
                    BeamLogManager.getSystemLogger().severe(e.getMessage());
                    VisatApp.getApp().showErrorDialog(e.getMessage());
                } finally {
                    pm.done();
                }
                return null;
            }
        };
        worker.executeWithBlocking();

    }

    void fillClipboardWithJsonCode(Product product, Random random) throws IOException {
        final String jsonCode = createJsonCode(product, random);
        StringSelection clipboardContent = new StringSelection(jsonCode);
        clipboard.setContents(clipboardContent, clipboardContent);
    }

    String createJsonCode(Product product, Random random) throws IOException {
        final ExpectedContent expectedContent = new ExpectedContent(product, random);
        ExpectedDataset expectedDataset = new ExpectedDataset();
        expectedDataset.setId(generateID(product));
        expectedDataset.setExpectedContent(expectedContent);
        expectedDataset.setDecodeQualification(DecodeQualification.INTENDED.name());
        ObjectWriter writer = getConfiguredJsonWriter();
        final StringWriter stringWriter = new StringWriter();
        writer.writeValue(stringWriter, expectedDataset);
        stringWriter.flush();
        return stringWriter.toString();
    }

    String generateID(Product product) {
        String id = product.getName();
        id = id.replace(" ", "_");
        id = id.replace(".", "_");
        return id;
    }

    static ObjectWriter getConfiguredJsonWriter() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        final VisibilityChecker<?> defaultVisibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
        final VisibilityChecker<?> visibilityChecker = defaultVisibilityChecker.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
        mapper.setVisibilityChecker(visibilityChecker);
        final ObjectWriter writer = mapper.writer();
        final MyDefaultPrettyPrinter prettyPrinter = new MyDefaultPrettyPrinter();
        final IdeaLikeIndenter indenter = new IdeaLikeIndenter();
        prettyPrinter.indentArraysWith(indenter);
        prettyPrinter.indentObjectsWith(indenter);
        return writer.with(prettyPrinter);
    }


    private static class IdeaLikeIndenter extends DefaultPrettyPrinter.NopIndenter {

        @Override
        public boolean isInline() {
            return false;
        }

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException {
            jg.writeRaw(LF);
            while (level > 0) {
                jg.writeRaw("    ");
                level--;
            }

        }
    }

    private static class MyDefaultPrettyPrinter implements PrettyPrinter {

        private final DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();

        public void indentArraysWith(DefaultPrettyPrinter.Indenter i) {
            defaultPrettyPrinter.indentArraysWith(i);
        }

        public void indentObjectsWith(DefaultPrettyPrinter.Indenter i) {
            defaultPrettyPrinter.indentObjectsWith(i);
        }

        public void writeRootValueSeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeRootValueSeparator(jg);
        }

        public void writeStartObject(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeStartObject(jg);
        }

        public void beforeObjectEntries(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.beforeObjectEntries(jg);
        }

        public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(": ");
        }

        public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeObjectEntrySeparator(jg);
        }

        public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException {
            defaultPrettyPrinter.writeEndObject(jg, nrOfEntries);
        }

        public void writeStartArray(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeStartArray(jg);
        }

        public void beforeArrayValues(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.beforeArrayValues(jg);
        }

        public void writeArrayValueSeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeArrayValueSeparator(jg);
        }

        public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException {
            defaultPrettyPrinter.writeEndArray(jg, nrOfValues);
        }

    }
}

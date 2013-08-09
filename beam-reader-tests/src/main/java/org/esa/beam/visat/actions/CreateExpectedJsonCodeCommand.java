package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.esa.beam.dataio.ExpectedContent;
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

    private final Clipboard clipboard;
    private static final String LF = System.getProperty("line.separator");


    public CreateExpectedJsonCodeCommand() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard());
    }

    CreateExpectedJsonCodeCommand(Clipboard clipboard) {
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
        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(window, "Extracting expected content.") {
            @Override
            protected Void doInBackground(ProgressMonitor progressMonitor) throws Exception {
                fillClipboardWithJsonCode(product);
                return null;
            }
        };
        worker.executeWithBlocking();

    }

    void fillClipboardWithJsonCode(Product product) throws IOException {
        final String jsonCode = createJsonCode(product);
        if (clipboard != null) {
            StringSelection clipboardContent = new StringSelection(jsonCode);
            clipboard.setContents(clipboardContent, clipboardContent);
        } else {
            BeamLogManager.getSystemLogger().severe("Not able to obtain a clipboard instance");
        }
    }

    String createJsonCode(Product product) throws IOException {
        final ExpectedContent expectedContent = new ExpectedContent(product, new Random(123546));

        ObjectWriter writer = getConfigureJsonWriter();
        final StringWriter stringWriter = new StringWriter();
        writer.writeValue(stringWriter, expectedContent);
        stringWriter.flush();
        return stringWriter.toString();
    }

    private ObjectWriter getConfigureJsonWriter() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        final VisibilityChecker<?> defaultVisibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
        final VisibilityChecker<?> visibilityChecker = defaultVisibilityChecker.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
        mapper.setVisibilityChecker(visibilityChecker);
        final ObjectWriter writer = mapper.writer();
        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        final DefaultPrettyPrinter.NopIndenter ideaLikeIndenter = new DefaultPrettyPrinter.NopIndenter() {

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
        };
        prettyPrinter.indentArraysWith(ideaLikeIndenter);
        prettyPrinter.indentObjectsWith(ideaLikeIndenter);
        return writer.with(prettyPrinter);
    }


}

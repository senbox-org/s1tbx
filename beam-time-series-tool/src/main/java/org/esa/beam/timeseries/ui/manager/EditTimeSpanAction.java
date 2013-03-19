package org.esa.beam.timeseries.ui.manager;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.GridTimeCoding;
import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class EditTimeSpanAction extends AbstractAction {

    private final AbstractTimeSeries timeSeries;

    EditTimeSpanAction(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
        setEnabled(timeSeries != null);
//        putValue(NAME, "[?]"); // todo set name
        URL editTimeSpanIconImageURL = UIUtils.getImageURL("/org/esa/beam/timeseries/ui/icons/timeseries-rangeedit24.png", EditTimeSpanAction.class);
        putValue(LARGE_ICON_KEY, new ImageIcon(editTimeSpanIconImageURL));
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SHORT_DESCRIPTION, "Edit time span");
        putValue("componentName", "EditTimeSpan");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        Window window = null;
        if (source instanceof Component) {
            window = SwingUtilities.getWindowAncestor((Component) source);
        }

        final ModalDialog dialog = new EditTimeSpanDialog(window, timeSeries);
        dialog.show();
    }

    private static class EditTimeSpanDialog extends ModalDialog {

        private final SimpleDateFormat dateFormat;
        private AbstractTimeSeries timeSeries;
        private DateComboBox startTimeBox;
        private DateComboBox endTimeBox;
        private JLabel startTimeLabel;
        private JLabel endTimeLabel;
        private JCheckBox autoAdjustBox;

        private EditTimeSpanDialog(Window window, AbstractTimeSeries timeSeries) {
            super(window, "Edit Time Span", ModalDialog.ID_OK_CANCEL, null);
            dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
            this.timeSeries = timeSeries;
            createUserInterface();
        }

        @Override
        protected void onOK() {
            timeSeries.setAutoAdjustingTimeCoding(autoAdjustBox.isSelected());
            final ProductData.UTC startTime = ProductData.UTC.create(startTimeBox.getDate(), 0);
            final ProductData.UTC endTime = ProductData.UTC.create(endTimeBox.getDate(), 0);
            timeSeries.setTimeCoding(new GridTimeCoding(startTime, endTime));

            super.onOK();
        }

        @Override
        protected boolean verifyUserInput() {
            if (endTimeBox.getCalendar().compareTo(startTimeBox.getCalendar()) < 0) {
                showErrorDialog("End time is before start time.");
                return false;
            }
            return true;
        }

        private void createUserInterface() {
            boolean isAutoAdjustingTimeCoding = timeSeries.isAutoAdjustingTimeCoding();
            final TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setTableFill(TableLayout.Fill.BOTH);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setCellColspan(0, 0, 2);
            JPanel content = new JPanel(tableLayout);
            autoAdjustBox = createAutoAdjustBox(isAutoAdjustingTimeCoding);
            startTimeLabel = new JLabel("Start time:");
            startTimeBox = createDateComboBox();
            final TimeCoding timeCoding = timeSeries.getTimeCoding();
            startTimeBox.setCalendar(timeCoding.getStartTime().getAsCalendar());
            endTimeLabel = new JLabel("End time:");
            endTimeBox = createDateComboBox();
            endTimeBox.setCalendar(timeCoding.getEndTime().getAsCalendar());
            content.add(autoAdjustBox);
            content.add(startTimeLabel);
            content.add(startTimeBox);
            content.add(endTimeLabel);
            content.add(endTimeBox);
            setUiEnabled(!isAutoAdjustingTimeCoding);
            setContent(content);
        }

        private JCheckBox createAutoAdjustBox(boolean autoAdjustingTimeCoding) {
            final JCheckBox autoAdjustBox = new JCheckBox("Auto adjust time information", autoAdjustingTimeCoding);
            autoAdjustBox.addActionListener(new AutoAdjustBoxListener(autoAdjustBox));
            return autoAdjustBox;
        }

        private List<Product> getCompatibleProducts() {
            List<Product> result = new ArrayList<Product>();
            for (ProductLocation productLocation : timeSeries.getProductLocations()) {
                for (Product product : productLocation.getProducts().values()) {
                    for (String variable : timeSeries.getEoVariables()) {
                        if (timeSeries.isProductCompatible(product, variable)) {
                            if (timeSeries.isEoVariableSelected(variable)) {
                                result.add(product);
                            }
                        }
                    }
                }
            }
            return result;
        }

        private ProductData.UTC getMaxEndTime(final ProductData.UTC endTime1, final ProductData.UTC endTime2) {
            ProductData.UTC endTime;
            if (endTime1.getAsDate().before(endTime2.getAsDate())) {
                endTime = endTime2;
            } else {
                endTime = endTime1;
            }
            return endTime;
        }

        private ProductData.UTC getMinStartTime(final ProductData.UTC startTime1, final ProductData.UTC startTime2) {
            ProductData.UTC startTime;
            if (startTime1.getAsDate().after(startTime2.getAsDate())) {
                startTime = startTime2;
            } else {
                startTime = startTime1;
            }
            return startTime;
        }

        private DateComboBox createDateComboBox() {
            final DateComboBox box = new DateComboBox();
            box.setTimeDisplayed(true);
            box.setFormat(dateFormat);
            box.setShowNoneButton(false);
            box.setShowTodayButton(false);
            box.setShowOKButton(true);
            box.setEditable(false);
            return box;
        }

        private void setUiEnabled(boolean enable) {
            startTimeBox.setEnabled(enable);
            startTimeLabel.setEnabled(enable);
            endTimeBox.setEnabled(enable);
            endTimeLabel.setEnabled(enable);
        }

        private class AutoAdjustBoxListener implements ActionListener {

            private final JCheckBox autoAdjustBox;

            private AutoAdjustBoxListener(JCheckBox autoAdjustBox) {
                this.autoAdjustBox = autoAdjustBox;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = autoAdjustBox.isSelected();
                setUiEnabled(!selected);
                if (!selected) {
                    return;
                }
                ProductData.UTC autoStartTime = null;
                ProductData.UTC autoEndTime = null;
                List<Product> compatibleProducts = getCompatibleProducts();
                for (Product product : compatibleProducts) {
                    TimeCoding varTimeCoding = GridTimeCoding.create(product);
                    if (autoStartTime == null) {
                        TimeCoding tsTimeCoding = timeSeries.getTimeCoding();
                        autoStartTime = tsTimeCoding.getStartTime();
                        autoEndTime = tsTimeCoding.getEndTime();
                    }
                    if (varTimeCoding != null) {
                        autoStartTime = getMinStartTime(autoStartTime,
                                                        varTimeCoding.getStartTime());
                        autoEndTime = getMaxEndTime(autoEndTime, varTimeCoding.getEndTime());
                    }
                }

                if (autoStartTime == null) {
                    try {
                        autoStartTime = ProductData.UTC.parse("1970-01-01", "yyyy-MM-dd");
                        autoEndTime = autoStartTime;
                    } catch (ParseException ignore) {
                    }
                }
                //noinspection ConstantConditions
                startTimeBox.setDate(autoStartTime.getAsDate());
                //noinspection ConstantConditions
                endTimeBox.setDate(autoEndTime.getAsDate());
            }
        }
    }

}

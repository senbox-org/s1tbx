importPackage(org.esa.beam.framework.datamodel);
importPackage(org.esa.beam.framework.dataio);
importPackage(org.esa.beam.visat);
importPackage(javax.swing);
importPackage(java.awt.event);

visat = VisatApp.getApp();

item = new JMenuItem("Say Hello...");
item.addActionListener(new ActionListener() {
    actionPerformed: function(event) {
        visat.showInfoDialog("Hello!", null);
    }
});
visat.getMainFrame().getJMenuBar().getMenu(4).add(item);
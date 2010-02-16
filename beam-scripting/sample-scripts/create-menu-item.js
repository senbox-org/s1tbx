importPackage(javax.swing);
importPackage(java.awt.event);

item = new JMenuItem("Say Hello...");
item.addActionListener(new ActionListener() {
    actionPerformed: function(event) {
        visat.showInfoDialog("Hello!", null);
    }
});
visat.getMainFrame().getJMenuBar().getMenu(4).add(item);
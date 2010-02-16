
var item = new JMenuItem("Weird operator");
item.addActionListener(new ActionListener() {
    actionPerformed: function(event) {
        performOperator();
    }
});
visat.getMainFrame().getJMenuBar().getMenu(4).add(item);


var myopImpl = {
    initialise: function() {
        var source = this.getSourceProduct("source");
        var target = new Product("JavaScriptProduct", "any", source.getSceneRasterWidth(), source.getSceneRasterHeight());
        target.addBand("b1", ProductData.TYPE_FLOAT32);
        target.addBand("b2", ProductData.TYPE_FLOAT32);
        this.setTargetProduct(target);
    },

    computeTile: function(targetBand, targetTile, pm) {
    }
}



function performOperator() {
    var source = visat.getSelectedProduct();
    if (source == null) {
        return;
    }
    visat.showInfoDialog("1", null);
    var op = new Operator(myopImpl);
    visat.showInfoDialog("2", null);
    op.addSourceProuct("source", source);
    visat.showInfoDialog("3", null);
    var target = op.getTargetProduct();
    visat.showInfoDialog("4", null);
    visat.addProduct(target);
    visat.showInfoDialog("5", null);
}


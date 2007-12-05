importPackage(org.esa.beam.framework.datamodel);
importPackage(org.esa.beam.framework.dataio);
importPackage(org.esa.beam.visat);

var visat = VisatApp.getApp();

var listenerImpl = {
    productAdded: function() {
        visat.showInfoDialog("Product added: " + event.getProduct(), null);
    },

    productRemoved: function() {
        visat.showInfoDialog("Product removed: " + event.getProduct(), null);
    }
};

var listener = new ProductManager.ProductManagerListener(listenerImpl);
var pm = visat.getProductManager();
pm.addListener(listener);

//obj = { run: function () { visat.showInfoDialog("Heoho",null); } }
//r = new java.lang.Runnable(obj);
//t = new java.lang.Thread(r);
//t.start();




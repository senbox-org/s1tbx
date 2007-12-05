importPackage(org.esa.beam.framework.datamodel);
importPackage(org.esa.beam.framework.dataio);
importPackage(org.esa.beam.visat);

visat = VisatApp.getApp();
listener = new ProductManager.ProductManagerListener()
{
    productAdded: function() {
        visat.showInfoDialog("Product added: " + event.getProduct(), null);
    }
,

    productRemoved: function() {
        visat.showInfoDialog("Product removed: " + event.getProduct(), null);
    }
}
;
pm = visat.getProductManager();
pm.addListener(listener);

// r = new java.lang.Runnable() {
//     run: function () {
//         visat.showInfoDialog("He-ho!", null);
//     }
// };
// t = new java.lang.Thread(r);
// t.start();

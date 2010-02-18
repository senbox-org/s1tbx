
var listener;
pm = visat.getProductManager();
if (listener != null) {
	pm.removeListener(listener);
}
listener = new ProductManager.Listener() {
    productAdded: function(event) {
        visat.showInfoDialog("Product added: " + event.getProduct(), null);
    },

    productRemoved: function(event) {
        visat.showInfoDialog("Product removed: " + event.getProduct(), null);
    }
};
pm.addListener(listener);


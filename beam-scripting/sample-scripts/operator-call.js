sp = visat.getSelectedProduct();
op = new SubsetOp();
op.setSourceProduct(sp);
op.setBandList(["radiance_1", "radiance_2"]);
tp = op.getTargetProduct();
tp.setName("SUBSET");
visat.getProductManager().addProduct(tp);

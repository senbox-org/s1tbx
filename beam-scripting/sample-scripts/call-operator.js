sp = visat.getSelectedProduct();

op = new SubsetOp();
op.setSourceProduct(sp);
op.setBandList(["radiance_1", "radiance_2", "radiance_3"]);

tp = op.getTargetProduct();
tp.setName(sp.getName() + "_SUBSET");

visat.addProduct(tp);

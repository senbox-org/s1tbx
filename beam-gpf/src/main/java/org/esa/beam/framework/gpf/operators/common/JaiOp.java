package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


@OperatorMetadata(alias = "JAI",
                  description = "Performs a JAI operation on all bands of product.",
                  internal = true)
public class JaiOp extends Operator {

    private static final Interpolation DEFAULT_INTERPOLATION = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    @SourceProduct
    private Product sourceProduct;

    //@Parameter
    private String operationName;

    //@Parameter
    private HashMap<String, Object> operationParameters;

    //@Parameter
    private RenderingHints renderingHints;


    public JaiOp() {
    }

    public JaiOp(Product sourceProduct,
                 String operationName,
                 HashMap<String, Object> operationParameters,
                 RenderingHints renderingHints) {
        this.sourceProduct = sourceProduct;
        this.operationName = operationName;
        this.operationParameters = operationParameters;
        this.renderingHints = renderingHints;
    }

    @Override
    public void initialize() throws OperatorException {
        if (operationName == null) {
            throw new OperatorException(MessageFormat.format("Missing parameter ''{0}''.", operationName));
        }
        if (operationParameters == null) {
            operationParameters = new HashMap<String, Object>(0);
        }
        if (renderingHints == null) {
            renderingHints = new RenderingHints(JAI.KEY_INTERPOLATION, DEFAULT_INTERPOLATION);
        } else if (!renderingHints.containsKey(JAI.KEY_INTERPOLATION)) {
            renderingHints.put(JAI.KEY_INTERPOLATION, DEFAULT_INTERPOLATION);
        }

        final Band[] sourceBands = sourceProduct.getBands();
        if (sourceBands.length == 0) {
            setTargetProduct(new Product("jai", "jai", 0, 0));
            return;
        }

        final RenderedOp[] targetOps = new RenderedOp[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            targetOps[i] = createTargetImage(sourceBands[i]);
        }

        final int width = targetOps[0].getWidth();
        final int height = targetOps[0].getHeight();

        Product targetProduct = new Product("jai", "jai", width, height);
        for (int i = 0; i < sourceBands.length; i++) {
            Band sourceBand = sourceBands[i];
            Band targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), width, height);
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
            targetBand.setImage(targetOps[i]);
            targetProduct.addBand(targetBand);
        }
        setTargetProduct(targetProduct);
    }

    private RenderedOp createTargetImage(Band sourceBand) {
        final ParameterBlockJAI parameterBlock = new ParameterBlockJAI(operationName);
        parameterBlock.addSource(sourceBand.getImage());
        for (Map.Entry<String, Object> parameter : operationParameters.entrySet()) {
            try {
                parameterBlock.setParameter(parameter.getKey(), parameter.getValue());
            } catch (IllegalArgumentException e) {
                throw new OperatorException(MessageFormat.format("Illegal parameter ''{0}'' for JAI operation ''{1}''.",
                                                                 parameter.getKey(), operationName), e);
            }
        }
        try {
            return JAI.create(operationName, parameterBlock, renderingHints);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("Illegal source or parameters for JAI operation ''{0}''.", operationName), e);
        }
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public HashMap<String, Object> getOperationParameters() {
        return operationParameters;
    }

    public void setOperationParameters(HashMap<String, Object> operationParameters) {
        this.operationParameters = operationParameters;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        throw new IllegalStateException(MessageFormat.format("Operator ''{0}'' cannot compute tiles on its own.", getClass().getName()));
    }
}

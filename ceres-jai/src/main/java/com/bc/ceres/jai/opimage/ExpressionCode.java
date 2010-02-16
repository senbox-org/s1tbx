package com.bc.ceres.jai.opimage;

import com.bc.ceres.compiler.Code;

import java.awt.image.RenderedImage;
import java.util.Vector;

public class ExpressionCode extends Code {
    private final Vector<RenderedImage> sources;

    public ExpressionCode(String className, String code, Vector<RenderedImage> sources) {
        super(className, code);
        this.sources = sources;
    }

    public Vector<RenderedImage> getSources() {
        return sources;
    }
}
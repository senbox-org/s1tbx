package com.bc.ceres.figure.support;

import com.bc.ceres.figure.support.FigureStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

public class DefaultFigureStyle implements FigureStyle {
    private Stroke drawStroke;
    private Paint drawPaint;
    private Paint fillPaint;

    public DefaultFigureStyle() {
        this(new BasicStroke(), Color.BLACK, Color.WHITE);
    }

    public DefaultFigureStyle(Stroke drawStroke, Paint drawPaint) {
        this(drawStroke, drawPaint, Color.WHITE);
    }

    public DefaultFigureStyle(Stroke drawStroke, Paint drawPaint, Paint fillPaint) {
        this.drawStroke = drawStroke;
        this.drawPaint = drawPaint;
        this.fillPaint = fillPaint;
    }

    @Override
    public Stroke getDrawStroke() {
        return drawStroke;
    }

    public void setDrawStroke(Stroke drawStroke) {
        this.drawStroke = drawStroke;
    }

    @Override
    public Paint getDrawPaint() {
        return drawPaint;
    }

    public void setDrawPaint(Paint drawPaint) {
        this.drawPaint = drawPaint;
    }

    @Override
    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    @Override
    public FigureStyle clone() {
        try {
            return (FigureStyle) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}

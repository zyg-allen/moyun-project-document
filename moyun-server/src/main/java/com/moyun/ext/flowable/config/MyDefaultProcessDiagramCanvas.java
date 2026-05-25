package com.moyun.ext.flowable.config;

import org.flowable.image.impl.DefaultProcessDiagramCanvas;

import java.awt.*;

/**
 * 自定义流程图画布
 *
 * @author Tony
 * @date 2021-04-03
 */
public class MyDefaultProcessDiagramCanvas extends DefaultProcessDiagramCanvas {

    protected static Color HIGHLIGHT_COLOR = new Color(255, 0, 0);

    public MyDefaultProcessDiagramCanvas(int width, int height, int minX, int minY, String imageType) {
        super(width, height, minX, minY, imageType);
    }

    public MyDefaultProcessDiagramCanvas(int width, int height, int minX, int minY, String imageType, String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {
        super(width, height, minX, minY, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);
    }

    @Override
    public void drawHighLight(int x, int y, int width, int height) {
        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();

        g.setPaint(HIGHLIGHT_COLOR);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x, y, width, height, 20, 20);

        g.setPaint(originalPaint);
        g.setStroke(originalStroke);
    }
}
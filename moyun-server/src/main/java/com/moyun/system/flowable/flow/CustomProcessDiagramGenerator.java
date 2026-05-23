package com.moyun.system.flowable.flow;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;

import java.io.InputStream;
import java.util.List;

/**
 * 自定义流程图生成器
 */
public class CustomProcessDiagramGenerator extends DefaultProcessDiagramGenerator {

    @Override
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
                                       List<String> highLightedFlows, String activityFontName, String labelFontName,
                                       String annotationFontName, ClassLoader customClassLoader, double scaleFactor,
                                       boolean drawSequenceFlowNameWithNoLabelDI) {
        // 使用默认实现
        return super.generateDiagram(bpmnModel, imageType, highLightedActivities, highLightedFlows,
                activityFontName, labelFontName, annotationFontName, customClassLoader, scaleFactor,
                drawSequenceFlowNameWithNoLabelDI);
    }
}
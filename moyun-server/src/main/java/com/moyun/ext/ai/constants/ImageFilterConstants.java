package com.moyun.ext.ai.constants;

/**
 * 图片过滤常量
 * 
 * <p>定义图片过滤相关的常量值</p>
 * 
 * @author laomao
 */
public final class ImageFilterConstants {

    /**
     * 私有构造函数，防止实例化
     */
    private ImageFilterConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ========== 页面区域常量 ==========

    /**
     * 页面顶部区域
     */
    public static final String REGION_HEADER_TOP = "HEADER_TOP";

    /**
     * 页眉区域
     */
    public static final String REGION_HEADER = "HEADER";

    /**
     * 内容中心区域
     */
    public static final String REGION_CONTENT_CENTER = "CONTENT_CENTER";

    /**
     * 内容顶部区域
     */
    public static final String REGION_CONTENT_TOP = "CONTENT_TOP";

    /**
     * 内容底部区域
     */
    public static final String REGION_CONTENT_BOTTOM = "CONTENT_BOTTOM";

    /**
     * 页脚区域
     */
    public static final String REGION_FOOTER = "FOOTER";

    /**
     * 页面底部区域
     */
    public static final String REGION_FOOTER_BOTTOM = "FOOTER_BOTTOM";

    /**
     * 左边距区域
     */
    public static final String REGION_MARGIN_LEFT = "MARGIN_LEFT";

    /**
     * 右边距区域
     */
    public static final String REGION_MARGIN_RIGHT = "MARGIN_RIGHT";

    // ========== 图片类型常量 ==========

    /**
     * 架构图
     */
    public static final String IMAGE_TYPE_ARCHITECTURE = "架构图";

    /**
     * 流程图
     */
    public static final String IMAGE_TYPE_FLOWCHART = "流程图";

    /**
     * 界面截图
     */
    public static final String IMAGE_TYPE_SCREENSHOT = "界面截图";

    /**
     * 数据图表
     */
    public static final String IMAGE_TYPE_CHART = "数据图表";

    /**
     * 示意图
     */
    public static final String IMAGE_TYPE_DIAGRAM = "示意图";

    /**
     * Logo
     */
    public static final String IMAGE_TYPE_LOGO = "Logo";

    /**
     * 装饰图
     */
    public static final String IMAGE_TYPE_DECORATION = "装饰图";

    /**
     * 纯色背景
     */
    public static final String IMAGE_TYPE_BACKGROUND = "纯色背景";

    // ========== 信息价值常量 ==========

    /**
     * 高价值
     */
    public static final String VALUE_HIGH = "高";

    /**
     * 低价值
     */
    public static final String VALUE_LOW = "低";

    // ========== 存储建议常量 ==========

    /**
     * 存储
     */
    public static final String ADVICE_STORE = "存储";

    /**
     * 丢弃
     */
    public static final String ADVICE_DISCARD = "丢弃";

    // ========== 默认阈值常量 ==========

    /**
     * 默认最小尺寸（像素）
     */
    public static final int DEFAULT_MIN_SIZE = 50;

    /**
     * 默认最大长宽比
     */
    public static final double DEFAULT_MAX_ASPECT_RATIO = 15.0;

    /**
     * 默认纯色最大颜色数
     */
    public static final int DEFAULT_MAX_PURE_COLORS = 3;

    /**
     * 默认页眉页脚重复阈值
     */
    public static final int DEFAULT_HEADER_FOOTER_REPEAT_THRESHOLD = 5;

    /**
     * 默认超高频重复阈值
     */
    public static final int DEFAULT_HIGH_REPEAT_THRESHOLD = 10;

    /**
     * 默认最小关键词数量
     */
    public static final int DEFAULT_MIN_KEYWORD_COUNT = 3;

    /**
     * 默认最小描述长度（字符）
     */
    public static final int DEFAULT_MIN_DESCRIPTION_LENGTH = 30;

    /**
     * 默认综合评分阈值
     */
    public static final double DEFAULT_COMPREHENSIVE_SCORE_THRESHOLD = 0.25;

    // ========== 权重系数常量 ==========

    /**
     * 默认位置权重系数
     */
    public static final double DEFAULT_POSITION_WEIGHT = 0.4;

    /**
     * 默认尺寸权重系数
     */
    public static final double DEFAULT_SIZE_WEIGHT = 0.3;

    /**
     * 默认复杂度权重系数
     */
    public static final double DEFAULT_COMPLEXITY_WEIGHT = 0.3;

    // ========== 采样参数常量 ==========

    /**
     * 颜色采样步长分母
     */
    public static final int COLOR_SAMPLE_STEP_DIVISOR = 50;

    /**
     * 边缘检测采样步长分母
     */
    public static final int EDGE_SAMPLE_STEP_DIVISOR = 100;

    /**
     * Hash计算采样网格大小
     */
    public static final int HASH_SAMPLE_GRID_SIZE = 16;

    /**
     * 颜色量化级数（256级降到8级）
     */
    public static final int COLOR_QUANTIZATION_LEVEL = 32;

    /**
     * 边缘检测梯度阈值
     */
    public static final int EDGE_GRADIENT_THRESHOLD = 30;

    /**
     * 最大统计颜色数
     */
    public static final int MAX_COLOR_COUNT = 100;
}

package com.moyun.agent.filter;

import com.moyun.agent.dto.ImageFilterContext;
import com.moyun.agent.dto.ImagePosition;
import com.moyun.agent.stats.ImageFilterStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.*;

/**
 * 图片过滤器
 * 
 * <p>实现第一层规则过滤，通过尺寸、位置、颜色、边缘密度等特征过滤无价值图片</p>
 * 
 * <p>过滤规则包括：</p>
 * <ul>
 *   <li>极端特征过滤：极小尺寸、极端长宽比、纯色图片</li>
 *   <li>位置组合过滤：页眉页脚Logo、边距装饰、超高频重复</li>
 *   <li>内容简单度过滤：小尺寸简单内容、大面积单色</li>
 *   <li>综合评分过滤：基于位置、尺寸、复杂度的加权评分</li>
 * </ul>
 * 
 * @author laomao
 */
@Slf4j
@Component
public class ImageFilter {
    
    /**
     * 图片hash计数器，用于统计重复次数
     */
    private Map<String, Integer> imageHashCount = new HashMap<>();
    
    /**
     * 统计对象
     */
    private ImageFilterStats stats;
    
    /**
     * 重置过滤器状态
     */
    public void reset() {
        imageHashCount.clear();
    }
    
    /**
     * 设置统计对象
     * 
     * @param stats 统计对象
     */
    public void setStats(ImageFilterStats stats) {
        this.stats = stats;
    }
    
    /**
     * 判断图片是否应该处理（进入AI分析）
     * 
     * @param context 图片过滤上下文
     * @return true-应该处理，false-应该过滤
     */
    public boolean shouldProcess(ImageFilterContext context) {
        
        // 第一关：极端特征过滤
        if (!passExtremeCases(context)) {
            return false;
        }
        
        // 第二关：位置+尺寸组合过滤
        if (!passPositionSizeCombo(context)) {
            return false;
        }
        
        // 第三关：内容简单度过滤
        if (!passContentSimplicity(context)) {
            return false;
        }
        
        // 第四关：综合评分过滤
        if (!passComprehensiveScore(context)) {
            return false;
        }
        
        // 通过所有过滤规则
        if (stats != null) {
            stats.setPassedRuleFilter(stats.getPassedRuleFilter() + 1);
        }
        
        return true;
    }
    
    /**
     * 极端特征过滤
     * 
     * <p>过滤明显无价值的极端情况</p>
     * 
     * @param context 过滤上下文
     * @return true-通过，false-过滤
     */
    private boolean passExtremeCases(ImageFilterContext context) {
        
        // 极小图片：宽高都小于50px
        if (context.getWidth() < 50 && context.getHeight() < 50) {
            log.debug("过滤：极小图片 {}x{}", context.getWidth(), context.getHeight());
            if (stats != null) {
                stats.setFilteredByExtremeSize(stats.getFilteredByExtremeSize() + 1);
            }
            return false;
        }
        
        // 极端长宽比：大于15:1，通常是分隔线
        if (context.getAspectRatio() > 15) {
            log.debug("过滤：极端长宽比 {}", context.getAspectRatio());
            if (stats != null) {
                stats.setFilteredByAspectRatio(stats.getFilteredByAspectRatio() + 1);
            }
            return false;
        }
        
        // 纯色图片：颜色种类<=3
        if (context.getUniqueColors() > 0 && context.getUniqueColors() <= 3) {
            log.debug("过滤：纯色图片，颜色数={}", context.getUniqueColors());
            if (stats != null) {
                stats.setFilteredByPureColor(stats.getFilteredByPureColor() + 1);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 位置+尺寸组合过滤
     * 
     * <p>结合位置和尺寸信息，过滤Logo、水印、装饰等</p>
     * 
     * @param context 过滤上下文
     * @return true-通过，false-过滤
     */
    private boolean passPositionSizeCombo(ImageFilterContext context) {
        ImagePosition pos = context.getPosition();
        
        // 页眉页脚Logo：位置在页眉页脚 + 重复5次以上 + 尺寸较小
        if (pos != null && pos.isInHeaderOrFooter() 
            && context.getRepeatCount() >= 5
            && (context.getWidth() < 250 || context.getHeight() < 150)) {
            log.debug("过滤：页眉页脚Logo，重复{}次", context.getRepeatCount());
            if (stats != null) {
                stats.setFilteredByHeaderFooter(stats.getFilteredByHeaderFooter() + 1);
            }
            return false;
        }
        
        // 边距装饰：位置在页面边距 + 尺寸小于100px
        if (pos != null && pos.isInMargin() 
            && (context.getWidth() < 100 || context.getHeight() < 100)) {
            log.debug("过滤：边距装饰");
            if (stats != null) {
                stats.setFilteredByMargin(stats.getFilteredByMargin() + 1);
            }
            return false;
        }
        
        // 超高频重复：重复10次以上，几乎肯定是Logo/水印
        if (context.getRepeatCount() >= 10) {
            log.debug("过滤：超高频重复，重复{}次", context.getRepeatCount());
            if (stats != null) {
                stats.setFilteredByHighRepeat(stats.getFilteredByHighRepeat() + 1);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 内容简单度过滤
     * 
     * <p>通过颜色数量和边缘密度判断内容复杂度，过滤简单图形</p>
     * 
     * @param context 过滤上下文
     * @return true-通过，false-过滤
     */
    private boolean passContentSimplicity(ImageFilterContext context) {
        
        // 小尺寸简单内容：尺寸<100px + 颜色<10种 + 边缘密度<5%
        if ((context.getWidth() < 100 || context.getHeight() < 100)
            && context.getUniqueColors() > 0 && context.getUniqueColors() < 10
            && context.getEdgeDensity() > 0 && context.getEdgeDensity() < 0.05) {
            log.debug("过滤：小尺寸+简单内容，颜色={}, 边缘密度={}", 
                context.getUniqueColors(), context.getEdgeDensity());
            if (stats != null) {
                stats.setFilteredBySimpleContent(stats.getFilteredBySimpleContent() + 1);
            }
            return false;
        }
        
        // 大面积单色：面积>50000px + 颜色<5种
        if (context.getArea() > 50000
            && context.getUniqueColors() > 0 && context.getUniqueColors() < 5) {
            log.debug("过滤：大面积单色，面积={}, 颜色={}", 
                context.getArea(), context.getUniqueColors());
            if (stats != null) {
                stats.setFilteredByLargeMonochrome(stats.getFilteredByLargeMonochrome() + 1);
            }
            return false;
        }
        
        // 中等尺寸但内容极简：尺寸>100px + 颜色<8种 + 边缘密度<3%
        if (context.getWidth() > 100 && context.getHeight() > 100
            && context.getUniqueColors() > 0 && context.getUniqueColors() < 8
            && context.getEdgeDensity() > 0 && context.getEdgeDensity() < 0.03) {
            log.debug("过滤：中等尺寸但内容极简");
            if (stats != null) {
                stats.setFilteredBySimpleContent(stats.getFilteredBySimpleContent() + 1);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 综合评分过滤
     * 
     * <p>综合位置、尺寸、复杂度三个维度计算加权评分</p>
     * <p>评分公式：位置权重×0.4 + 尺寸权重×0.3 + 复杂度权重×0.3</p>
     * 
     * @param context 过滤上下文
     * @return true-通过，false-过滤
     */
    private boolean passComprehensiveScore(ImageFilterContext context) {
        // 计算各维度权重
        double positionWeight = calculatePositionWeight(context.getPosition());
        double sizeWeight = Math.min(1.0, context.getArea() / 100000.0);
        double complexityWeight = calculateComplexityWeight(context);
        
        // 加权计算最终评分
        double finalScore = positionWeight * 0.4 + sizeWeight * 0.3 + complexityWeight * 0.3;
        
        // 评分低于阈值则过滤
        if (finalScore < 0.25) {
            log.debug("过滤：综合评分过低 score={:.2f}, pos={:.2f}, size={:.2f}, complex={:.2f}", 
                finalScore, positionWeight, sizeWeight, complexityWeight);
            if (stats != null) {
                stats.setFilteredByLowScore(stats.getFilteredByLowScore() + 1);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 计算位置权重
     * 
     * <p>根据图片在页面中的位置计算价值权重</p>
     * <p>中心区域权重最高，边缘区域权重最低</p>
     * 
     * @param pos 位置信息
     * @return 位置权重（0.0-1.0）
     */
    private double calculatePositionWeight(ImagePosition pos) {
        if (pos == null) {
            return 0.7; // 位置未知时给予中等权重
        }
        
        float relY = pos.getRelativeY();
        float relX = pos.getRelativeX();
        
        // 垂直位置权重
        double verticalWeight;
        if (relY < 0.05f || relY > 0.95f) {
            verticalWeight = 0.1; // 极端边缘
        } else if (relY < 0.1f || relY > 0.85f) {
            verticalWeight = 0.3; // 页眉页脚
        } else if (relY >= 0.3f && relY <= 0.7f) {
            verticalWeight = 1.0; // 页面中心
        } else {
            verticalWeight = 0.7; // 内容区域
        }
        
        // 水平位置权重
        double horizontalWeight;
        if (relX < 0.05f || relX > 0.95f) {
            horizontalWeight = 0.2; // 边距
        } else if (relX >= 0.2f && relX <= 0.8f) {
            horizontalWeight = 1.0; // 中央区域
        } else {
            horizontalWeight = 0.6; // 偏左或偏右
        }
        
        // 取垂直和水平权重的平均值
        return (verticalWeight + horizontalWeight) / 2;
    }
    
    /**
     * 计算复杂度权重
     * 
     * <p>基于颜色数量和边缘密度计算内容复杂度</p>
     * <p>复杂度越高，图片包含有效信息的可能性越大</p>
     * 
     * @param context 过滤上下文
     * @return 复杂度权重（0.0-1.0）
     */
    private double calculateComplexityWeight(ImageFilterContext context) {
        // 颜色复杂度评分：颜色种类越多分数越高
        double colorScore = 0;
        if (context.getUniqueColors() > 0) {
            colorScore = Math.min(1.0, context.getUniqueColors() / 50.0);
        }
        
        // 边缘复杂度评分：边缘密度越高分数越高
        double edgeScore = 0;
        if (context.getEdgeDensity() > 0) {
            edgeScore = Math.min(1.0, context.getEdgeDensity() * 10);
        }
        
        // 两个评分都为0时，给予中等权重
        if (colorScore == 0 && edgeScore == 0) {
            return 0.5;
        }
        
        // 只有一个评分时，使用该评分
        if (colorScore == 0) return edgeScore;
        if (edgeScore == 0) return colorScore;
        
        // 两个评分都有时，取平均值
        return (colorScore + edgeScore) / 2;
    }
    
    /**
     * 计算图片hash值
     * 
     * <p>使用MD5算法对图片进行采样hash，用于重复检测</p>
     * <p>包含尺寸信息和16x16采样点的颜色信息</p>
     * 
     * @param image 图片对象
     * @return hash字符串
     */
    public String calculateImageHash(BufferedImage image) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // 包含尺寸信息
            md.update((byte) (image.getWidth() >> 8));
            md.update((byte) image.getWidth());
            md.update((byte) (image.getHeight() >> 8));
            md.update((byte) image.getHeight());
            
            // 采样步长：将图片分成16x16网格
            int stepX = Math.max(1, image.getWidth() / 16);
            int stepY = Math.max(1, image.getHeight() / 16);
            
            // 对采样点进行hash
            for (int y = 0; y < image.getHeight(); y += stepY) {
                for (int x = 0; x < image.getWidth(); x += stepX) {
                    int rgb = image.getRGB(x, y);
                    md.update((byte) (rgb >> 16)); // R
                    md.update((byte) (rgb >> 8));  // G
                    md.update((byte) rgb);         // B
                }
            }
            
            // 转换为16进制字符串
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("计算图片hash失败", e);
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 记录图片hash
     * 
     * @param hash 图片hash值
     */
    public void recordImageHash(String hash) {
        imageHashCount.put(hash, imageHashCount.getOrDefault(hash, 0) + 1);
    }
    
    /**
     * 获取图片重复次数
     * 
     * @param hash 图片hash值
     * @return 重复次数
     */
    public int getRepeatCount(String hash) {
        return imageHashCount.getOrDefault(hash, 0);
    }
    
    /**
     * 计算图片唯一颜色数量
     * 
     * <p>使用采样和颜色量化技术统计颜色种类</p>
     * <p>颜色量化：将RGB各通道从256级降到8级，减少噪音影响</p>
     * 
     * @param image 图片对象
     * @return 唯一颜色数量（最大返回100）
     */
    public int calculateUniqueColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        
        // 采样步长：每隔step个像素采样一次
        int step = Math.max(1, Math.max(image.getWidth(), image.getHeight()) / 50);
        
        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int rgb = image.getRGB(x, y);
                
                // 提取RGB分量
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 颜色量化：256级 -> 8级（除以32）
                // 这样可以将相近的颜色归为一类，减少噪音
                int quantized = ((r / 32) << 10) | ((g / 32) << 5) | (b / 32);
                colors.add(quantized);
                
                // 超过100种颜色就不再统计，认为是复杂图片
                if (colors.size() > 100) {
                    return 100;
                }
            }
        }
        
        return colors.size();
    }
    
    /**
     * 计算边缘密度
     * 
     * <p>使用简单的梯度检测算法统计边缘像素占比</p>
     * <p>边缘密度高说明图片内容复杂，可能包含有效信息</p>
     * 
     * @param image 图片对象
     * @return 边缘密度（0.0-1.0）
     */
    public double calculateEdgeDensity(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int edgeCount = 0;
            int totalPixels = 0;
            
            // 采样步长：将图片分成100x100网格
            int step = Math.max(1, Math.max(width, height) / 100);
            
            // 遍历采样点，计算梯度
            for (int y = step; y < height - step; y += step) {
                for (int x = step; x < width - step; x += step) {
                    // 获取中心点和相邻点的灰度值
                    int center = getGrayscale(image.getRGB(x, y));
                    int right = getGrayscale(image.getRGB(x + step, y));
                    int bottom = getGrayscale(image.getRGB(x, y + step));
                    
                    // 计算水平和垂直梯度
                    int gradientX = Math.abs(center - right);
                    int gradientY = Math.abs(center - bottom);
                    
                    // 梯度超过阈值认为是边缘
                    if (gradientX > 30 || gradientY > 30) {
                        edgeCount++;
                    }
                    totalPixels++;
                }
            }
            
            // 返回边缘像素占比
            return totalPixels > 0 ? (double) edgeCount / totalPixels : 0;
        } catch (Exception e) {
            log.warn("计算边缘密度失败", e);
            return 0;
        }
    }
    
    /**
     * 将RGB颜色转换为灰度值
     * 
     * @param rgb RGB颜色值
     * @return 灰度值（0-255）
     */
    private int getGrayscale(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // 简单平均法
        return (r + g + b) / 3;
    }
}

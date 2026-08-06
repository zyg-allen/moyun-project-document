package com.moyun.ext.ai.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 数据库查询优化工具类
 * 
 * <p>提供批处理、分页查询等优化方法，避免一次加载大量数据导致OOM</p>
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>批量处理：自动分批查询和处理数据</li>
 *     <li>流式处理：逐页处理，降低内存占用</li>
 *     <li>安全限制：默认最大查询数量限制</li>
 * </ul>
 * 
 * @author laomao
 * @since 2025-11-30
 */
public final class QueryOptimizer {
    
    /** 默认批处理大小 */
    public static final int DEFAULT_BATCH_SIZE = 1000;
    
    /** 默认最大查询数量 */
    public static final int DEFAULT_MAX_LIMIT = 10000;
    
    private QueryOptimizer() {
        // 工具类禁止实例化
    }
    
    /**
     * 批量处理数据
     * 
     * <p>自动分页查询并处理数据，避免一次加载过多数据</p>
     * <p>适用场景：需要遍历大量数据但不关心返回结果</p>
     * 
     * @param queryFunc 分页查询函数，入参为Page对象
     * @param processor 数据处理器，处理每一批数据
     * @param batchSize 每批处理的数量
     * @param <T> 数据类型
     */
    public static <T> void batchProcess(
            Function<Page<T>, Page<T>> queryFunc,
            Consumer<List<T>> processor,
            int batchSize) {
        
        int pageNum = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            Page<T> page = new Page<>(pageNum, batchSize);
            Page<T> result = queryFunc.apply(page);
            
            List<T> records = result.getRecords();
            if (records != null && !records.isEmpty()) {
                processor.accept(records);
            }
            
            // 检查是否还有更多数据
            hasMore = pageNum < result.getPages();
            pageNum++;
        }
    }
    
    /**
     * 批量查询所有数据
     * 
     * <p>自动分页查询并聚合结果</p>
     * <p>注意：如果数据量很大，建议使用batchProcess流式处理</p>
     * 
     * @param queryFunc 分页查询函数
     * @param maxLimit 最大查询数量限制
     * @param batchSize 每批查询数量
     * @param <T> 数据类型
     * @return 所有数据列表（最多maxLimit条）
     */
    public static <T> List<T> queryAll(
            Function<Page<T>, Page<T>> queryFunc,
            int maxLimit,
            int batchSize) {
        
        List<T> allRecords = new ArrayList<>();
        int pageNum = 1;
        boolean hasMore = true;
        
        while (hasMore && allRecords.size() < maxLimit) {
            Page<T> page = new Page<>(pageNum, batchSize);
            Page<T> result = queryFunc.apply(page);
            
            List<T> records = result.getRecords();
            if (records != null && !records.isEmpty()) {
                // 确保不超过最大限制
                int remainingSpace = maxLimit - allRecords.size();
                if (records.size() > remainingSpace) {
                    allRecords.addAll(records.subList(0, remainingSpace));
                    break;
                } else {
                    allRecords.addAll(records);
                }
            }
            
            // 检查是否还有更多数据
            hasMore = pageNum < result.getPages();
            pageNum++;
        }
        
        return allRecords;
    }
    
    /**
     * 添加安全的LIMIT限制
     * 
     * <p>为LambdaQueryWrapper添加LIMIT，防止查询过多数据</p>
     * 
     * @param wrapper 查询包装器
     * @param limit 限制数量
     * @param <T> 实体类型
     * @return 添加限制后的包装器
     */
    public static <T> LambdaQueryWrapper<T> addLimit(LambdaQueryWrapper<T> wrapper, int limit) {
        if (limit > 0 && limit <= DEFAULT_MAX_LIMIT) {
            wrapper.last("LIMIT " + limit);
        } else if (limit > DEFAULT_MAX_LIMIT) {
            // 超过最大限制，使用默认值
            wrapper.last("LIMIT " + DEFAULT_MAX_LIMIT);
        }
        return wrapper;
    }
    
    /**
     * 创建安全的分页对象
     * 
     * <p>确保分页参数在合理范围内</p>
     * 
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return Page对象
     */
    public static <T> Page<T> createSafePage(long pageNum, long pageSize) {
        // 页码最小为1
        long safePageNum = Math.max(1, pageNum);
        
        // 每页大小限制在1-1000之间
        long safePageSize = Math.min(Math.max(1, pageSize), DEFAULT_BATCH_SIZE);
        
        return new Page<>(safePageNum, safePageSize);
    }
    
    /**
     * 计算总数是否超过阈值
     * 
     * <p>用于判断是否需要分页处理</p>
     * 
     * @param count 总数
     * @param threshold 阈值
     * @return true表示超过阈值
     */
    public static boolean shouldUsePagination(long count, int threshold) {
        return count > threshold;
    }
    
    /**
     * 获取推荐的批处理大小
     * 
     * <p>根据数据总量动态计算合适的批大小</p>
     * 
     * @param totalCount 总数据量
     * @return 推荐的批大小
     */
    public static int getRecommendedBatchSize(long totalCount) {
        if (totalCount <= 100) {
            return (int) totalCount;
        } else if (totalCount <= 1000) {
            return 100;
        } else if (totalCount <= 10000) {
            return 500;
        } else {
            return DEFAULT_BATCH_SIZE;
        }
    }
}

package com.moyun.util.cache;

import com.moyun.common.constant.CacheConstants;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.util.spring.SpringUtils;

import java.util.Collection;
import java.util.List;

public class DictUtils
{
    public static final String SEPARATOR = ",";

    public static void setDictCache(String key, List<SysDictData> dictDatas)
    {
        SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas);
    }

    public static List<SysDictData> getDictCache(String key)
    {
        List<SysDictData> arrayCache = SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
        if (com.moyun.util.string.StringUtils.isNotNull(arrayCache))
        {
            return arrayCache;
        }
        return null;
    }

    public static String getDictLabel(String dictType, String dictValue)
    {
        if (com.moyun.util.string.StringUtils.isEmpty(dictValue))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    public static String getDictValue(String dictType, String dictLabel)
    {
        if (com.moyun.util.string.StringUtils.isEmpty(dictLabel))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (com.moyun.util.string.StringUtils.isNull(datas))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        if (com.moyun.util.string.StringUtils.containsAny(separator, dictValue))
        {
            for (SysDictData dict : datas)
            {
                for (String value : dictValue.split(separator))
                {
                    if (value.equals(dict.getDictValue()))
                    {
                        propertyString.append(dict.getDictLabel()).append(separator);
                        break;
                    }
                }
            }
        }
        else
        {
            for (SysDictData dict : datas)
            {
                if (dictValue.equals(dict.getDictValue()))
                {
                    return dict.getDictLabel();
                }
            }
        }
        return com.moyun.util.string.StringUtils.stripEnd(propertyString.toString(), separator);
    }

    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (com.moyun.util.string.StringUtils.isNull(datas))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        if (com.moyun.util.string.StringUtils.containsAny(separator, dictLabel))
        {
            for (SysDictData dict : datas)
            {
                for (String label : dictLabel.split(separator))
                {
                    if (label.equals(dict.getDictLabel()))
                    {
                        propertyString.append(dict.getDictValue()).append(separator);
                        break;
                    }
                }
            }
        }
        else
        {
            for (SysDictData dict : datas)
            {
                if (dictLabel.equals(dict.getDictLabel()))
                {
                    return dict.getDictValue();
                }
            }
        }
        return com.moyun.util.string.StringUtils.stripEnd(propertyString.toString(), separator);
    }

    public static String getDictValues(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (com.moyun.util.string.StringUtils.isNull(datas))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictValue()).append(SEPARATOR);
        }
        return com.moyun.util.string.StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    public static String getDictLabels(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (com.moyun.util.string.StringUtils.isNull(datas))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictLabel()).append(SEPARATOR);
        }
        return com.moyun.util.string.StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    public static void removeDictCache(String key)
    {
        SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
    }

    public static void setDictTreeCache(String dictType, List<SysDictData> tree)
    {
        SpringUtils.getBean(RedisCache.class).setCacheObject(getDictTreeCacheKey(dictType), tree);
    }

    public static List<SysDictData> getDictTreeCache(String dictType)
    {
        return SpringUtils.getBean(RedisCache.class).getCacheObject(getDictTreeCacheKey(dictType));
    }

    public static void removeDictTreeCache(String dictType)
    {
        SpringUtils.getBean(RedisCache.class).deleteObject(getDictTreeCacheKey(dictType));
    }

    public static void clearDictCache()
    {
        RedisCache redisCache = SpringUtils.getBean(RedisCache.class);
        Collection<String> keys = redisCache.keys(CacheConstants.SYS_DICT_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisCache.deleteObject(keys);
        }
        Collection<String> treeKeys = redisCache.keys(CacheConstants.SYS_DICT_TREE_KEY + "*");
        if (treeKeys != null && !treeKeys.isEmpty()) {
            redisCache.deleteObject(treeKeys);
        }
    }

    public static String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_DICT_KEY + configKey;
    }

    public static String getDictTreeCacheKey(String dictType)
    {
        return CacheConstants.SYS_DICT_TREE_KEY + dictType;
    }
}

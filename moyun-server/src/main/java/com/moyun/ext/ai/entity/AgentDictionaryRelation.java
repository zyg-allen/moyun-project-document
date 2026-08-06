package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体-词典关联实体
 *
 * <p>对应数据库表 agent_dictionary_relation，存储智能体与领域词典的多对多关系</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("agent_dictionary_relation")
public class AgentDictionaryRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 智能体ID
    private Long agentId;

    // 词典ID
    private Long dictionaryId;

    // 是否启用
    private Boolean enabled;

    // 创建时间
    private LocalDateTime createTime;
}

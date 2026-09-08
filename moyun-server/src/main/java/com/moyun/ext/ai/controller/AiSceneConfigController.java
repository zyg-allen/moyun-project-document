package com.moyun.ext.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.ai.service.AiSceneConfigService;
import com.moyun.ext.ai.service.AiSceneResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 场景配置中心 Controller
 *
 * <p>业务场景与 Agent/模型/知识库/工具/工作流的动态绑定管理（版本 + 灰度权重）。</p>
 *
 * @author moyun
 */
@Slf4j
@Tag(name = "AI场景配置中心")
@RestController
@RequestMapping("/cms/ai/scene")
public class AiSceneConfigController {

    @Autowired
    private AiSceneConfigService sceneConfigService;

    @Autowired
    private AiSceneResolver sceneResolver;

    @Operation(summary = "场景配置列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:list')")
    public AjaxResult getList(@RequestParam(required = false) String sceneCode) {
        try {
            List<AiSceneConfig> list = sceneConfigService.list(new LambdaQueryWrapper<AiSceneConfig>()
                    .eq(sceneCode != null && !sceneCode.isBlank(), AiSceneConfig::getSceneCode, sceneCode)
                    .orderByDesc(AiSceneConfig::getSceneCode)
                    .orderByDesc(AiSceneConfig::getCreateTime));
            return AjaxResult.success(new ListResponse<>(list));
        } catch (Exception e) {
            log.error("获取场景配置列表失败", e);
            return AjaxResult.error("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 场景注册表（v11.38）：系统支持的全部场景元数据（代码/名称/核心能力/输入/输出）。
     * 场景代码的唯一权威来源是 AiSceneEnum，本接口供管理页总览与下拉选择使用。
     */
    @Operation(summary = "场景注册表（场景代码/名称/能力/输入/输出）")
    @GetMapping("/registry")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:list')")
    public AjaxResult registry() {
        return AjaxResult.success(AiSceneEnum.registry());
    }

    @Operation(summary = "场景配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:query')")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        try {
            AiSceneConfig config = sceneConfigService.getById(id);
            if (config == null) {
                return AjaxResult.error("记录不存在");
            }
            return AjaxResult.success(config);
        } catch (Exception e) {
            log.error("获取场景配置详情失败 - ID: {}", id, e);
            return AjaxResult.error("获取详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增场景配置")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:create')")
    public AjaxResult create(@RequestBody AiSceneConfig config) {
        try {
            String error = validate(config, null);
            if (error != null) {
                return AjaxResult.error(error);
            }
            if (config.getEnabled() == null) {
                config.setEnabled(true);
            }
            if (config.getWeight() == null) {
                config.setWeight(100);
            }
            if (config.getPriority() == null) {
                config.setPriority(0);
            }
            sceneConfigService.save(config);
            log.info("新增场景配置成功 - ID: {}, sceneCode: {}, version: {}",
                    config.getId(), config.getSceneCode(), config.getVersion());
            return AjaxResult.success("创建成功", config);
        } catch (Exception e) {
            log.error("新增场景配置失败", e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新场景配置")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:update')")
    public AjaxResult update(@RequestBody AiSceneConfig config) {
        try {
            if (config.getId() == null) {
                return AjaxResult.error("ID不能为空");
            }
            String error = validate(config, config.getId());
            if (error != null) {
                return AjaxResult.error(error);
            }
            sceneConfigService.updateById(config);
            log.info("更新场景配置成功 - ID: {}", config.getId());
            return AjaxResult.success("更新成功");
        } catch (Exception e) {
            log.error("更新场景配置失败 - ID: {}", config.getId(), e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除场景配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        try {
            sceneConfigService.removeById(id);
            log.info("删除场景配置成功 - ID: {}", id);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除场景配置失败 - ID: {}", id, e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "测试场景配置（查看解析后的绑定摘要）")
    @PostMapping("/{id}/test")
    @PreAuthorize("@ss.hasPermi('cms:ai:scene:query')")
    public AjaxResult test(@PathVariable("id") Long id) {
        try {
            AiSceneConfig config = sceneConfigService.getById(id);
            if (config == null) {
                return AjaxResult.error("记录不存在");
            }
            AiSceneBinding binding = sceneResolver.bind(config);
            Map<String, Object> summary = new HashMap<>();
            summary.put("sceneCode", config.getSceneCode());
            summary.put("version", config.getVersion());
            summary.put("bindType", binding.hasAgent() ? "agent" : (binding.hasModelOnly() ? "model" : "empty"));
            summary.put("agentId", binding.getAgent() == null ? null : binding.getAgent().getId());
            summary.put("agentName", binding.getAgent() == null ? null : binding.getAgent().getName());
            summary.put("modelConfigId", binding.getModelConfig() == null
                    ? (binding.getAgent() == null ? null : binding.getAgent().getModelConfigId())
                    : binding.getModelConfig().getId());
            summary.put("modelName", binding.getModelConfig() == null
                    ? null : binding.getModelConfig().getModelName());
            summary.put("knowledgeLibraryCount", binding.getKnowledgeLibraryIds().size());
            summary.put("toolCount", binding.getToolIds().size());
            summary.put("workflowId", binding.getWorkflowId());
            summary.put("configJson", binding.getConfigJson());
            return AjaxResult.success(summary);
        } catch (Exception e) {
            log.error("测试场景配置失败 - ID: {}", id, e);
            return AjaxResult.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 基础校验 + 同场景同版本唯一性校验（更新时排除自身）
     */
    private String validate(AiSceneConfig config, Long excludeId) {
        if (config.getSceneCode() == null || config.getSceneCode().isBlank()) {
            return "场景代码不能为空";
        }
        // v11.38：场景代码必须在注册表内（AiSceneEnum），防止随意输入导致绑定永不生效
        AiSceneEnum scene = AiSceneEnum.of(config.getSceneCode());
        if (scene == null) {
            return "未注册的场景代码: " + config.getSceneCode() + "（合法值: "
                    + String.join(" / ", java.util.Arrays.stream(AiSceneEnum.values())
                            .map(AiSceneEnum::getCode).toArray(String[]::new)) + "）";
        }
        // 场景名称以注册表为准，避免同场景多个版本名称不一致
        config.setSceneName(scene.getName());
        if (config.getVersion() == null || config.getVersion().isBlank()) {
            config.setVersion("v1");
        }
        Long duplicated = sceneConfigService.count(new LambdaQueryWrapper<AiSceneConfig>()
                .eq(AiSceneConfig::getSceneCode, config.getSceneCode())
                .eq(AiSceneConfig::getVersion, config.getVersion())
                .ne(excludeId != null, AiSceneConfig::getId, excludeId));
        if (duplicated != null && duplicated > 0) {
            return "同场景下版本号已存在: " + config.getSceneCode() + " / " + config.getVersion();
        }
        return null;
    }
}
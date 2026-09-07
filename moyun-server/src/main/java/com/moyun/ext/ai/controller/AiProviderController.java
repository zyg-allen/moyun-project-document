package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.entity.AiProvider;
import com.moyun.ext.ai.mapper.AiProviderMapper;
import com.moyun.ext.ai.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 提供商注册表 Controller（V11.0.2 配置驱动改造）
 *
 * <p>后台「AI 模块 → 提供商管理」CRUD；新增 OpenAI 兼容提供商（DeepSeek/Moonshot 等）
 * 只需在此添加记录并启用，模型配置即可选择，全链路零代码改动。</p>
 *
 * @author moyun
 */
@Slf4j
@Tag(name = "AI提供商管理")
@RestController
@RequestMapping("/cms/ai/provider")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;
    private final AiProviderMapper providerMapper;

    @Operation(summary = "提供商列表", description = "全部注册的提供商（含禁用）")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:list')")
    public AjaxResult list() {
        List<AiProvider> list = providerService.lambdaQuery()
                .orderByAsc(AiProvider::getSortOrder)
                .list();
        return AjaxResult.success(list);
    }

    @Operation(summary = "启用的提供商", description = "模型配置表单下拉数据源（匿名端点不开放，需登录）")
    @GetMapping("/enabled")
    public AjaxResult enabled() {
        return AjaxResult.success(providerService.listEnabled());
    }

    @Operation(summary = "创建提供商", description = "注册新的模型提供商")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:add')")
    public AjaxResult create(@RequestBody AiProvider provider) {
        if (provider.getCode() == null || provider.getCode().isBlank()
                || provider.getName() == null || provider.getName().isBlank()) {
            return AjaxResult.error("提供商编码与名称不能为空");
        }
        provider.setCode(provider.getCode().trim().toLowerCase());
        if (providerService.getByCode(provider.getCode()) != null) {
            return AjaxResult.error("提供商编码已存在: " + provider.getCode());
        }
        // apiStyle 归一化：留空默认 OpenAI 兼容；非法值直接拒绝（工厂按此分支，脏值会导致走错协议）
        if (provider.getApiStyle() == null || provider.getApiStyle().isBlank()) {
            provider.setApiStyle(AiProvider.STYLE_OPENAI_COMPATIBLE);
        } else if (!AiProvider.STYLE_OPENAI_COMPATIBLE.equals(provider.getApiStyle())
                && !AiProvider.STYLE_OLLAMA_NATIVE.equals(provider.getApiStyle())) {
            return AjaxResult.error("非法的 API 风格: " + provider.getApiStyle()
                    + "（仅支持 openai_compatible / ollama_native）");
        }
        providerService.save(provider);
        log.info("注册AI提供商成功: code={}, apiStyle={}", provider.getCode(), provider.getApiStyle());
        return AjaxResult.success("创建成功", provider);
    }

    @Operation(summary = "更新提供商", description = "更新提供商能力元数据（即时生效，缓存自动失效）")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:edit')")
    public AjaxResult update(@RequestBody AiProvider provider) {
        if (provider.getId() == null) {
            return AjaxResult.error("id 不能为空");
        }
        AiProvider dbProvider = providerService.getById(provider.getId());
        if (dbProvider == null) {
            return AjaxResult.error("提供商不存在");
        }
        // code 是模型配置的逻辑外键，禁止变更（大小写归一化前缀不同会导致注册表缓存键失配）
        if (provider.getCode() != null && !provider.getCode().isBlank()
                && !dbProvider.getCode().equals(provider.getCode().trim().toLowerCase())) {
            return AjaxResult.error("提供商编码不允许修改（已被模型配置引用），如需更换请新建记录");
        }
        provider.setCode(null);
        if (provider.getApiStyle() != null && !AiProvider.STYLE_OPENAI_COMPATIBLE.equals(provider.getApiStyle())
                && !AiProvider.STYLE_OLLAMA_NATIVE.equals(provider.getApiStyle())) {
            return AjaxResult.error("非法的 API 风格: " + provider.getApiStyle());
        }
        providerService.updateById(provider);
        log.info("更新AI提供商: id={}, code={}", provider.getId(), dbProvider.getCode());
        return AjaxResult.success("更新成功");
    }

    @Operation(summary = "删除提供商", description = "物理删除（uk_code 唯一键不允许软删残留；已被模型配置引用时不建议删除，可停用）")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:model-config:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        providerMapper.hardDeleteById(id);
        providerService.evictCache();
        return AjaxResult.success("删除成功");
    }
}

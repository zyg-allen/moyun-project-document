package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerMemo;
import com.moyun.ledger.service.ILedgerMemoService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 门户记账-备忘录控制器（首页待办事项与备忘录页共用）
 *
 * <p>v11.34 增强：事项标题/内容/事项时间/是否提醒/提醒方式/重要程度。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/memos")
public class PortalLedgerMemoController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ILedgerMemoService memoService;

    /** 待办列表（limit>0 时仅取前 N 条，首页展示用） */
    @GetMapping
    public AjaxResult list(@RequestParam(value = "limit", required = false, defaultValue = "0") Integer limit) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerMemo> list = memoService.listMemos(userId, limit == null ? 0 : limit);
        return AjaxResult.success(Map.of("records", list));
    }

    /** 新增待办 */
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        Long id = memoService.createMemo(userId,
                str(body, "title"), str(body, "content"),
                dt(body, "eventTime"), intVal(body, "remindEnabled"),
                str(body, "remindRule"), str(body, "importance"));
        return AjaxResult.success("添加成功", Map.of("id", id));
    }

    /** 更新待办（title/content/提醒设置/done 任意，null 跳过） */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        Integer done = null;
        if (body != null && body.get("done") != null) {
            done = "true".equals(String.valueOf(body.get("done"))) || "1".equals(String.valueOf(body.get("done"))) ? 1 : 0;
        }
        memoService.updateMemo(userId, id,
                str(body, "title"), str(body, "content"),
                dt(body, "eventTime"), intVal(body, "remindEnabled"),
                str(body, "remindRule"), str(body, "importance"), done);
        return AjaxResult.success("已更新");
    }

    /** 切换完成状态 */
    @PostMapping("/{id}/toggle")
    public AjaxResult toggle(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        memoService.toggleDone(userId, id);
        return AjaxResult.success("已更新");
    }

    /** 删除待办 */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        memoService.deleteMemo(userId, id);
        return AjaxResult.success("已删除");
    }

    private String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String v = String.valueOf(body.get(key)).trim();
        return v.isEmpty() ? null : v;
    }

    private Integer intVal(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        if (body.get(key) instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(body.get(key)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime dt(Map<String, Object> body, String key) {
        String v = str(body, key);
        if (v == null) {
            return null;
        }
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }
}
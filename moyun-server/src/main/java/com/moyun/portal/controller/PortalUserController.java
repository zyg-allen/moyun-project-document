package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.UserQuery;
import com.moyun.portal.domain.vo.UserStatsVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户用户", description = "门户用户的增删改查操作接口")
@RestController
@RequestMapping("/portal/user")
@Slf4j
public class PortalUserController extends BaseController {

    @Autowired
    private IPortalUserService portalUserService;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalArticleMapper articleMapper;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Operation(summary = "获取用户列表", description = "根据条件分页查询用户列表")
    @GetMapping("/list")
    public AjaxResult list(UserQuery query) {
        Page<PortalUser> page = PageUtils.buildPage(query);
        Page<PortalUser> resultPage = portalUserService.selectPortalUserPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出用户", description = "导出用户数据到Excel文件")
    @Log(title = "门户用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserQuery query) {
        List<PortalUser> list = portalUserService.selectPortalUserList(query);
        ExcelUtil<PortalUser> util = new ExcelUtil<>(PortalUser.class);
        util.exportExcel(response, list, "门户用户数据");
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "用户ID") @PathVariable Long id) {
        return success(portalUserService.selectPortalUserById(id));
    }

    @Operation(summary = "获取当前登录用户信息", description = "获取当前登录的门户用户信息，未登录返回null")
    @GetMapping("/me")
    public AjaxResult getCurrentUserInfo() {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            // 未登录时返回null，不返回错误，让前端静默处理
            return success(null);
        }
        // 查询最新的用户数据（确保是最新的，避免缓存延迟）
        PortalUser freshUser = portalUserService.selectPortalUserById(currentUser.getId());
        if (freshUser == null) {
            return success(null);
        }
        return success(freshUser);
    }

    @Operation(summary = "更新个人资料", description = "当前登录用户更新自己的个人资料")
    @Log(title = "门户用户-更新资料", businessType = BusinessType.UPDATE)
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody Map<String, Object> params) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        PortalUser user = new PortalUser();
        user.setId(currentUser.getId());

        // 从 map 中读取可能的字段，允许部分更新
        if (params.containsKey("nickname") && params.get("nickname") != null) {
            user.setNickname(String.valueOf(params.get("nickname")));
        }
        if (params.containsKey("username") && params.get("username") != null) {
            user.setUsername(String.valueOf(params.get("username")));
        }
        if (params.containsKey("bio") && params.get("bio") != null) {
            user.setBio(String.valueOf(params.get("bio")));
        }
        if (params.containsKey("email") && params.get("email") != null) {
            user.setEmail(String.valueOf(params.get("email")));
        }
        if (params.containsKey("phone") && params.get("phone") != null) {
            user.setPhone(String.valueOf(params.get("phone")));
        }
        if (params.containsKey("wechat") && params.get("wechat") != null) {
            user.setWechat(String.valueOf(params.get("wechat")));
        }
        if (params.containsKey("position") && params.get("position") != null) {
            user.setPosition(String.valueOf(params.get("position")));
        }
        if (params.containsKey("avatar") && params.get("avatar") != null) {
            user.setAvatar(String.valueOf(params.get("avatar")));
        }
        if (params.containsKey("gender") && params.get("gender") != null) {
            user.setGender(String.valueOf(params.get("gender")));
        }
        if (params.containsKey("birthday") && params.get("birthday") != null) {
            user.setBirthday(String.valueOf(params.get("birthday")));
        }
        if (params.containsKey("location") && params.get("location") != null) {
            user.setLocation(String.valueOf(params.get("location")));
        }
        if (params.containsKey("website") && params.get("website") != null) {
            user.setWebsite(String.valueOf(params.get("website")));
        }
        if (params.containsKey("github") && params.get("github") != null) {
            user.setGithub(String.valueOf(params.get("github")));
        }
        if (params.containsKey("company") && params.get("company") != null) {
            user.setCompany(String.valueOf(params.get("company")));
        }
        if (params.containsKey("school") && params.get("school") != null) {
            user.setSchool(String.valueOf(params.get("school")));
        }
        if (params.containsKey("language") && params.get("language") != null) {
            user.setLanguage(String.valueOf(params.get("language")));
        }
        if (params.containsKey("timezone") && params.get("timezone") != null) {
            user.setTimezone(String.valueOf(params.get("timezone")));
        }
        // 通知设置
        if (params.containsKey("notifyLike")) {
            user.setNotifyLike(Boolean.TRUE.equals(params.get("notifyLike")));
        }
        if (params.containsKey("notifyComment")) {
            user.setNotifyComment(Boolean.TRUE.equals(params.get("notifyComment")));
        }
        if (params.containsKey("notifyFollow")) {
            user.setNotifyFollow(Boolean.TRUE.equals(params.get("notifyFollow")));
        }
        if (params.containsKey("notifySystem")) {
            user.setNotifySystem(Boolean.TRUE.equals(params.get("notifySystem")));
        }
        // 隐私设置
        if (params.containsKey("privacyFollow")) {
            user.setPrivacyFollow(Boolean.TRUE.equals(params.get("privacyFollow")));
        }
        if (params.containsKey("privacyBookmark")) {
            user.setPrivacyBookmark(Boolean.TRUE.equals(params.get("privacyBookmark")));
        }
        if (params.containsKey("privacyEmail")) {
            user.setPrivacyEmail(Boolean.TRUE.equals(params.get("privacyEmail")));
        }
        if (params.containsKey("privacyPhone")) {
            user.setPrivacyPhone(Boolean.TRUE.equals(params.get("privacyPhone")));
        }

        int result = portalUserService.updatePortalUser(user);
        if (result > 0) {
            // 返回更新后的用户数据
            PortalUser updated = portalUserService.selectPortalUserById(currentUser.getId());
            return success(updated);
        }
        return error("更新失败");
    }

    @Operation(summary = "修改密码", description = "当前登录用户修改自己的密码")
    @Log(title = "门户用户-修改密码", businessType = BusinessType.UPDATE)
    @PutMapping("/password")
    public AjaxResult updatePassword(@RequestBody Map<String, String> params) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        String confirmPassword = params.get("confirmPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return error("请输入当前密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return error("请输入新密码");
        }
        if (newPassword.length() < 6) {
            return error("新密码长度不能少于6位");
        }
        if (!newPassword.equals(confirmPassword)) {
            return error("两次输入的密码不一致");
        }

        // 查询当前用户
        PortalUser user = portalUserService.selectPortalUserById(currentUser.getId());
        if (user == null) {
            return error("用户不存在");
        }

        // 验证旧密码
        if (user.getPassword() != null && !user.getPassword().startsWith("$")) {
            // 未加密存储的情况
            if (!oldPassword.equals(user.getPassword())) {
                return error("当前密码错误");
            }
        } else if (user.getPassword() != null) {
            // BCrypt 加密存储
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return error("当前密码错误");
            }
        }

        // 更新为新密码（BCrypt 加密）
        int result = portalUserService.resetPortalUserPwd(user.getUsername(), passwordEncoder.encode(newPassword));
        if (result > 0) {
            return success("密码修改成功");
        }
        return error("密码修改失败");
    }

    @Operation(summary = "上传头像", description = "当前登录用户上传头像")
    @Log(title = "门户用户-上传头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult uploadAvatar(@RequestParam("file") MultipartFile file) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        if (file == null || file.isEmpty()) {
            return error("请选择上传的文件");
        }

        // 文件大小限制 5MB
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return error("图片大小不能超过5MB");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return error("文件名无效");
        }
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExts = List.of("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExts.contains(fileExt)) {
            return error("不支持的图片格式，请上传 jpg/jpeg/png/gif/webp 格式的图片");
        }

        try {
            // 使用已有的文件上传服务
            SysFile uploadedFile = sysFileService.uploadFileForPortal(file, "avatar", String.valueOf(currentUser.getId()));
            if (uploadedFile == null || uploadedFile.getFileUrl() == null) {
                return error("上传失败");
            }

            // 更新用户头像
            String avatarUrl = uploadedFile.getFileUrl();
            boolean updateResult = portalUserService.updatePortalUserAvatar(currentUser.getUsername(), avatarUrl);
            if (!updateResult) {
                return error("头像更新失败");
            }

            Map<String, String> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            return success(result);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return error("上传失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取用户统计信息", description = "获取当前用户的统计信息（文章数、浏览量、获赞数、关注、粉丝、收藏等）")
    @GetMapping("/stats")
    public AjaxResult getUserStats(
            @Parameter(description = "用户ID（可选，不传则获取当前登录用户的统计）")
            @RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            PortalUser currentUser = PortalSecurityUtils.getUser();
            if (currentUser == null) {
                return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
            }
            targetUserId = currentUser.getId();
        }

        // 从成长体系统计聚合表读取真实数据
        UserStatsVO stats = portalGrowthService.getUserStats(targetUserId);
        return success(stats);
    }

    @Operation(summary = "新增用户", description = "创建新用户")
    @Log(title = "门户用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalUser portalUser) {
        if (!portalUserService.checkPortalUserNameUnique(portalUser)) {
            return error("新增用户'" + portalUser.getUsername() + "'失败，登录账号已存在");
        }
        if (!portalUserService.checkPortalEmailUnique(portalUser)) {
            return error("新增用户'" + portalUser.getUsername() + "'失败，邮箱已存在");
        }
        return toAjax(portalUserService.insertPortalUser(portalUser));
    }

    @Operation(summary = "修改用户", description = "更新用户信息")
    @Log(title = "门户用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalUser portalUser) {
        if (!portalUserService.checkPortalUserNameUnique(portalUser)) {
            return error("修改用户'" + portalUser.getUsername() + "'失败，登录账号已存在");
        }
        if (!portalUserService.checkPortalEmailUnique(portalUser)) {
            return error("修改用户'" + portalUser.getUsername() + "'失败，邮箱已存在");
        }
        return toAjax(portalUserService.updatePortalUser(portalUser));
    }

    @Operation(summary = "删除用户", description = "批量删除用户")
    @Log(title = "门户用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "用户ID数组") @PathVariable Long[] ids) {
        return toAjax(portalUserService.deletePortalUserByIds(ids));
    }

    @Operation(summary = "获取名家列表", description = "获取首页展示的名家列表，含文章数/浏览/获赞/创作天数等统计")
    @GetMapping("/authors")
    public AjaxResult getAuthors(@Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer limit) {
        UserQuery query = new UserQuery();
        query.setStatus("0");
        List<PortalUser> list = portalUserService.selectPortalUserList(query);
        List<Map<String, Object>> result = list.stream().limit(limit).map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("avatar", user.getAvatar());
            item.put("bio", user.getBio());
            item.put("position", user.getPosition());
            // 真实统计：从文章表实时聚合
            try {
                java.util.Map<String, Object> stats = articleMapper.selectAuthorArticleStats(user.getId());
                if (stats != null) {
                    item.put("works", toInt(stats.get("articleCount")));
                    item.put("views", toLong(stats.get("viewSum")));
                    item.put("likes", toLong(stats.get("likeSum")));
                } else {
                    item.put("works", 0);
                    item.put("views", 0L);
                    item.put("likes", 0L);
                }
            } catch (Exception e) {
                item.put("works", 0);
                item.put("views", 0L);
                item.put("likes", 0L);
            }
            // 创作天数：从注册时间计算
            if (user.getCreateTime() != null) {
                long days = java.time.Duration.between(
                        user.getCreateTime(), java.time.LocalDateTime.now()).toDays();
                item.put("days", days);
            } else {
                item.put("days", 0L);
            }
            return item;
        }).toList();
        return success(result);
    }

    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
}

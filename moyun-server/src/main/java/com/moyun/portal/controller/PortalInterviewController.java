package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 面试指南Controller
 *
 * @author moyun
 */
@Tag(name = "面试指南", description = "面试指南相关接口")
@RestController
@RequestMapping("/portal/interview")
public class PortalInterviewController {

    /**
     * 获取面试指南首页数据
     */
    @Operation(summary = "获取面试指南首页数据", description = "获取面试指南首页展示数据")
    @GetMapping("/home")
    @Anonymous
    public AjaxResult getInterviewHome() {
        Map<String, Object> data = new HashMap<>();
        
        // 题目分类
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("id", 1L);
        cat1.put("name", "算法与数据结构");
        cat1.put("slug", "algorithm");
        cat1.put("description", "算法题、数据结构相关面试题");
        cat1.put("icon", "fa-code");
        cat1.put("sort", 1);
        cat1.put("questionCount", 150);
        categories.add(cat1);
        
        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("id", 2L);
        cat2.put("name", "系统设计");
        cat2.put("slug", "system-design");
        cat2.put("description", "系统架构设计、分布式系统等面试题");
        cat2.put("icon", "fa-sitemap");
        cat2.put("sort", 2);
        cat2.put("questionCount", 60);
        categories.add(cat2);
        
        Map<String, Object> cat3 = new HashMap<>();
        cat3.put("id", 3L);
        cat3.put("name", "前端开发");
        cat3.put("slug", "frontend");
        cat3.put("description", "JavaScript、CSS、Vue、React等前端技术面试题");
        cat3.put("icon", "fa-laptop-code");
        cat3.put("sort", 3);
        cat3.put("questionCount", 120);
        categories.add(cat3);
        
        Map<String, Object> cat4 = new HashMap<>();
        cat4.put("id", 4L);
        cat4.put("name", "后端开发");
        cat4.put("slug", "backend");
        cat4.put("description", "Java、Python、Go等后端技术面试题");
        cat4.put("icon", "fa-server");
        cat4.put("sort", 4);
        cat4.put("questionCount", 130);
        categories.add(cat4);
        
        Map<String, Object> cat5 = new HashMap<>();
        cat5.put("id", 5L);
        cat5.put("name", "数据库");
        cat5.put("slug", "database");
        cat5.put("description", "MySQL、Redis等数据库相关面试题");
        cat5.put("icon", "fa-database");
        cat5.put("sort", 5);
        cat5.put("questionCount", 80);
        categories.add(cat5);
        
        // 热门题目
        List<Map<String, Object>> hotQuestions = new ArrayList<>();
        Map<String, Object> q1 = new HashMap<>();
        q1.put("id", 1L);
        q1.put("title", "两数之和");
        q1.put("difficulty", "easy");
        q1.put("tags", Arrays.asList("数组", "哈希表"));
        q1.put("companies", Arrays.asList("字节跳动", "阿里巴巴", "腾讯"));
        q1.put("acceptanceRate", 65.5);
        q1.put("submissionCount", 12500L);
        q1.put("likeCount", 2580L);
        hotQuestions.add(q1);
        
        Map<String, Object> q2 = new HashMap<>();
        q2.put("id", 2L);
        q2.put("title", "最长无重复子串");
        q2.put("difficulty", "medium");
        q2.put("tags", Arrays.asList("字符串", "滑动窗口"));
        q2.put("companies", Arrays.asList("字节跳动", "阿里巴巴"));
        q2.put("acceptanceRate", 48.2);
        q2.put("submissionCount", 8900L);
        q2.put("likeCount", 1680L);
        hotQuestions.add(q2);
        
        Map<String, Object> q3 = new HashMap<>();
        q3.put("id", 3L);
        q3.put("title", "LRU缓存机制");
        q3.put("difficulty", "medium");
        q3.put("tags", Arrays.asList("设计", "哈希表", "双向链表"));
        q3.put("companies", Arrays.asList("字节跳动", "腾讯"));
        q3.put("acceptanceRate", 35.8);
        q3.put("submissionCount", 6500L);
        q3.put("likeCount", 1250L);
        hotQuestions.add(q3);
        
        // 热门面经
        List<Map<String, Object>> experiences = new ArrayList<>();
        Map<String, Object> exp1 = new HashMap<>();
        exp1.put("id", 1L);
        exp1.put("title", "字节跳动后端开发面试经验分享");
        exp1.put("company", "字节跳动");
        exp1.put("position", "后端开发工程师");
        exp1.put("year", 2026);
        exp1.put("month", 5);
        exp1.put("content", "今天分享一下我在字节跳动的后端开发面试经验。一共三轮技术面+一轮HR面...");
        exp1.put("viewCount", 5600L);
        exp1.put("likeCount", 234L);
        exp1.put("commentCount", 45L);
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1L);
        user1.put("nickname", "分享者");
        user1.put("avatar", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&h=80&fit=crop");
        exp1.put("user", user1);
        experiences.add(exp1);
        
        Map<String, Object> exp2 = new HashMap<>();
        exp2.put("id", 2L);
        exp2.put("title", "腾讯前端开发面试总结");
        exp2.put("company", "腾讯");
        exp2.put("position", "前端开发工程师");
        exp2.put("year", 2026);
        exp2.put("month", 4);
        exp2.put("content", "分享一下腾讯前端开发岗的面试过程，主要考察了基础、算法和项目经验...");
        exp2.put("viewCount", 3800L);
        exp2.put("likeCount", 189L);
        exp2.put("commentCount", 32L);
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 1L);
        user2.put("nickname", "分享者");
        user2.put("avatar", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&h=80&fit=crop");
        exp2.put("user", user2);
        experiences.add(exp2);
        
        // 简历模板
        List<Map<String, Object>> resumeTemplates = new ArrayList<>();
        Map<String, Object> resume1 = new HashMap<>();
        resume1.put("id", 1L);
        resume1.put("title", "技术岗通用简历模板");
        resume1.put("description", "适用于技术岗位的通用简历模板，简洁大方");
        resume1.put("cover", "https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=400&h=300&fit=crop");
        resume1.put("category", "技术岗");
        resume1.put("likeCount", 456L);
        resume1.put("downloadCount", 1234L);
        resumeTemplates.add(resume1);
        
        Map<String, Object> resume2 = new HashMap<>();
        resume2.put("id", 2L);
        resume2.put("title", "应届生简洁简历模板");
        resume2.put("description", "适合应届生使用的简洁风格简历模板");
        resume2.put("cover", "https://images.unsplash.com/photo-1553877522-43269d4ea984?w=400&h=300&fit=crop");
        resume2.put("category", "应届生");
        resume2.put("likeCount", 389L);
        resume2.put("downloadCount", 856L);
        resumeTemplates.add(resume2);
        
        data.put("categories", categories);
        data.put("hotQuestions", hotQuestions);
        data.put("experiences", experiences);
        data.put("resumeTemplates", resumeTemplates);
        
        return AjaxResult.success(data);
    }

    /**
     * 获取题目列表
     */
    @Operation(summary = "获取题目列表", description = "获取题目列表，支持分类筛选")
    @GetMapping("/questions")
    @Anonymous
    public AjaxResult getQuestions(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String difficulty,
                                   @RequestParam(required = false, defaultValue = "1") Integer page,
                                   @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Map<String, Object>> questions = new ArrayList<>();
        
        Map<String, Object> q1 = new HashMap<>();
        q1.put("id", 1L);
        q1.put("title", "两数之和");
        q1.put("difficulty", "easy");
        q1.put("tags", Arrays.asList("数组", "哈希表"));
        q1.put("companies", Arrays.asList("字节跳动", "阿里巴巴", "腾讯"));
        q1.put("acceptanceRate", 65.5);
        q1.put("submissionCount", 12500L);
        q1.put("likeCount", 2580L);
        questions.add(q1);
        
        Map<String, Object> q2 = new HashMap<>();
        q2.put("id", 2L);
        q2.put("title", "最长无重复子串");
        q2.put("difficulty", "medium");
        q2.put("tags", Arrays.asList("字符串", "滑动窗口"));
        q2.put("companies", Arrays.asList("字节跳动", "阿里巴巴"));
        q2.put("acceptanceRate", 48.2);
        q2.put("submissionCount", 8900L);
        q2.put("likeCount", 1680L);
        questions.add(q2);
        
        Map<String, Object> q3 = new HashMap<>();
        q3.put("id", 3L);
        q3.put("title", "LRU缓存机制");
        q3.put("difficulty", "medium");
        q3.put("tags", Arrays.asList("设计", "哈希表", "双向链表"));
        q3.put("companies", Arrays.asList("字节跳动", "腾讯"));
        q3.put("acceptanceRate", 35.8);
        q3.put("submissionCount", 6500L);
        q3.put("likeCount", 1250L);
        questions.add(q3);
        
        Map<String, Object> q4 = new HashMap<>();
        q4.put("id", 4L);
        q4.put("title", "实现二叉树的前序遍历");
        q4.put("difficulty", "easy");
        q4.put("tags", Arrays.asList("二叉树", "树", "递归"));
        q4.put("companies", Arrays.asList("阿里巴巴", "腾讯"));
        q4.put("acceptanceRate", 72.3);
        q4.put("submissionCount", 4500L);
        q4.put("likeCount", 890L);
        questions.add(q4);
        
        Map<String, Object> q5 = new HashMap<>();
        q5.put("id", 5L);
        q5.put("title", "实现单例模式");
        q5.put("difficulty", "easy");
        q5.put("tags", Arrays.asList("设计模式", "单例"));
        q5.put("companies", Arrays.asList("阿里巴巴", "腾讯"));
        q5.put("acceptanceRate", 68.5);
        q5.put("submissionCount", 3200L);
        q5.put("likeCount", 760L);
        questions.add(q5);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", questions);
        result.put("total", 15L);
        
        return AjaxResult.success(result);
    }
}

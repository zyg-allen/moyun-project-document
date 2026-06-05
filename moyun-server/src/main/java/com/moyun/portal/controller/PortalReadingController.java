package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 读书空间Controller
 *
 * @author moyun
 */
@Tag(name = "读书空间", description = "读书空间相关接口")
@RestController
@RequestMapping("/portal/reading")
public class PortalReadingController {

    /**
     * 获取读书空间首页数据
     */
    @Operation(summary = "获取读书空间首页数据", description = "获取读书空间首页展示数据")
    @GetMapping("/home")
    @Anonymous
    public AjaxResult getReadingHome() {
        Map<String, Object> data = new HashMap<>();
        
        // 热门书单
        List<Map<String, Object>> bookLists = new ArrayList<>();
        Map<String, Object> list1 = new HashMap<>();
        list1.put("id", 1L);
        list1.put("title", "程序员必读书单");
        list1.put("description", "精选编程领域的经典著作，提升技术水平");
        list1.put("cover", "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&h=300&fit=crop");
        list1.put("bookCount", 3);
        list1.put("viewCount", 1250);
        list1.put("likeCount", 234);
        bookLists.add(list1);
        
        Map<String, Object> list2 = new HashMap<>();
        list2.put("id", 2L);
        list2.put("title", "经典文学作品");
        list2.put("description", "经典文学著作合集，感受文学魅力");
        list2.put("cover", "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=400&h=300&fit=crop");
        list2.put("bookCount", 5);
        list2.put("viewCount", 890);
        list2.put("likeCount", 156);
        bookLists.add(list2);
        
        // 热门书籍
        List<Map<String, Object>> books = new ArrayList<>();
        Map<String, Object> book1 = new HashMap<>();
        book1.put("id", 1L);
        book1.put("title", "代码整洁之道");
        book1.put("author", "Robert C. Martin");
        book1.put("cover", "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=300&fit=crop");
        book1.put("description", "本书是软件工程领域的经典著作，阐述了如何编写整洁、可维护的代码");
        book1.put("rating", new BigDecimal("4.8"));
        book1.put("readingCount", 1520);
        books.add(book1);
        
        Map<String, Object> book2 = new HashMap<>();
        book2.put("id", 2L);
        book2.put("title", "深入理解计算机系统");
        book2.put("author", "Randal E. Bryant");
        book2.put("cover", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop");
        book2.put("description", "本书从程序员视角深入解析计算机系统工作原理");
        book2.put("rating", new BigDecimal("4.9"));
        book2.put("readingCount", 2340);
        books.add(book2);
        
        Map<String, Object> book3 = new HashMap<>();
        book3.put("id", 3L);
        book3.put("title", "活着");
        book3.put("author", "余华");
        book3.put("cover", "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=300&fit=crop");
        book3.put("description", "讲述了福贵一生的悲欢故事，深刻反映人生的苦难与希望");
        book3.put("rating", new BigDecimal("4.7"));
        book3.put("readingCount", 5680);
        books.add(book3);
        
        // 金句摘录
        List<Map<String, Object>> quotes = new ArrayList<>();
        Map<String, Object> quote1 = new HashMap<>();
        quote1.put("id", 1L);
        quote1.put("content", "代码是写给人看的，顺便给机器执行。");
        quote1.put("page", "第15页");
        quote1.put("chapter", "第一章");
        quote1.put("likeCount", 156);
        Map<String, Object> quoteBook1 = new HashMap<>();
        quoteBook1.put("id", 1L);
        quoteBook1.put("title", "代码整洁之道");
        quoteBook1.put("cover", "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=300&fit=crop");
        quote1.put("book", quoteBook1);
        quotes.add(quote1);
        
        Map<String, Object> quote2 = new HashMap<>();
        quote2.put("id", 2L);
        quote2.put("content", "人是为活着本身而活着的，而不是为了活着之外的任何事物所活着。");
        quote2.put("page", "第50页");
        quote2.put("chapter", "第一章");
        quote2.put("likeCount", 423);
        Map<String, Object> quoteBook2 = new HashMap<>();
        quoteBook2.put("id", 3L);
        quoteBook2.put("title", "活着");
        quoteBook2.put("cover", "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=300&fit=crop");
        quote2.put("book", quoteBook2);
        quotes.add(quote2);
        
        data.put("bookLists", bookLists);
        data.put("books", books);
        data.put("quotes", quotes);
        
        return AjaxResult.success(data);
    }

    /**
     * 获取书籍列表
     */
    @Operation(summary = "获取书籍列表", description = "获取书籍列表，支持分类筛选")
    @GetMapping("/books")
    @Anonymous
    public AjaxResult getBooks(@RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false, defaultValue = "1") Integer page,
                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Map<String, Object>> books = new ArrayList<>();
        
        Map<String, Object> book1 = new HashMap<>();
        book1.put("id", 1L);
        book1.put("title", "代码整洁之道");
        book1.put("author", "Robert C. Martin");
        book1.put("cover", "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=300&fit=crop");
        book1.put("description", "本书是软件工程领域的经典著作，阐述了如何编写整洁、可维护的代码");
        book1.put("rating", new BigDecimal("4.8"));
        book1.put("readingCount", 1520);
        book1.put("tags", Arrays.asList("编程", "代码质量", "软件工程"));
        books.add(book1);
        
        Map<String, Object> book2 = new HashMap<>();
        book2.put("id", 2L);
        book2.put("title", "深入理解计算机系统");
        book2.put("author", "Randal E. Bryant");
        book2.put("cover", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop");
        book2.put("description", "本书从程序员视角深入解析计算机系统工作原理");
        book2.put("rating", new BigDecimal("4.9"));
        book2.put("readingCount", 2340);
        book2.put("tags", Arrays.asList("计算机系统", "编程基础", "操作系统"));
        books.add(book2);
        
        Map<String, Object> book3 = new HashMap<>();
        book3.put("id", 3L);
        book3.put("title", "活着");
        book3.put("author", "余华");
        book3.put("cover", "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=300&fit=crop");
        book3.put("description", "讲述了福贵一生的悲欢故事，深刻反映人生的苦难与希望");
        book3.put("rating", new BigDecimal("4.7"));
        book3.put("readingCount", 5680);
        book3.put("tags", Arrays.asList("文学", "小说", "经典"));
        books.add(book3);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", books);
        result.put("total", 10L);
        
        return AjaxResult.success(result);
    }
}

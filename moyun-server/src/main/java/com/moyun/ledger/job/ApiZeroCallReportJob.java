package com.moyun.ledger.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moyun.core.config.ApiCatalog;
import com.moyun.core.config.ApiStatsFilter;

/**
 * 每日零调用接口报告：09:30 输出到日志与 logs/api-zero-call-日期.log。
 *
 * <p>只做报告，不自动删除；连续多日为 0 的接口由人工确认后下线。</p>
 */
@Component
public class ApiZeroCallReportJob {

    private static final Logger log = LoggerFactory.getLogger(ApiZeroCallReportJob.class);

    @Autowired
    private ApiCatalog catalog;

    @Scheduled(cron = "0 30 9 * * ?")
    public void report() {
        try {
            Set<String> called = ApiStatsFilter.calledKeys();
            Set<String> zero = catalog.zeroCall(called);
            List<String> lines = zero.stream().toList();

            StringBuilder sb = new StringBuilder();
            sb.append("# 零调用接口报告 ").append(LocalDate.now()).append('\n');
            sb.append("# 总数=").append(lines.size()).append("，已调用=").append(called.size())
                    .append("，接口总数=").append(catalog.all().size()).append("\n");
            lines.forEach(l -> sb.append(l).append('\n'));

            log.info("零调用接口报告：{} 个（接口总数 {}）", lines.size(), catalog.all().size());

            Path dir = Paths.get("logs");
            Files.createDirectories(dir);
            Path file = dir.resolve("api-zero-call-" + LocalDate.now() + ".log");
            Files.writeString(file, sb.toString());
            log.info("零调用接口报告已写入 {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("零调用接口报告写入失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("零调用接口报告异常: {}", e.getMessage());
        }
    }
}

package com.moyun.portal.judge;

import java.util.Arrays;
import java.util.List;

import lombok.Data;

/**
 * 语言运行时配置（v6.3 OJ 判题系统）
 * <p>
 * 描述每种语言的编译/执行命令与产物路径策略，供 {@link ProcessJudgeEngine} 使用。
 *
 * @author moyun
 */
@Data
public class LanguageRuntime {

    /** 语言名（小写） */
    private String language;

    /** 文件后缀（不含点，如 "py"、"cpp"） */
    private String fileSuffix;

    /** 是否编译型语言 */
    private boolean compiled;

    /** 编译命令模板（编译型语言使用，命令数组的列表形式） */
    private List<String> compileCommand;

    /** 运行命令模板（compiled 时执行产物路径，解释型直接执行源文件） */
    private List<String> runCommand;

    /** 是否需要 wrap 用户代码到模板（如 Java 需包成类，C/C++ 不需要） */
    private boolean needsWrapper;

    public static LanguageRuntime javascript() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "javascript";
        r.fileSuffix = "js";
        r.compiled = false;
        r.runCommand = Arrays.asList("node", "--no-warnings", "<SRC>");
        r.needsWrapper = false;
        return r;
    }

    public static LanguageRuntime typescript() {
        // 当前沙箱无 tsx/ts-node，降级为：先 tsc 编译为 js 再 node 运行
        // 若生产环境无 typescript 工具链，则该语言不可用，前端应在语言列表中过滤
        LanguageRuntime r = new LanguageRuntime();
        r.language = "typescript";
        r.fileSuffix = "ts";
        r.compiled = true;
        r.compileCommand = Arrays.asList("tsc", "--target", "ES2020", "--module", "CommonJS",
                "--moduleResolution", "node", "--skipLibCheck", "<SRC>", "--outDir", "<OUTDIR>");
        r.runCommand = Arrays.asList("node", "<OUT>");
        r.needsWrapper = false;
        return r;
    }

    public static LanguageRuntime python() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "python";
        r.fileSuffix = "py";
        r.compiled = false;
        r.runCommand = Arrays.asList("python3", "-I", "-B", "<SRC>");
        r.needsWrapper = false;
        return r;
    }

    public static LanguageRuntime java() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "java";
        r.fileSuffix = "java";
        r.compiled = true;
        r.compileCommand = Arrays.asList("javac", "-encoding", "UTF-8", "<SRC>");
        // 类名固定为 Solution，与 wrap 模板一致
        r.runCommand = Arrays.asList("java", "-Xss64m", "-cp", "<WORKDIR>", "Solution");
        r.needsWrapper = true;
        return r;
    }

    public static LanguageRuntime go() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "go";
        r.fileSuffix = "go";
        r.compiled = false; // go run 内置编译+执行，无需单独编译步骤
        r.runCommand = Arrays.asList("go", "run", "<SRC>");
        r.needsWrapper = false;
        return r;
    }

    public static LanguageRuntime cpp() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "cpp";
        r.fileSuffix = "cpp";
        r.compiled = true;
        r.compileCommand = Arrays.asList("g++", "-O2", "-std=c++17", "-o", "<OUT>", "<SRC>");
        r.runCommand = Arrays.asList("<OUT>");
        r.needsWrapper = false;
        return r;
    }

    public static LanguageRuntime rust() {
        LanguageRuntime r = new LanguageRuntime();
        r.language = "rust";
        r.fileSuffix = "rs";
        r.compiled = true;
        r.compileCommand = Arrays.asList("rustc", "-O", "-o", "<OUT>", "<SRC>");
        r.runCommand = Arrays.asList("<OUT>");
        r.needsWrapper = false;
        return r;
    }

    /**
     * 根据语言名获取运行时配置，不识别返回 null
     */
    public static LanguageRuntime of(String language) {
        if (language == null) return null;
        String l = language.trim().toLowerCase();
        // 兼容常见别名
        switch (l) {
            case "javascript":
            case "js":
                return javascript();
            case "typescript":
            case "ts":
                return typescript();
            case "python":
            case "python3":
            case "py":
                return python();
            case "java":
                return java();
            case "go":
            case "golang":
                return go();
            case "cpp":
            case "c++":
            case "cxx":
                return cpp();
            case "rust":
            case "rs":
                return rust();
            default:
                return null;
        }
    }
}

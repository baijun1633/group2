package com.cqu.springboot.util;

import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 解析工具类（RFC 4180 兼容）
 * <p>
 * 支持：
 * - 逗号分隔字段
 * - 双引号包裹含逗号/换行的字段
 * - 双引号内两个连续双引号转义为一个双引号
 * </p>
 */
public final class CsvUtil {

    private CsvUtil() {}

    /**
     * 解析整段 CSV 文本，第一行作为表头，返回每行为 字段名→值 的 Map
     *
     * @param content CSV 文本内容
     * @return 数据行列表（不含表头行）
     */
    public static List<Map<String, String>> parse(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.CSV_FORMAT_ERROR, "CSV内容为空");
        }
        // 统一换行符
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = splitLines(normalized);

        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.CSV_FORMAT_ERROR, "CSV无有效数据行");
        }

        // 第一行为表头
        List<String> headers = parseLine(lines.get(0));
        if (headers.isEmpty()) {
            throw new BusinessException(ErrorCode.CSV_FORMAT_ERROR, "CSV表头为空");
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            List<String> fields = parseLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                String value = j < fields.size() ? fields.get(j) : "";
                row.put(headers.get(j).trim(), value);
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 按 CSV 规则解析单行（可能跨多行，因引号内换行），这里处理已按行分割的文本
     */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // 两个连续引号转义为一个
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * 分割文本为行，处理引号内的换行符（将跨行字段合并）
     */
    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '\n' && !inQuotes) {
                lines.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}

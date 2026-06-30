package com.cqu.springboot.util;

import com.cqu.springboot.config.FileConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FileUrlUtil {

    private final FileConfig fileConfig;

    public String buildFullUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        return fileConfig.getBaseUrl() + relativePath;
    }

    public String buildFullUrl(String relativePath, String baseUrl) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        return baseUrl + relativePath;
    }
}
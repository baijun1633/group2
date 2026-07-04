package com.cqu.springboot.service;

import com.cqu.springboot.dto.PythonKgResponse;
import com.cqu.springboot.dto.RecommendItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Python KG 微服务客户端
 * <p>
 * 调用同学的 Python 推荐服务接口，将返回结果转换为 RecommendItem
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonKgClient {

    @Qualifier("pythonRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${python-service.base-url}")
    private String baseUrl;

    /**
     * 调用 Python KG 推荐接口
     */
    public List<RecommendItem> recommend(List<String> preferredTags,
                                          List<String> top3Categories,
                                          List<String> top5Categories,
                                          List<String> preferredAuthors,
                                          List<Long> likedBookIds,
                                          int limit) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/api/v1/recommend/kg");

            if (preferredTags != null) {
                for (String tag : preferredTags) {
                    uriBuilder.queryParam("preferred_tags", tag);
                }
            }
            if (top3Categories != null) {
                for (String cat : top3Categories) {
                    uriBuilder.queryParam("top3_categories", cat);
                }
            }
            if (top5Categories != null) {
                for (String cat : top5Categories) {
                    uriBuilder.queryParam("top5_categories", cat);
                }
            }
            if (preferredAuthors != null) {
                for (String author : preferredAuthors) {
                    uriBuilder.queryParam("preferred_authors", author);
                }
            }
            if (likedBookIds != null) {
                for (Long bookId : likedBookIds) {
                    uriBuilder.queryParam("liked_book_ids", bookId);
                }
            }
            uriBuilder.queryParam("limit", limit);

            String url = uriBuilder.toUriString();
            log.info("调用 Python KG 推荐: {}", url);

            ResponseEntity<PythonKgResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            PythonKgResponse body = response.getBody();
            if (body == null || body.getCode() != 200 || body.getData() == null) {
                log.warn("Python KG 推荐返回异常: code={}, msg={}",
                        body != null ? body.getCode() : "null",
                        body != null ? body.getMsg() : "null");
                return Collections.emptyList();
            }

            // 转换为 RecommendItem
            return body.getData().stream()
                    .map(this::convertToRecommendItem)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("调用 Python KG 推荐服务失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 调用 Python 图谱查询接口
     */
    public Map<String, Object> getGraph(Long bookId) {
        try {
            String url = baseUrl + "/api/v1/kg/graph/" + bookId;
            log.info("调用 Python 图谱查询: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用 Python 图谱查询失败, bookId={}", bookId, e);
            return null;
        }
    }

    private RecommendItem convertToRecommendItem(PythonKgResponse.KgRecommendItem item) {
        RecommendItem recommendItem = new RecommendItem();
        recommendItem.setBookId(item.getBookId());
        recommendItem.setTitle(item.getBookTitle());
        recommendItem.setScore(item.getMatchScore() != null ? item.getMatchScore() / 20.0 : 0.5);
        recommendItem.setReason(item.getRecommendReason());
        recommendItem.setSource(item.getRecommendType() != null ? item.getRecommendType() : "KG_PYTHON");
        return recommendItem;
    }
}

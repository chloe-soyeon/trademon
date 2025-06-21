package kr.trademon.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.trademon.dto.NewsDTO;
import kr.trademon.service.INewsService;
import kr.trademon.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class NewsService implements INewsService {

    private final MongoTemplate mongoTemplate;

    public NewsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    @Override
    public List<NewsDTO> searchNews(String keyword) throws Exception {
        String encoded = UriUtils.encode(keyword, StandardCharsets.UTF_8);
        String apiUrl = "https://openapi.naver.com/v1/search/news.json?query=" + encoded + "&display=10&sort=date";

        log.info("🔍 네이버 뉴스 API 호출: {}", apiUrl);

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Naver-Client-Id", clientId);
        conn.setRequestProperty("X-Naver-Client-Secret", clientSecret);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        log.info("🔎 응답 코드: {}", responseCode);

        Scanner sc;
        if (responseCode == 200) {
            sc = new Scanner(conn.getInputStream()); // 정상 호출
        } else {
            sc = new Scanner(conn.getErrorStream()); // 오류 발생 시
            log.error("❌ 네이버 API 호출 실패: {}", sc.useDelimiter("\\A").next());
            throw new RuntimeException("네이버 뉴스 API 호출 실패: HTTP " + responseCode);
        }

        String response = sc.useDelimiter("\\A").next();
        sc.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode items = root.get("items");

        List<NewsDTO> result = new ArrayList<>();

        for (JsonNode item : items) {
            NewsDTO dto = new NewsDTO();
            dto.setNewsId(UUID.randomUUID().toString());

            // ✅ HTML 태그 제거 + HTML Entity 디코딩
            String rawTitle = item.get("title").asText();
            String cleanedTitle = StringEscapeUtils.unescapeHtml4(rawTitle.replaceAll("<[^>]*>", ""));
            dto.setTitle(cleanedTitle);

            dto.setUrl(item.get("link").asText());
            result.add(dto);
        }

        return result;
    }


    @Override
    public int saveScrap(NewsDTO pDTO) throws Exception {
        log.info("💾 뉴스 스크랩 저장: {}", pDTO.getTitle());

        // 👇 이메일 암호화 적용
        String encryptedEmail = EncryptUtil.encAES128CBC(pDTO.getUserEmail());
        pDTO.setUserEmail(encryptedEmail);


        pDTO.setScrapTime(new Date());
        mongoTemplate.save(pDTO, "NEWS");
        return 1;
    }

    @Override
    public List<NewsDTO> getScrapList(String userEmail) throws Exception {
        // 🔐 AES 암호화 처리
        String encryptedEmail = EncryptUtil.encAES128CBC(userEmail);

        Query query = new Query(Criteria.where("userEmail").is(encryptedEmail))
                .with(Sort.by(Sort.Direction.DESC, "scrapTime"));
        return mongoTemplate.find(query, NewsDTO.class, "NEWS");
    }

    @Override
    public int deleteScrap(NewsDTO pDTO) throws Exception {
        String encEmail = pDTO.getUserEmail();
        String newsId = pDTO.getNewsId();

        Query query = new Query();
        query.addCriteria(Criteria.where("userEmail").is(encEmail).and("newsId").is(newsId));

        return (int) mongoTemplate.remove(query, "NEWS").getDeletedCount();
    }



}

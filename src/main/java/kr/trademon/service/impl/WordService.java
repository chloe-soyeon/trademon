package kr.trademon.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.trademon.dto.WordDTO;
import kr.trademon.service.IWordService;
import kr.trademon.util.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class WordService implements IWordService {

    @Value("${bok.api.key}")
    private String apiKey;

    @Override
    public WordDTO searchWord(String word) throws Exception {
        String encoded = UriUtils.encode(word, StandardCharsets.UTF_8);
        String apiUrl = "https://ecos.bok.or.kr/api/StatisticWord/" + apiKey + "/json/kr/1/1/" + encoded;

        log.info("📡 한국은행 용어 API 호출 URL: {}", apiUrl);

        String response = NetworkUtil.get(apiUrl);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        JsonNode wordNode = root.path("StatisticWord").path("row").get(0);
        if (wordNode == null || wordNode.isMissingNode()) {
            return null;
        }

        WordDTO dto = new WordDTO();
        dto.setSearchWord(word);
        dto.setWordTitle(wordNode.path("WORD").asText());
        dto.setWordExplanation(wordNode.path("CONTENT").asText());

        return dto;
    }
}

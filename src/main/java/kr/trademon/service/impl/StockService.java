package kr.trademon.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.trademon.dto.*;

import kr.trademon.service.IStockService;
import kr.trademon.util.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;


@Slf4j
@Service
public class StockService implements IStockService {

    @Value("${dart.api.key}")
    private String apiKey;

    @Value("${krx.api.key}")
    private String krxApiKey;

    @Value("${kis.api.appkey}")
    private String kisAppKey;

    @Value("${kis.api.secret}")
    private String kisAppSecret;

    private static final String KIS_BASE_URL = "https://openapivts.koreainvestment.com:29443";

    private String cachedAccessToken = null;
    private LocalDateTime tokenExpiryTime = null;

    @Override
    public List<CorpInfoDTO> searchCorpNames(String keyword) {
        log.info("🔍 [searchCorpNames] 키워드: {}", keyword);
        List<CorpInfoDTO> result = new ArrayList<>();
        try (InputStream is = new ClassPathResource("data/CORPCODE.xml").getInputStream()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            doc.getDocumentElement().normalize();

            NodeList list = doc.getElementsByTagName("list");
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                String corpName = e.getElementsByTagName("corp_name").item(0).getTextContent();
                String corpCode = e.getElementsByTagName("corp_code").item(0).getTextContent();

                if (corpName.contains(keyword)) {
                    CorpInfoDTO dto = new CorpInfoDTO();
                    dto.setCorpName(corpName);
                    dto.setCorpCode(corpCode);
                    result.add(dto);
                }
            }

            log.info("✅ [searchCorpNames] 검색 결과 수: {}", result.size());

        } catch (Exception e) {
            log.error("❌ [searchCorpNames] 실패: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public List<DisclosureDTO> getDisclosures(String corpName, String startDate, String endDate) {
        log.info("📑 [getDisclosures] 기업명: {}, 시작일: {}, 종료일: {}", corpName, startDate, endDate);
        String corpCode = getCorpCode(corpName);
        if (corpCode == null) {
            log.warn("⚠️ [getDisclosures] '{}'의 corpCode 찾기 실패", corpName);
            return Collections.emptyList();
        }

        List<DisclosureDTO> list = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        int pageNo = 1;
        int pageCount = 100;
        int totalCount = 0;

        try {
            do {
                String apiUrl = String.format(
                        "https://opendart.fss.or.kr/api/list.json?crtfc_key=%s&corp_code=%s&bgn_de=%s&end_de=%s&page_no=%d&page_count=%d",
                        apiKey, corpCode, startDate, endDate, pageNo, pageCount);
                log.info("📡 [getDisclosures] API 호출 URL: {}", apiUrl);

                String response = NetworkUtil.get(apiUrl);
                JsonNode root = mapper.readTree(response);

                if (!"000".equals(root.path("status").asText())) {
                    log.warn("❗ [getDisclosures] DART 오류: {}", root.path("message").asText());
                    break;
                }

                for (JsonNode item : root.path("list")) {
                    DisclosureDTO dto = new DisclosureDTO();
                    dto.setReportName(item.get("report_nm").asText());
                    dto.setReceiptNo(item.get("rcept_no").asText());
                    dto.setReceiptDate(item.get("rcept_dt").asText());
                    dto.setUrl("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + dto.getReceiptNo());
                    list.add(dto);
                }

                totalCount = root.path("total_count").asInt();
                log.info("📃 [getDisclosures] pageNo: {}, 누적 수집: {}", pageNo, list.size());
                pageNo++;

            } while ((pageNo - 1) * pageCount < totalCount);

        } catch (Exception e) {
            log.error("❌ [getDisclosures] 공시 정보 수집 실패: {}", e.getMessage());
        }

        log.info("✅ [getDisclosures] 총 공시 건수: {}", list.size());
        return list;
    }

    @Override
    public String fetchKrxStockInfo(String corpName) {
        log.info("🔍 [fetchKrxStockInfo] 기업명: {}", corpName);
        try {
            String encodedName = URLEncoder.encode(corpName, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo" +
                            "?serviceKey=%s&numOfRows=1&pageNo=1&resultType=json&itmsNm=%s",
                    krxApiKey, encodedName);
            log.info("📡 [fetchKrxStockInfo] API URL: {}", url);

            String response = NetworkUtil.get(url);
            JsonNode root = new ObjectMapper().readTree(response);

            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            if (itemsNode.isArray() && itemsNode.size() > 0) {
                JsonNode item = itemsNode.get(0);
                String srtnCd = item.get("srtnCd").asText().replace("A", "");
                return srtnCd;
            } else {
                log.warn("❗ [fetchKrxStockInfo] '{}'에 대한 종목 정보가 없습니다.", corpName);
                return null;
            }


//            JsonNode item = root.path("response").path("body").path("items").path("item").get(0);
//            String srtnCd = item.get("srtnCd").asText().replace("A", "");
//            log.info("✅ [fetchKrxStockInfo] 종목코드: {}", srtnCd);
//            return srtnCd;
        } catch (Exception e) {
            log.error("❌ [fetchKrxStockInfo] 실패: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getKisAccessToken() {
        log.info("🔐 [getKisAccessToken] KIS 인증 토큰 요청 시작");

        try {
            if (cachedAccessToken != null && tokenExpiryTime != null && tokenExpiryTime.isAfter(LocalDateTime.now())) {
                log.info("♻️ [getKisAccessToken] 캐시 토큰 재사용 (만료 시각: {})", tokenExpiryTime);
                return cachedAccessToken;
            }

            String url = KIS_BASE_URL + "/oauth2/tokenP";
            String jsonBody = String.format("{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}", kisAppKey, kisAppSecret);
            Map<String, String> headers = Map.of("Content-Type", "application/json");

            log.info("📡 [getKisAccessToken] 요청 URL: {}", url);

            String response = NetworkUtil.post(url, headers, jsonBody);
            JsonNode root = new ObjectMapper().readTree(response);

            cachedAccessToken = root.path("access_token").asText();
            tokenExpiryTime = LocalDateTime.parse(
                    root.path("access_token_token_expired").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.info("✅ [getKisAccessToken] 토큰 발급 완료 (만료: {})", tokenExpiryTime);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("❌ [getKisAccessToken] 실패: {}", e.getMessage());
            return null;
        }
    }

    private final Semaphore kisApiLock = new Semaphore(1); // 동시 실행 제한

    @Override
    public StockInfoDTO getKisStockInfo(String stockCode, String accessToken) {
        log.info("📊 [getKisStockInfo] 종목코드: {}", stockCode);

        try {
            kisApiLock.acquire(); // 동시 요청 제한
            Thread.sleep(300); // API 요청 간 시간차

            String url = String.format(
                    "%s/uapi/domestic-stock/v1/quotations/inquire-price?fid_cond_mrkt_div_code=J&fid_input_iscd=%s",
                    KIS_BASE_URL, stockCode);

            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer " + accessToken,
                    "appkey", kisAppKey,
                    "appsecret", kisAppSecret,
                    "tr_id", "FHKST01010100",
                    "Content-Type", "application/json"
            );

            log.info("📡 [getKisStockInfo] API 호출 URL: {}", url);
            String response = NetworkUtil.get(url, headers);
            JsonNode output = new ObjectMapper().readTree(response).path("output");

            // ✅ 필수 필드 유효성 검사
            String currentPrice = output.path("stck_prpr").asText();
            if (currentPrice == null || currentPrice.isBlank()) {
                log.warn("⚠️ [getKisStockInfo] 현재가가 비어있습니다 - 종목코드: {}, 응답 원문: {}", stockCode, output.toPrettyString());
                return StockInfoDTO.builder().build();
            }

            // 부호 변환 로직
            String rawSign = output.path("prdy_vrss_sign").asText();
            String normalizedSign;
            switch (rawSign) {
                case "1": case "2": case "3":
                    normalizedSign = rawSign;
                    break;
                case "4": case "5":
                    normalizedSign = String.valueOf(Integer.parseInt(rawSign) - 3);
                    break;
                default:
                    normalizedSign = "3"; // 보합
            }

            StockInfoDTO dto = StockInfoDTO.builder()
                    .per(output.path("per").asText())
                    .eps(output.path("eps").asText())
                    .pbr(output.path("pbr").asText())
                    .bps(output.path("bps").asText())
                    .w52Hgpr(output.path("w52_hgpr").asText())
                    .w52Lwpr(output.path("w52_lwpr").asText())
                    .htsFrgnEhrt(output.path("hts_frgn_ehrt").asText())
                    .currentPrice(currentPrice)
                    .prdyVrss(output.path("prdy_vrss").asText())
                    .prdyVrssSign(normalizedSign)
                    .prdyCtrt(output.path("prdy_ctrt").asText())
                    .build();

            log.info("\n✅ [getKisStockInfo] 종목 정보 조회 결과\n" +
                            "📌 현재가        : {}\n" +
                            "📉 전일 대비     : {} (부호: {}, 변환됨)\n" +
                            "📊 전일 대비율   : {}%\n" +
                            "💡 PER           : {}\n" +
                            "💰 EPS           : {}\n" +
                            "📈 PBR           : {}\n" +
                            "🌏 외인 보유율   : {}%",
                    dto.getCurrentPrice(),
                    dto.getPrdyVrss(),
                    dto.getPrdyVrssSign(),
                    dto.getPrdyCtrt(),
                    dto.getPer(),
                    dto.getEps(),
                    dto.getPbr(),
                    dto.getHtsFrgnEhrt());

            return dto;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🚫 [getKisStockInfo] 인터럽트 예외: {}", e.getMessage());
            return StockInfoDTO.builder().build();
        } catch (Exception e) {
            log.error("❌ [getKisStockInfo] 실패: {}", e.getMessage());
            return StockInfoDTO.builder().build();
        } finally {
            kisApiLock.release(); // 반드시 해제
        }
    }


    @Override
    public List<CandleDTO> getKisCandleChart(String stockCode, String accessToken) {
        log.info("📈 [getKisCandleChart] 캔들차트 요청 - 종목코드: {}", stockCode);
        List<CandleDTO> candles = new ArrayList<>();

        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(10000);
            String url = String.format(
                    "%s/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice" +
                            "?fid_cond_mrkt_div_code=J&fid_input_iscd=%s&fid_period_div_code=D&fid_org_adj_prc=1&fid_input_date_1=%s&fid_input_date_2=%s",
                    KIS_BASE_URL, stockCode,
                    start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    end.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            );

            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer " + accessToken,
                    "appkey", kisAppKey,
                    "appsecret", kisAppSecret,
                    "tr_id", "FHKST03010100",
                    "Content-Type", "application/json"
            );

            log.info("📡 [getKisCandleChart] API URL: {}", url);

            String response = NetworkUtil.get(url, headers);
            JsonNode root = new ObjectMapper().readTree(response).path("output2");

            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode node = root.get(i);
                CandleDTO dto = CandleDTO.builder()
                        .time(LocalDate.parse(node.path("stck_bsop_date").asText(), DateTimeFormatter.ofPattern("yyyyMMdd")).toString())
                        .open(node.path("stck_oprc").asDouble())
                        .high(node.path("stck_hgpr").asDouble())
                        .low(node.path("stck_lwpr").asDouble())
                        .close(node.path("stck_clpr").asDouble())
                        .build();
                candles.add(dto);
            }

            log.info("✅ [getKisCandleChart] 수집된 캔들 수: {}", candles.size());

        } catch (Exception e) {
            log.error("❌ [getKisCandleChart] 실패: {}", e.getMessage());
        }

        return candles;
    }

    private String getCorpCode(String corpName) {
        String corpCode = searchCorpNames(corpName).stream()
                .filter(c -> c.getCorpName().equals(corpName))
                .map(CorpInfoDTO::getCorpCode)
                .findFirst()
                .orElse(null);
        log.info("🔍 [getCorpCode] '{}'의 코드: {}", corpName, corpCode);
        return corpCode;
    }

}
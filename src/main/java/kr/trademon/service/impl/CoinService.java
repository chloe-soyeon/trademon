package kr.trademon.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.trademon.dto.CoinNameCodeDTO;
import kr.trademon.dto.CoinCandleDTO;
import kr.trademon.service.ICoinService;
import kr.trademon.util.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoinService implements ICoinService {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<CoinNameCodeDTO> allCoinNameCode = new ArrayList<>();

    @Value("${upbit.access-key}")
    private String accessKey;

    @Value("${upbit.secret-key}")
    private String secretKey;

    public CoinService() {
        loadAllCoinNameCode(); // 애플리케이션 시작 시 모든 코인 이름-코드 정보 로딩
    }

    private void loadAllCoinNameCode() {
        String url = "https://api.upbit.com/v1/market/all?isDetails=false";
        log.info("📡 [loadAllCoinNameCode] 업비트 API 호출 URL: {}", url);
        try {
            String json = NetworkUtil.get(url);
            JsonNode markets = mapper.readTree(json);

            if (markets != null && markets.isArray()) {
                allCoinNameCode = new ArrayList<>();
                for (JsonNode market : markets) {
                    if (market.has("market") && market.has("korean_name")) {
                        String code = market.get("market").asText();
                        String name = market.get("korean_name").asText();
                        if (code.startsWith("KRW-")) { // 원화 마켓만 필터링 (선택 사항)
                            allCoinNameCode.add(new CoinNameCodeDTO(name, code));
                            log.debug("✅ [loadAllCoinNameCode] {} - {}", name, code);
                        }
                    }
                }
                log.info("✅ [loadAllCoinNameCode] 총 {}개의 코인 이름-코드 정보 로딩 완료", allCoinNameCode.size());
            } else {
                log.warn("⚠️ [loadAllCoinNameCode] 업비트 API 응답이 올바르지 않습니다.");
            }
        } catch (Exception e) {
            log.error("❌ [loadAllCoinNameCode] 업비트 API 호출 실패: {}", e.getMessage());
        }
    }

    public List<CoinNameCodeDTO> searchCoinByName(String query) {
        log.info("🔍 [searchCoinByName] 검색어: {}", query);
        return allCoinNameCode.stream()
                .filter(dto -> dto.getName().contains(query.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public String getCoinPrice(String coinCode) {
        String url = "https://api.upbit.com/v1/ticker?markets=" + coinCode;
        log.info("📡 [getCoinPrice] 업비트 API 호출 URL: {}", url);

        try {
            String json = NetworkUtil.get(url);
            JsonNode root = mapper.readTree(json);

            if (root.isArray() && root.size() > 0 && root.get(0).has("trade_price")) {
                String price = root.get(0).get("trade_price").asText();
                if (price != null && !price.isEmpty()) {
                    log.info("✅ [getCoinPrice] {} 현재가: {}", coinCode, price);
                    return price;
                } else {
                    log.info("❌ [getCoinPrice] {} 가격 데이터가 null 또는 비어 있음", coinCode);
                    return "0"; // 기본값
                }
            } else {
                log.info("❌ [getCoinPrice] {} 응답 데이터에 가격 정보가 없음", coinCode);
                return "0"; // 기본값
            }
        } catch (Exception e) {
            log.error("❌ [getCoinPrice] 실패: {}", e.getMessage());
            return "0"; // 기본값
        }
    }

    @Override
    public List<CoinCandleDTO> getCandleChart(String coinCode) {
        String url = "https://api.upbit.com/v1/candles/days?market=" + coinCode + "&count=100";
        List<CoinCandleDTO> result = new ArrayList<>();
        log.info("📈 [getCandleChart] 캔들차트 요청 URL: {}", url);

        try {
            String json = NetworkUtil.get(url);
            JsonNode arr = mapper.readTree(json);
            log.debug("✅ [getCandleChart] 원본 응답 JSON: {}", json);

            if (arr != null && arr.isArray()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

                for (JsonNode node : arr) {
                    String timeStr = node.path("candle_date_time_kst").asText("");
                    double open = node.path("opening_price").asDouble();
                    double high = node.path("high_price").asDouble();
                    double low = node.path("low_price").asDouble();
                    double close = node.path("trade_price").asDouble();

                    if (!timeStr.isEmpty()) {
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(timeStr, formatter);
                            long timestamp = ldt.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();

                            CoinCandleDTO dto = CoinCandleDTO.builder()
                                    .time(timestamp)
                                    .open(open)
                                    .high(high)
                                    .low(low)
                                    .close(close)
                                    .build();

                            log.debug("📦 [CandleDTO] {}", dto);
                            result.add(dto);
                        } catch (Exception ex) {
                            log.warn("⚠️ [getCandleChart] 시간 파싱 실패: {} - {}", timeStr, ex.getMessage());
                        }
                    }
                }
            }

            log.info("✅ [getCandleChart] 최종 반환 캔들 수: {}", result.size());
        } catch (Exception e) {
            log.error("❌ [getCandleChart] 요청 실패: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, String>> getTopCoinPrices(int limit) {
        List<CoinNameCodeDTO> topCoins = getTopCoinNameCodes(limit);
        List<Map<String, String>> result = new ArrayList<>();

        if (topCoins.isEmpty()) return result;

        // KRW-코인만 대상
        String marketQuery = topCoins.stream()
                .map(CoinNameCodeDTO::getCode)
                .filter(code -> code.startsWith("KRW-"))
                .collect(Collectors.joining(","));

        String url = "https://api.upbit.com/v1/ticker?markets=" + marketQuery;
        log.info("📡 [getTopCoinPrices] 업비트 시세 URL: {}", url);

        try {
            String json = NetworkUtil.get(url);
            JsonNode prices = mapper.readTree(json);

            for (JsonNode priceNode : prices) {
                String market = priceNode.get("market").asText();       // KRW-BTC
                String tradePrice = priceNode.get("trade_price").asText();

                topCoins.stream()
                        .filter(dto -> dto.getCode().equals(market))
                        .findFirst()
                        .ifPresent(dto -> {
                            result.add(Map.of(
                                    "code", market,
                                    "symbol", market.replace("KRW-", ""),
                                    "name", dto.getName(),
                                    "price", tradePrice
                            ));
                        });
            }

            log.info("✅ [getTopCoinPrices] 시세 조회 완료 ({}개)", result.size());
        } catch (Exception e) {
            log.error("❌ [getTopCoinPrices] 실패: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public List<CoinNameCodeDTO> getTopCoinNameCodes(int limit) {
        return allCoinNameCode.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }




}
package kr.trademon.service.impl;

import com.google.common.util.concurrent.RateLimiter;
import kr.trademon.dto.StockInfoDTO;
import kr.trademon.dto.TradeAssetDTO;
import kr.trademon.mapper.ITradeAssetMapper;
import kr.trademon.service.ICoinService;
import kr.trademon.service.IStockService;
import kr.trademon.service.ITradeAssetService;
import kr.trademon.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class TradeAssetService implements ITradeAssetService {

    private final ITradeAssetMapper tradeAssetMapper;
    private final ICoinService coinService;
    private final IStockService stockService;

    private static final BigDecimal INITIAL_CASH = BigDecimal.valueOf(50000000);

    // ✅ 초당 2회 호출 제한
    private final RateLimiter rateLimiter = RateLimiter.create(2.0);

    @Override
    public int insertTrade(TradeAssetDTO pDTO) throws Exception {
        log.info("▶ 거래 저장 시작");
        return tradeAssetMapper.insertTradeAsset(pDTO);
    }

    @Override
    public BigDecimal getTotalQuantityUntil(String userEmail, String assetCode, String tradeType, String untilTime) {
        return safe(tradeAssetMapper.sumQuantityUntil(userEmail, assetCode, tradeType, untilTime));
    }

    @Override
    public BigDecimal getRealizedProfit(String userEmail) throws Exception {
        return safe(tradeAssetMapper.sumRealizedProfit(userEmail));
    }

    @Override
    public Map<String, Object> getAssetSummary(String userEmail) throws Exception {
        log.info("▶ 자산 요약 계산 시작");

        Map<String, Object> rMap = new HashMap<>();
        List<TradeAssetDTO> holdings = tradeAssetMapper.getCurrentHoldings(userEmail);
        Map<String, BigDecimal> priceMap = getCurrentPricesForHoldings(holdings);

        BigDecimal totalBuyRaw = safe(tradeAssetMapper.sumTotalBuyAmount(userEmail));
        BigDecimal totalSellRaw = safe(tradeAssetMapper.sumTotalSellAmount(userEmail));
        BigDecimal realizedProfit = safe(tradeAssetMapper.sumRealizedProfit(userEmail));

        BigDecimal totalEval = BigDecimal.ZERO;
        BigDecimal totalBuy = BigDecimal.ZERO;

        for (TradeAssetDTO asset : holdings) {
            String code = asset.getAssetCode();
            BigDecimal totalBuyQty = safe(tradeAssetMapper.sumQuantityForAsset(userEmail, code, "BUY"));
            BigDecimal totalSellQty = safe(tradeAssetMapper.sumQuantityForAsset(userEmail, code, "SELL"));
            BigDecimal currentQty = totalBuyQty.subtract(totalSellQty); // 👉 순 보유 수량

            if (currentQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal price = priceMap.getOrDefault(code, BigDecimal.ZERO);
            totalEval = totalEval.add(currentQty.multiply(price));

            BigDecimal avgBuyPrice = safe(tradeAssetMapper.sumBuyAmountForAsset(userEmail, code))
                    .divide(totalBuyQty, 10, RoundingMode.HALF_UP);

            BigDecimal currentBuyAmount = avgBuyPrice.multiply(currentQty);
            totalBuy = totalBuy.add(currentBuyAmount);
        }


        BigDecimal availableCash = INITIAL_CASH.subtract(totalBuyRaw).add(totalSellRaw);
        BigDecimal totalAsset = availableCash.add(totalEval);
        BigDecimal unrealizedProfit = totalEval.subtract(totalBuy);
        BigDecimal totalProfit = realizedProfit.add(unrealizedProfit);

        BigDecimal rate = totalBuyRaw.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalBuyRaw, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        rMap.put("totalAsset", totalAsset);
        rMap.put("availableCash", availableCash);
        rMap.put("totalEval", totalEval);
        rMap.put("unrealizedProfit", unrealizedProfit);
        rMap.put("realizedProfit", realizedProfit);
        rMap.put("rate", rate);
        rMap.put("totalBuy", totalBuy);

        return rMap;
    }

    @Override
    public void deleteAllTrades(String userEmail) throws Exception {
        log.info("🗑️ 거래 초기화 시작 - 사용자: {}", userEmail);
        tradeAssetMapper.deleteAllByUserEmail(userEmail);
    }

    private Map<String, BigDecimal> getCurrentPricesForHoldings(List<TradeAssetDTO> holdings) {
        Map<String, BigDecimal> priceMap = new HashMap<>();

        for (TradeAssetDTO asset : holdings) {
            String code = asset.getAssetCode();
            String type = asset.getAssetType();

            if (!priceMap.containsKey(code)) {
                try {
                    log.info("📌 가격 조회 시작 - 종목코드: {}", code);
                    BigDecimal price = retryableGetCurrentMarketPrice(code, type);
                    priceMap.put(code, price);
                } catch (Exception e) {
                    log.error("❌ 가격 조회 실패 - {}: {}", code, e.getMessage());
                }
            }
        }

        return priceMap;
    }


    private BigDecimal retryableGetCurrentMarketPrice(String code, String type) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if ("COIN".equals(type)) {
                    String coinPrice = coinService.getCoinPrice(code);
                    return new BigDecimal(coinPrice);
                }

                // ✅ 호출 전에 acquire
                rateLimiter.acquire();

                String token = stockService.getKisAccessToken();
                StockInfoDTO dto = stockService.getKisStockInfo(code, token);

                if (dto != null && dto.getCurrentPrice() != null && !dto.getCurrentPrice().isBlank()) {
                    log.info("✅ [getPrice] {} 현재가: {}", code, dto.getCurrentPrice());
                    return new BigDecimal(dto.getCurrentPrice());
                }

                log.warn("❗ 재시도 {}회 - 현재가 없음: {}, 응답 DTO: {}", attempt, code, dto);
                Thread.sleep(300L * attempt); // ⏳ 점진적 백오프

            } catch (Exception e) {
                log.error("❌ 예외 - {}: {}", code, e.getMessage());
            }
        }

        log.warn("⚠️ [최종실패] 현재가 조회 실패 - {}", code);
        return BigDecimal.ZERO;
    }


    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public List<Map<String, Object>> getHoldingRatio(String userEmail) throws Exception {
        List<TradeAssetDTO> holdings = tradeAssetMapper.getCurrentHoldings(userEmail);
        Map<String, BigDecimal> priceMap = getCurrentPricesForHoldings(holdings);

        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal totalEval = BigDecimal.ZERO;
        Map<String, BigDecimal> evalMap = new HashMap<>();

        for (TradeAssetDTO asset : holdings) {
            BigDecimal qty = safe(asset.getQuntity());
            BigDecimal eval = qty.multiply(priceMap.getOrDefault(asset.getAssetCode(), BigDecimal.ZERO));
            totalEval = totalEval.add(eval);
            evalMap.put(asset.getAssetName(), eval);
        }

        BigDecimal availableCash = INITIAL_CASH
                .subtract(safe(tradeAssetMapper.sumTotalBuyAmount(userEmail)))
                .add(safe(tradeAssetMapper.sumTotalSellAmount(userEmail)));

        totalEval = totalEval.add(availableCash);

        for (Map.Entry<String, BigDecimal> entry : evalMap.entrySet()) {
            result.add(Map.of("name", entry.getKey(), "value", entry.getValue()));
        }

        if (availableCash.compareTo(BigDecimal.ZERO) > 0) {
            result.add(Map.of("name", "현금", "value", availableCash));
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getProfitRatio(String userEmail) throws Exception {
        List<TradeAssetDTO> holdings = tradeAssetMapper.getCurrentHoldings(userEmail);
        Map<String, BigDecimal> priceMap = getCurrentPricesForHoldings(holdings);

        List<Map<String, Object>> result = new ArrayList<>();

        for (TradeAssetDTO asset : holdings) {
            BigDecimal qty = safe(asset.getQuntity());
            BigDecimal curPrice = priceMap.getOrDefault(asset.getAssetCode(), BigDecimal.ZERO);
            BigDecimal eval = qty.multiply(curPrice);



            if (curPrice.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("⚠️ 현재가가 0으로 수익률 계산 불가: {} ({})", asset.getAssetName(), asset.getAssetCode());
                continue;
            }

            BigDecimal totalBuyAmount = safe(tradeAssetMapper.sumBuyAmountForAsset(userEmail, asset.getAssetCode()));
            BigDecimal totalBuyQty = safe(tradeAssetMapper.sumQuantityForAsset(userEmail, asset.getAssetCode(), "BUY"));

            BigDecimal avgBuyPrice = totalBuyQty.compareTo(BigDecimal.ZERO) > 0
                    ? totalBuyAmount.divide(totalBuyQty, 10, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal relevantBuyAmount = avgBuyPrice.multiply(qty);
            BigDecimal profit = eval.subtract(relevantBuyAmount);
            BigDecimal ratio = relevantBuyAmount.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(BigDecimal.valueOf(100)).divide(relevantBuyAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;


            log.info("📊 [{}] 수익률 계산", asset.getAssetName());
            log.info("   ⦿ 보유 수량: {}", qty);
            log.info("   ⦿ 현재가: {}", curPrice);
            log.info("   ⦿ 평가 금액: {}", eval);
            log.info("   ⦿ 총 매입 금액: {}", relevantBuyAmount);

            log.info("   ⦿ 미실현 수익: {}", profit);
            log.info("   ⦿ 수익률: {}%", ratio);

            result.add(Map.of("name", asset.getAssetName(), "value", ratio));

        }

        return result;
    }

    @Override
    public LinkedHashMap<String, BigDecimal> getDailyAssetHistory(String userEmail) throws Exception {
        log.info("📈 [getDailyAssetHistory] 일별 자산 변화 추이 계산 시작 - 사용자: {}", userEmail);

        List<Map<String, Object>> dailyList = tradeAssetMapper.getDailyAssetSummary(userEmail);
        LinkedHashMap<String, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal cumulative = INITIAL_CASH;

        for (Map<String, Object> item : dailyList) {
            String date = item.get("tradeDate").toString();
            BigDecimal delta = (BigDecimal) item.get("amountChange");
            cumulative = cumulative.add(delta);
            result.put(date, cumulative);
        }

        return result;
    }

    @Override
    public List<TradeAssetDTO> getTradeHistory(String userEmail) throws Exception {
        String encEmail = EncryptUtil.encAES128CBC(userEmail); // ✅ 암호화
        return tradeAssetMapper.getTradeHistory(encEmail);
    }

    @Override
    public List<TradeAssetDTO> getTradeHistoryByType(String userEmail, String tradeType) throws Exception {
        String encEmail = EncryptUtil.encAES128CBC(userEmail);
        return tradeAssetMapper.getTradeHistoryByType(encEmail, tradeType);
    }

    @Override
    public BigDecimal sumQuantityForAsset(String userEmail, String assetCode, String tradeType) {
        return safe(tradeAssetMapper.sumQuantityForAsset(userEmail, assetCode, tradeType));
    }

}

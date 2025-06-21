package kr.trademon.mapper;

import kr.trademon.dto.TradeAssetDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ITradeAssetMapper {

    // 거래 저장
    int insertTradeAsset(TradeAssetDTO pDTO);

    // 특정 시점까지의 수량 합계 (백테스트용)
    BigDecimal sumQuantityUntil(@Param("userEmail") String userEmail,
                                @Param("assetCode") String assetCode,
                                @Param("tradeType") String tradeType,
                                @Param("untilTime") String untilTime);

    // ✅ 특정 종목의 매수 금액 합계 (보유 종목 평가용)
    BigDecimal sumBuyAmountForAsset(@Param("userEmail") String userEmail,
                                    @Param("assetCode") String assetCode);

    // ✅ 전체 매수 금액 합계 (현금 흐름 계산용) ← 누락되었던 부분 추가
    BigDecimal sumTotalBuyAmount(@Param("userEmail") String userEmail);

    // ✅ 전체 매도 금액 합계
    BigDecimal sumTotalSellAmount(@Param("userEmail") String userEmail);

    // ✅ 보유 수량이 남아있는 종목 리스트 (잔고 기준)
    List<TradeAssetDTO> getCurrentHoldings(@Param("userEmail") String userEmail);

    void deleteAllByUserEmail(@Param("userEmail") String userEmail);


    BigDecimal sumRealizedProfit(@Param("userEmail") String userEmail);

    List<TradeAssetDTO> getAllTradesByUser(@Param("userEmail") String userEmail);

    // 날짜별 자산 평가금액 합계 조회 (예: 일자별 시세 총합)
    List<Map<String, Object>> getDailyAssetSummary(@Param("userEmail") String userEmail);

    List<TradeAssetDTO> getTradeHistory(String userEmail);

    List<TradeAssetDTO> getTradeHistoryByType(String userEmail, String tradeType);

    BigDecimal sumQuantityForAsset(@Param("userEmail") String userEmail,
                                   @Param("assetCode") String assetCode,
                                   @Param("tradeType") String tradeType);

    BigDecimal getEffectiveBuyAmount(@Param("userEmail") String userEmail,
                                     @Param("assetCode") String assetCode);
    List<TradeAssetDTO> getAllBuyTradesForAsset(@Param("userEmail") String userEmail,
                                                @Param("assetCode") String assetCode);

    BigDecimal getTotalSellQuantity(@Param("userEmail") String userEmail,
                                    @Param("assetCode") String assetCode);


}

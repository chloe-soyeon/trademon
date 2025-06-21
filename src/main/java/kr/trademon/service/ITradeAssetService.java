package kr.trademon.service;

import kr.trademon.dto.TradeAssetDTO;
import org.apache.ibatis.annotations.Param;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public interface ITradeAssetService {

    // 거래 저장
    int insertTrade(TradeAssetDTO pDTO) throws Exception;

    // 특정 시점까지의 총 수량 (백테스트용)
    BigDecimal getTotalQuantityUntil(String userEmail, String assetCode, String tradeType, String untilTime);

    // 대시보드 자산 요약 정보
    Map<String, Object> getAssetSummary(String userEmail) throws Exception;

    // 거래 전체 초기화
    void deleteAllTrades(String userEmail) throws Exception;

    // 실현 손익 계산
    BigDecimal getRealizedProfit(String userEmail) throws Exception;

    // 날짜별 총 자산 변화 추이 (차트용)
    LinkedHashMap<String, BigDecimal> getDailyAssetHistory(String userEmail) throws Exception;

    // 종목별 보유 평가금액 비율 (파이차트용)
    List<Map<String, Object>> getHoldingRatio(String userEmail) throws Exception;

    // 종목별 수익률 계산 (수익 분석 바 차트용)
    List<Map<String, Object>> getProfitRatio(String userEmail) throws Exception;

    List<TradeAssetDTO> getTradeHistory(String userEmail) throws Exception;

    List<TradeAssetDTO> getTradeHistoryByType(String userEmail, String tradeType) throws Exception;

    BigDecimal sumQuantityForAsset(String userEmail, String assetCode, String tradeType);

}

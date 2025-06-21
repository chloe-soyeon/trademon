package kr.trademon.service;

import kr.trademon.dto.CandleDTO;
import kr.trademon.dto.CorpInfoDTO;
import kr.trademon.dto.DisclosureDTO;
import kr.trademon.dto.StockInfoDTO;

import java.util.List;

public interface IStockService {

    /**
     * 🔍 CORPCODE.xml 기반 기업명 자동완성 검색
     */
    List<CorpInfoDTO> searchCorpNames(String keyword);

    /**
     * 📄 OpenDART 전체 공시 조회 (페이지 반복 포함)
     *    + 📈 KRX API를 통한 종목 정보 조회 로그 포함
     */
    List<DisclosureDTO> getDisclosures(String corpName, String startDate, String endDate);

    /**
     * 📈 금융위원회 KRX API를 통해 종목 정보(itmsNm, srtnCd) 조회
     *    (자동 로그 출력 전용)
     */
    String fetchKrxStockInfo(String corpName);

    /**
     * 🛡️ 한국투자증권 API 토큰 발급
     */
    String getKisAccessToken();

    StockInfoDTO getKisStockInfo(String stockCode, String accessToken);
    /**
     * 📈 한국투자증권 API로 일자별 시세 캔들 차트 조회
     */
    List<CandleDTO> getKisCandleChart(String stockCode, String accessToken);
}

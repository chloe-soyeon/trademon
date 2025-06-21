package kr.trademon.service;

import kr.trademon.dto.CoinNameCodeDTO;
import kr.trademon.dto.CoinCandleDTO;

import java.util.List;
import java.util.Map;

public interface ICoinService {

    /**
     * 업비트에서 코인 현재가를 조회합니다.
     * @param coinCode 예: "KRW-BTC"
     * @return 현재가 (문자열 형식)
     */
    String getCoinPrice(String coinCode);

    /**
     * 업비트에서 코인 일봉 캔들차트 데이터를 조회합니다.
     * @param coinCode 예: "KRW-BTC"
     * @return 캔들차트 데이터 리스트
     */
    List<CoinCandleDTO> getCandleChart(String coinCode);

    /**
     * 암호화폐 이름으로 검색하여 이름과 코드 정보를 반환합니다.
     * @param query 검색어
     * @return 검색된 암호화폐 이름과 코드 정보 리스트
     */
    List<CoinNameCodeDTO> searchCoinByName(String query);


    List<CoinNameCodeDTO> getTopCoinNameCodes(int limit);


    /**
     * 한화 마켓(KRW) 기준 상위 코인들의 코드, 심볼, 이름, 시세를 반환합니다.
     * @param limit 조회할 코인 개수 (예: 20)
     * @return 각 코인의 코드, 심볼, 이름, 시세 정보 리스트
     */
    List<Map<String, String>> getTopCoinPrices(int limit);




}
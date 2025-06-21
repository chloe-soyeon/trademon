package kr.trademon.controller;

import kr.trademon.dto.CoinCandleDTO;
import kr.trademon.dto.CoinNameCodeDTO;
import kr.trademon.service.ICoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/coin")
public class CoinController {

    private final ICoinService coinService;

    /**
     * 코인 차트 뷰 페이지로 이동
     */
    @GetMapping("/view")
    public String coinView(@RequestParam(defaultValue = "KRW-BTC") String coinCode, Model model) {
        model.addAttribute("coinCode", coinCode);
        return "coin/coin"; // 👉 coin.jsp or coin.html
    }

    /**
     * 코인 현재가 조회 API
     */
    @GetMapping("/price")
    @ResponseBody
    public String getCoinPrice(@RequestParam String coinCode) {
        log.info("💰 [getCoinPrice] 요청된 코인: {}", coinCode);
        return coinService.getCoinPrice(coinCode);
    }

    /**
     * 코인 캔들차트 데이터 조회 API
     */
    @GetMapping("/candle")
    @ResponseBody
    public List<CoinCandleDTO> getCandleChart(@RequestParam String coinCode) {
        log.info("📊 [getCoinChart] 요청된 코인: {}", coinCode);
        return coinService.getCandleChart(coinCode);
    }

    /**
     * 암호화폐 이름 검색 API
     */
    @GetMapping("/api/crypto/search")
    @ResponseBody
    public List<CoinNameCodeDTO> searchCrypto(@RequestParam String query) {
        log.info("🔍 [searchCrypto] 검색어: {}", query);
        return coinService.searchCoinByName(query);
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, String> getCoinTickerList() {
        return coinService.getTopCoinNameCodes(20)
                .stream()
                .collect(Collectors.toMap(
                        CoinNameCodeDTO::getCode,
                        CoinNameCodeDTO::getName,
                        (oldValue, newValue) -> oldValue // 중복 키 허용 처리
                ));
    }


    @GetMapping("/prices")
    @ResponseBody
    public List<Map<String, String>> getTopCoinPrices() {
        return coinService.getTopCoinPrices(20); // 20개 코인 이름+심볼+시세 한 번에 응답
    }





}
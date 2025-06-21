package kr.trademon.controller;

import kr.trademon.dto.CandleDTO;
import kr.trademon.dto.CorpInfoDTO;
import kr.trademon.dto.DisclosureDTO;
import kr.trademon.dto.StockInfoDTO;
import kr.trademon.service.IStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stock")
public class StockController {

    private final IStockService StockService;

    // 📄 HTML 뷰 반환
    @GetMapping("/view")
    public String viewPage() {
//        return "disclosure/stock";
        return "disclosure/stock";
    }

    // 🔍 기업명 자동완성
    @ResponseBody
    @GetMapping("/search")
    public List<CorpInfoDTO> search(@RequestParam String keyword) {
        return StockService.searchCorpNames(keyword);
    }

    // 📑 공시 리스트 (전체 반복조회)
    @ResponseBody
    @GetMapping("/list")
    public List<DisclosureDTO> list(@RequestParam String corpName,
                                    @RequestParam String startDate,
                                    @RequestParam String endDate) {
        return StockService.getDisclosures(corpName, startDate, endDate);
    }

    /// 📈 한국투자증권 API 투자지표 조회
    @ResponseBody
    @GetMapping("/stockinfo")
    public StockInfoDTO getStockInfo(@RequestParam String stockCode) {
        String accessToken = StockService.getKisAccessToken();
        return StockService.getKisStockInfo(stockCode, accessToken);
    }
    // 📊 KRX 종목코드 조회 (srtnCd → 'A' 제거된 코드 반환)
    @GetMapping("/krx")
    @ResponseBody
    public Map<String, Object> getStockCode(@RequestParam String corpName) {
        String stockCode = StockService.fetchKrxStockInfo(corpName);

        if (stockCode == null) {
            return Map.of(
                    "status", "fail",
                    "message", corpName + "에 대한 검색 결과가 없습니다.",
                    "hint", """
                        - 단어의 철자가 정확한지 확인해 주세요.
                        - 영문 이름의 경우 한글로 검색해 주세요.
                        """
            );
        }

        return Map.of(
                "status", "success",
                "stockCode", stockCode
        );
    }

    @GetMapping("/candlechart")
    @ResponseBody
    public List<CandleDTO> getCandleChart(@RequestParam String stockCode) {
        String accessToken = StockService.getKisAccessToken();
        return StockService.getKisCandleChart(stockCode, accessToken);
    }



}

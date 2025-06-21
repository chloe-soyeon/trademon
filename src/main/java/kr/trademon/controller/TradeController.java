package kr.trademon.controller;

import jakarta.servlet.http.HttpSession;
import kr.trademon.dto.TradeAssetDTO;
import kr.trademon.service.ITradeAssetService;
import kr.trademon.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final ITradeAssetService tradeAssetService;

    @GetMapping("/availableQty")
    @ResponseBody
    public Map<String, Object> getAvailableQty(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email,
            @RequestParam String assetCode) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        BigDecimal totalBuy = tradeAssetService.getTotalQuantityUntil(userEmailEnc, assetCode, "BUY", now);
        BigDecimal totalSell = tradeAssetService.getTotalQuantityUntil(userEmailEnc, assetCode, "SELL", now);
        BigDecimal available = totalBuy.subtract(totalSell);

        res.put("status", "success");
        res.put("availableQty", available.stripTrailingZeros().toPlainString());
        return res;
    }

    @PostMapping("/execute")
    public Map<String, Object> executeTrade(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email,
            @RequestBody TradeAssetDTO pDTO) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            log.error("❌ 세션 없음 또는 로그인 정보 누락");
            res.put("status", "fail");
            res.put("message", "로그인된 사용자만 거래할 수 있습니다.");
            res.put("redirectUrl", "/user/login");
            return res;
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String userEmailEnc = EncryptUtil.encAES128CBC(email);

        pDTO.setTradeId(UUID.randomUUID().toString());
        pDTO.setUserEmail(userEmailEnc);
        pDTO.setTradeTime(now);

        // ✅ 매수 거래 시 보유 현금 체크
        if ("BUY".equalsIgnoreCase(pDTO.getTradeType())) {
            Map<String, Object> summary = tradeAssetService.getAssetSummary(userEmailEnc);
            BigDecimal availableCash = new BigDecimal(summary.get("availableCash").toString());

            BigDecimal orderAmount = pDTO.getQuntity().multiply(pDTO.getPrice());
            log.info("💰 보유 현금: {}, 주문 금액: {}", availableCash, orderAmount);

            if (orderAmount.compareTo(availableCash) > 0) {
                log.warn("❌ 매수 실패 - 보유 현금 초과");
                res.put("status", "fail");
                res.put("message", "보유 현금(" + availableCash.toPlainString() + "원)보다 많은 금액을 매수할 수 없습니다.");
                return res;
            }
        }

        // ✅ 매도 거래 시 보유 수량 체크
        if ("SELL".equalsIgnoreCase(pDTO.getTradeType())) {
            BigDecimal totalBuyQty = tradeAssetService.getTotalQuantityUntil(userEmailEnc, pDTO.getAssetCode(), "BUY", now);
            BigDecimal totalSellQty = tradeAssetService.getTotalQuantityUntil(userEmailEnc, pDTO.getAssetCode(), "SELL", now);
            BigDecimal availableQty = totalBuyQty.subtract(totalSellQty);

            log.info("📊 보유수량 검사: 매수 {}, 매도 {}, 가능 {}", totalBuyQty, totalSellQty, availableQty);

            if (pDTO.getQuntity().compareTo(availableQty) > 0) {
                log.warn("❌ 매도 실패 - 보유 수량 초과 (보유: {}, 요청: {})", availableQty, pDTO.getQuntity());
                res.put("status", "fail");
                res.put("message", "보유 수량(" + availableQty.toPlainString() + "개)보다 많은 수량을 매도할 수 없습니다.");
                return res;
            }
        }

        log.info("💸 거래 요청: {}", pDTO);
        int result = tradeAssetService.insertTrade(pDTO);

        res.put("status", "success");
        res.put("message", "정상적으로 거래가 처리되었습니다.");
        res.put("result", result);
        return res;
    }


    /**
     * ✅ 사용자 자산 요약 정보 (대시보드용)
     */
    @GetMapping("/summary")
    public Map<String, Object> getAssetSummary(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);

        try {
            Map<String, Object> summary = tradeAssetService.getAssetSummary(userEmailEnc);

            res.put("status", "success");
            res.put("summary", summary);
        } catch (Exception e) {
            log.error("❌ [getAssetSummary] 자산 요약 실패: {}", e.getMessage());
            res.put("status", "fail");
            res.put("message", "자산 정보를 불러오는 데 실패했습니다.");
        }

        return res;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetTradeHistory(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);
        tradeAssetService.deleteAllTrades(userEmailEnc);

        res.put("status", "success");
        res.put("message", "모의 투자 내역이 초기화되었습니다.");
        return res;
    }

    @GetMapping("/asset-history")
    public Map<String, Object> getAssetHistory(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);

        try {
            Map<String, BigDecimal> dailyHistory = tradeAssetService.getDailyAssetHistory(userEmailEnc);
            res.put("status", "success");
            res.put("history", dailyHistory);  // { "2025-05-01": 50123400, ... }
        } catch (Exception e) {
            log.error("❌ [getAssetHistory] 자산 추이 조회 실패: {}", e.getMessage());
            res.put("status", "fail");
            res.put("message", "자산 추이 데이터를 불러오는 데 실패했습니다.");
        }

        return res;
    }

    @GetMapping("/holding-ratio")
    public Map<String, Object> getHoldingRatio(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);

        try {
            List<Map<String, Object>> ratioList = tradeAssetService.getHoldingRatio(userEmailEnc);
            res.put("status", "success");
            res.put("holdings", ratioList);
        } catch (Exception e) {
            log.error("❌ [getHoldingRatio] 보유 비중 조회 실패: {}", e.getMessage());
            res.put("status", "fail");
            res.put("message", "보유 자산 비중 데이터를 불러오는 데 실패했습니다.");
        }

        return res;
    }

    @GetMapping("/profit-ratio")
    public Map<String, Object> getProfitRatio(
            @SessionAttribute(name = "SS_USER_EMAIL", required = false) String email) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (email == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        String userEmailEnc = EncryptUtil.encAES128CBC(email);

        try {
            List<Map<String, Object>> ratioList = tradeAssetService.getProfitRatio(userEmailEnc);
            res.put("status", "success");
            res.put("data", ratioList); // 📌 프런트에서 'data' 키로 받음
        } catch (Exception e) {
            log.error("❌ [getProfitRatio] 수익률 조회 실패: {}", e.getMessage());
            res.put("status", "fail");
            res.put("message", "수익률 데이터를 불러오는 데 실패했습니다.");
        }

        return res;
    }

    @GetMapping("/history")
    @ResponseBody
    public Map<String, Object> getTradeHistory(HttpSession session) throws Exception {
        String userEmail = (String) session.getAttribute("SS_USER_EMAIL");

        Map<String, Object> res = new HashMap<>();
        List<TradeAssetDTO> history = tradeAssetService.getTradeHistory(userEmail);

        res.put("status", "success");
        res.put("history", history);
        return res;
    }


    @GetMapping(value = "/history", params = "type")
    @ResponseBody
    public Map<String, Object> getTradeHistoryByType(HttpSession session,
                                                     @RequestParam String type) throws Exception {
        String userEmail = (String) session.getAttribute("SS_USER_EMAIL");
        Map<String, Object> res = new HashMap<>();

        if (userEmail == null) {
            res.put("status", "fail");
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        List<TradeAssetDTO> history = tradeAssetService.getTradeHistoryByType(userEmail, type.toUpperCase());

        res.put("status", "success");
        res.put("history", history);
        return res;
    }







}

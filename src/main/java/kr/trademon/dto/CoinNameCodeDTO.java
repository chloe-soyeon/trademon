package kr.trademon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoinNameCodeDTO {
    private String name; // ex: 비트코인
    private String code; // ex: KRW-BTC

    // ✅ "KRW-BTC" → "BTC" 반환
    public String getSymbol() {
        if (code != null && code.contains("-")) {
            return code.split("-")[1];
        }
        return "";
    }
}

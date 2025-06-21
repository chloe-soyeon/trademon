package kr.trademon.dto;

import lombok.Data;

import java.math.BigDecimal;

import java.math.BigDecimal;

@Data
public class TradeAssetDTO {
    private String tradeId;
    private String userEmail;
    private String assetType;
    private String assetCode;
    private String assetName;
    private String tradeType;
    private BigDecimal quntity;  // 변경
    private BigDecimal price;    // 변경
    private String tradeTime;
}

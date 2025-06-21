package kr.trademon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfoDTO {
    private String stckPrpr;
    private String stckOprc;
    private String stckHgpr;
    private String stckLwpr;
    private String per;
    private String eps;
    private String pbr;
    private String bps;
    private String w52Hgpr;
    private String w52Lwpr;
    private String htsFrgnEhrt;
    private String currentPrice; // 현재가
    private String prdyVrss;       // 전일 대비
    private String prdyVrssSign;   // 부호 (1:상승, 2:하락, 3:보합)
    private String prdyCtrt;       // 전일 대비율



}

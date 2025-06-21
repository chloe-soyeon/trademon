package kr.trademon.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CoinCandleDTO {
    private long time; // Unix timestamp (초 단위)
    private double open;
    private double high;
    private double low;
    private double close;
}

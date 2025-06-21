package kr.trademon.dto;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class CandleDTO {
    private String time;     // yyyy-MM-dd 형식 (차트용)
    private double open;
    private double high;
    private double low;
    private double close;
}

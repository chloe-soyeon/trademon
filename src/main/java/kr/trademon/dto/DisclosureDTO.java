package kr.trademon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisclosureDTO {
    private String reportName;
    private String receiptNo;
    private String receiptDate;
    private String url;

    // ✅ 종목코드 추가
    private String stockCode;
}

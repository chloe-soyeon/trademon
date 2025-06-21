package kr.trademon.dto;

import lombok.Data;

import java.util.Date;

@Data
public class NewsDTO {

    private String userEmail;     // 이메일 (FK)
    private String newsId;        // 뉴스 ID (PK)
    private String title;         // 뉴스 제목
    private String url;           // 뉴스 URL
    private String note;          // 메모
    private Date scrapTime;       // 스크랩 시간
}

package kr.trademon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WordDTO {

    private String searchWord;       // 사용자가 입력한 검색어
    private String wordTitle;        // 용어명
    private String wordExplanation;  // 용어 설명
}

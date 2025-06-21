package kr.trademon.service;

import kr.trademon.dto.NewsDTO;

import java.util.List;

public interface INewsService {

    // 네이버 뉴스 검색
    List<NewsDTO> searchNews(String keyword) throws Exception;

    // 뉴스 스크랩 저장
    int saveScrap(NewsDTO pDTO) throws Exception;

    // 즐겨찾기 리스트 가져오기
    List<NewsDTO> getScrapList(String userEmail) throws Exception;

    // 스크랩 삭제
    int deleteScrap(NewsDTO pDTO) throws Exception;
}

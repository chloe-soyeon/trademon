package kr.trademon.service;

import kr.trademon.dto.WordDTO;

public interface IWordService {
    WordDTO searchWord(String word) throws Exception;
}

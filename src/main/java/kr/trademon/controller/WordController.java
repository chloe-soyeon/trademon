package kr.trademon.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.trademon.dto.WordDTO;
import kr.trademon.service.IWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WordController {

    private final IWordService wordService;

    /**
     * 한국은행 용어 검색 결과 화면
     * @param query 검색어
     * @param model 템플릿 렌더링용 모델
     * @return dictionary.html
     */
    @GetMapping("/word/search")
    public String searchWord(@RequestParam(value = "query", required = false) String query, Model model) {
        if (query == null || query.isBlank()) {
            return "dictionary/dictionary"; // 빈 화면만 보여줌
        }

        try {
            WordDTO result = wordService.searchWord(query);
            if (result == null) {
                model.addAttribute("errorMsg", "검색 결과가 없습니다.");
            } else {
                model.addAttribute("word", result);
            }
        } catch (Exception e) {
            model.addAttribute("errorMsg", "API 호출 중 오류가 발생했습니다.");
        }

        return "dictionary/dictionary";
    }

}

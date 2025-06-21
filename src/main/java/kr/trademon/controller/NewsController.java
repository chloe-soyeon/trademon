package kr.trademon.controller;

import jakarta.servlet.http.HttpSession;
import kr.trademon.dto.NewsDTO;
import kr.trademon.service.INewsService;
import kr.trademon.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final INewsService newsService;

    // ✅ 검색창만 보여주는 GET
    @GetMapping("")
    public String newsPage() {
        return "news/newsResult";
    }

    // ✅ 검색어 있을 때만 결과 출력
    @GetMapping("/search")
    public String searchNews(@RequestParam(value = "keyword", required = false) String keyword, Model model) throws Exception {
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<NewsDTO> newsList = newsService.searchNews(keyword);
            model.addAttribute("newsList", newsList);
        }
        return "news/newsResult";
    }

    // ✅ 스크랩 저장 (AES128-CBC 방식으로 이메일 암호화 → 서비스 내부 처리)
    @PostMapping("/scrap")
    @ResponseBody
    public ResponseEntity<String> scrapNews(@RequestBody NewsDTO pDTO, HttpSession session) throws Exception {
        String userEmail = (String) session.getAttribute("SS_USER_EMAIL");

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 후 이용 가능합니다.");
        }

        // 평문 이메일 설정 → 서비스에서 AES 암호화 적용됨
        pDTO.setUserEmail(userEmail);
        newsService.saveScrap(pDTO);
        return ResponseEntity.ok("success");
    }

    @GetMapping("/scrapList")
    public String getScrapList(HttpSession session, Model model) throws Exception {
        String userEmail = (String) session.getAttribute("SS_USER_EMAIL");

        if (userEmail == null) {
            model.addAttribute("msg", "로그인 후 이용 가능합니다.");
            return "redirect:/user/login";
        }

        // ⛔ 여기서는 암호화하지 말고, 서비스에서 하도록 그대로 넘김!
        List<NewsDTO> scrapList = newsService.getScrapList(userEmail);

        model.addAttribute("scrapList", scrapList);
        return "news/scrapList";
    }
    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<String> deleteScrap(@RequestBody NewsDTO pDTO, HttpSession session) throws Exception {
        String userEmail = (String) session.getAttribute("SS_USER_EMAIL");

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String encEmail = EncryptUtil.encAES128CBC(userEmail);
        pDTO.setUserEmail(encEmail);

        newsService.deleteScrap(pDTO); // newsId와 userEmail로 삭제
        return ResponseEntity.ok("deleted");
    }



}

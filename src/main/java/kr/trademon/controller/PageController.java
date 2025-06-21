package kr.trademon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard/dashboard"; // templates/dashboard/dashboard.html
    }
}

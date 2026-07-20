package com.master.oauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GuideController {

    @Value("${server.port:8080}")
    private String serverPort;

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("baseUrl", "http://localhost:" + serverPort);
        return "guide";
    }
}

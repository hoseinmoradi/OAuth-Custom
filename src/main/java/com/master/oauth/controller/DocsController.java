package com.master.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {

    @GetMapping({"/docs", "/docs/", "/redoc", "/swagger", "/api-docs"})
    public String docs() {
        return "redirect:/docs/index.html";
    }
}

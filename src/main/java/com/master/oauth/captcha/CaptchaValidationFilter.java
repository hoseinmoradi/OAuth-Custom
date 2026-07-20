package com.master.oauth.captcha;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Validates captcha on POST /login before Spring Security authenticates credentials.
 */
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final AntPathRequestMatcher loginMatcher =
            new AntPathRequestMatcher("/login", "POST");

    public CaptchaValidationFilter(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (captchaService.isEnabled() && loginMatcher.matches(request)) {
            String captcha = request.getParameter("captcha");
            if (!captchaService.validate(request.getSession(false), captcha)) {
                response.sendRedirect(request.getContextPath() + "/login?error=captcha");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

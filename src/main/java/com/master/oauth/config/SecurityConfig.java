package com.master.oauth.config;

import com.master.oauth.auth.PluggableAuthenticationProvider;
import com.master.oauth.captcha.CaptchaService;
import com.master.oauth.captcha.CaptchaValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PluggableAuthenticationProvider authenticationProvider;
    private final CaptchaService captchaService;

    public SecurityConfig(PluggableAuthenticationProvider authenticationProvider,
                          CaptchaService captchaService) {
        this.authenticationProvider = authenticationProvider;
        this.captchaService = captchaService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatchers()
                .antMatchers(
                        "/",
                        "/home",
                        "/login",
                        "/logout",
                        "/guide",
                        "/captcha",
                        "/oauth/authorize",
                        "/oauth/consent",
                        "/oauth/confirm_access",
                        "/api/consent/**",
                        "/css/**",
                        "/js/**",
                        "/h2-console/**"
                )
                .and()
                .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/captcha", "/guide", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/home", false)
                .failureUrl("/login?error=credentials")
                .permitAll()
                .and()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll()
                .and()
                .csrf()
                .ignoringAntMatchers("/h2-console/**", "/api/consent/**", "/oauth/token", "/api/auth/**")
                .and()
                .headers()
                .frameOptions().sameOrigin()
                .and()
                .cors()
                .and()
                .addFilterBefore(new CaptchaValidationFilter(captchaService),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

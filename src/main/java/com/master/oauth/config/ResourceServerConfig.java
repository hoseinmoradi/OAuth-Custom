package com.master.oauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    public static final String RESOURCE_ID = "oauth-resource";

    private final TokenStore tokenStore;
    private final DefaultTokenServices tokenServices;

    public ResourceServerConfig(TokenStore tokenStore, DefaultTokenServices tokenServices) {
        this.tokenStore = tokenStore;
        this.tokenServices = tokenServices;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources
                .resourceId(RESOURCE_ID)
                .tokenStore(tokenStore)
                .tokenServices(tokenServices);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(
                        "/",
                        "/home",
                        "/login",
                        "/guide",
                        "/captcha",
                        "/oauth/**",
                        "/api/public/**",
                        "/api/auth/**",
                        "/docs",
                        "/docs/**",
                        "/redoc",
                        "/swagger",
                        "/api-docs",
                        "/openapi.yaml",
                        "/h2-console/**",
                        "/css/**",
                        "/js/**",
                        "/error"
                ).permitAll()
                .antMatchers(HttpMethod.GET, "/api/scopes", "/api/scopes/**").permitAll()
                .antMatchers("/api/users/**").access("#oauth2.hasScope('write')")
                .antMatchers(HttpMethod.POST, "/api/scopes/**").access("#oauth2.hasScope('write')")
                .antMatchers(HttpMethod.PUT, "/api/scopes/**").access("#oauth2.hasScope('write')")
                .antMatchers(HttpMethod.DELETE, "/api/scopes/**").access("#oauth2.hasScope('write')")
                .antMatchers("/api/admin/**").access("#oauth2.hasScope('write')")
                .antMatchers("/api/tokens/user/**").access("#oauth2.hasScope('write')")
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated();
    }
}

package com.master.oauth.config;

import com.master.oauth.approval.ScopeAwareOAuth2RequestFactory;
import com.master.oauth.approval.ScopeSelectingUserApprovalHandler;
import com.master.oauth.service.CustomUserDetailsService;
import com.master.oauth.service.TokenRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private final DataSource dataSource;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenEnhancer customTokenEnhancer;
    private final TokenRegistryService tokenRegistryService;

    @Value("${oauth.jwt.signing-key}")
    private String jwtSigningKey;

    @Value("${oauth.token.access-token-validity-seconds:3600}")
    private int accessTokenValidity;

    @Value("${oauth.token.refresh-token-validity-seconds:86400}")
    private int refreshTokenValidity;

    public AuthorizationServerConfig(DataSource dataSource,
                                     AuthenticationManager authenticationManager,
                                     CustomUserDetailsService userDetailsService,
                                     PasswordEncoder passwordEncoder,
                                     TokenEnhancer customTokenEnhancer,
                                     TokenRegistryService tokenRegistryService) {
        this.dataSource = dataSource;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.customTokenEnhancer = customTokenEnhancer;
        this.tokenRegistryService = tokenRegistryService;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security
                .tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()")
                .allowFormAuthenticationForClients()
                .passwordEncoder(passwordEncoder);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.jdbc(dataSource).passwordEncoder(passwordEncoder);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
        enhancerChain.setTokenEnhancers(Arrays.asList(customTokenEnhancer, accessTokenConverter()));

        ClientDetailsService clientDetailsService = endpoints.getClientDetailsService();
        if (clientDetailsService == null) {
            org.springframework.security.oauth2.provider.client.JdbcClientDetailsService jdbc =
                    new org.springframework.security.oauth2.provider.client.JdbcClientDetailsService(dataSource);
            jdbc.setPasswordEncoder(passwordEncoder);
            clientDetailsService = jdbc;
        }

        ScopeAwareOAuth2RequestFactory requestFactory =
                new ScopeAwareOAuth2RequestFactory(clientDetailsService, userDetailsService);

        ScopeSelectingUserApprovalHandler approvalHandler =
                new ScopeSelectingUserApprovalHandler(userDetailsService);
        approvalHandler.setApprovalStore(approvalStore());
        approvalHandler.setClientDetailsService(clientDetailsService);
        approvalHandler.setRequestFactory(requestFactory);

        endpoints
                .authenticationManager(authenticationManager)
                .userDetailsService(userDetailsService)
                .tokenStore(tokenStore())
                .accessTokenConverter(accessTokenConverter())
                .tokenEnhancer(enhancerChain)
                .tokenServices(tokenServices())
                .approvalStore(approvalStore())
                .userApprovalHandler(approvalHandler)
                .requestFactory(requestFactory)
                .pathMapping("/oauth/confirm_access", "/oauth/consent");
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(jwtSigningKey);
        return converter;
    }

    @Bean
    public ApprovalStore approvalStore() {
        return new JdbcApprovalStore(dataSource);
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        RevocableTokenServices tokenServices = new RevocableTokenServices(tokenRegistryService);
        tokenServices.setTokenStore(tokenStore());
        tokenServices.setSupportRefreshToken(true);
        tokenServices.setReuseRefreshToken(false);
        tokenServices.setAccessTokenValiditySeconds(accessTokenValidity);
        tokenServices.setRefreshTokenValiditySeconds(refreshTokenValidity);
        TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
        enhancerChain.setTokenEnhancers(Arrays.asList(customTokenEnhancer, accessTokenConverter()));
        tokenServices.setTokenEnhancer(enhancerChain);
        tokenServices.setAuthenticationManager(authenticationManager);
        tokenServices.setClientDetailsService(
                new org.springframework.security.oauth2.provider.client.JdbcClientDetailsService(dataSource));
        return tokenServices;
    }
}

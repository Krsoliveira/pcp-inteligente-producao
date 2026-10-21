package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.infrastructure.security.JwtAuthenticationFilter;
import com.krsoliveira.pcp.infrastructure.security.LimiteTamanhoCorpoFilter;
import com.krsoliveira.pcp.infrastructure.security.RateLimitingFilter;
import com.krsoliveira.pcp.infrastructure.security.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração central do Spring Security.
 *
 * <h3>Autenticação e autorização</h3>
 * <ul>
 *   <li>Sessões stateless — o estado fica no token, não no servidor.</li>
 *   <li>CSRF desabilitado — desnecessário em API REST com JWT.</li>
 *   <li>Rotas públicas: {@code /api/v1/auth/**} e Swagger UI.</li>
 *   <li>Demais rotas: exigem token JWT válido no cabeçalho {@code Authorization}.</li>
 *   <li>Requisições sem token retornam 401 (sem redirect para login).</li>
 * </ul>
 *
 * <h3>Headers de segurança HTTP</h3>
 * Além dos defaults do Spring Security (Cache-Control, X-Content-Type-Options,
 * X-Frame-Options, X-XSS-Protection), esta configuração adiciona explicitamente:
 * <ul>
 *   <li>{@code Strict-Transport-Security} — força HTTPS e impede downgrade attacks.</li>
 *   <li>{@code Referrer-Policy} — evita vazamento de URL nos logs de serviços externos.</li>
 *   <li>{@code Permissions-Policy} — desabilita APIs de browser não utilizadas pela aplicação.</li>
 * </ul>
 *
 * <h3>CORS</h3>
 * Origens permitidas definidas pela variável de ambiente {@code CORS_ORIGINS}
 * (separadas por vírgula). Padrão: {@code http://localhost:3000}.
 * Em produção, configure com a URL real do frontend antes do deploy.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracaoSeguranca {

    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtAuthenticationFilter jwtFilter;
    private final LimiteTamanhoCorpoFilter limiteTamanhoCorpoFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${CORS_ORIGINS:http://localhost:3000}")
    private String corsOriginsConfig;

    public ConfiguracaoSeguranca(UsuarioDetailsService usuarioDetailsService,
                                 JwtAuthenticationFilter jwtFilter,
                                 LimiteTamanhoCorpoFilter limiteTamanhoCorpoFilter,
                                 RateLimitingFilter rateLimitingFilter) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtFilter = jwtFilter;
        this.limiteTamanhoCorpoFilter = limiteTamanhoCorpoFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // Explícito: força HTTPS por 1 ano e inclui subdomínios.
                        // Spring Security já aplica em HTTPS, mas tornamos visível aqui.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31_536_000)
                                .includeSubDomains(true))
                        // Evita vazamento da URL completa de referência para domínios externos.
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Desabilita APIs de browser que esta aplicação não usa.
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // /error é o forward interno do Spring para erros (ex.: 400 por
                        // parâmetro ausente). Protegido, ele virava um falso 401 e o
                        // frontend deslogava o usuário.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authenticationProvider(authenticationProvider())
                // Limite de corpo e rate limiting ficam DEPOIS do CorsFilter: as respostas 413 e
                // 429 também precisam dos cabeçalhos CORS, senão o navegador as descarta e o
                // usuário vê "erro de conexão" em vez da mensagem real.
                .addFilterAfter(limiteTamanhoCorpoFilter, CorsFilter.class)
                .addFilterAfter(rateLimitingFilter, LimiteTamanhoCorpoFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Como os filtros abaixo são beans, o Spring Boot os registraria também como filtros de
     * servlet, antes do Spring Security (e do CORS). Aqui eles ficam só na cadeia de segurança.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingForaDoServlet(RateLimitingFilter filtro) {
        FilterRegistrationBean<RateLimitingFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<LimiteTamanhoCorpoFilter> limiteCorpoForaDoServlet(LimiteTamanhoCorpoFilter filtro) {
        FilterRegistrationBean<LimiteTamanhoCorpoFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /**
     * Configura as origens permitidas para requisições cross-origin.
     *
     * <p>A variável {@code CORS_ORIGINS} aceita múltiplas origens separadas por
     * vírgula: {@code https://app.exemplo.com,https://preview.exemplo.com}.</p>
     *
     * <p>O header {@code X-Request-ID} é exposto na response para permitir que
     * o frontend correlacione logs com o backend.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origens = Arrays.stream(corsOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

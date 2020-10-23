package com.github.wenslo.fluent.adminserver.config

import de.codecentric.boot.admin.server.config.AdminServerProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer
import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import java.util.*


@Configuration(proxyBeanMethods = false)
class SecuritySecureConfig : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var adminServer: AdminServerProperties

    override fun configure(http: HttpSecurity) {
        val successHandler = SavedRequestAwareAuthenticationSuccessHandler()
        successHandler.setTargetUrlParameter("redirectTo")
        successHandler.setDefaultTargetUrl(adminServer.path("/"))
        http.authorizeRequests { authorizeRequests ->
            authorizeRequests
                .antMatchers(adminServer.path("/assets/**")).permitAll()
                .antMatchers(adminServer.path("/login"))
                .permitAll().anyRequest().authenticated()
        }.formLogin { formLogin: FormLoginConfigurer<HttpSecurity> ->
            formLogin.loginPage(adminServer.path("/login")).successHandler(successHandler).and()
        }.logout { logout: LogoutConfigurer<HttpSecurity?> ->
            logout.logoutUrl(adminServer.path("/logout"))
        }.httpBasic(Customizer.withDefaults())
            .csrf { csrf: CsrfConfigurer<HttpSecurity?> ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        AntPathRequestMatcher(adminServer.path("/instances"), HttpMethod.POST.toString()),
                        AntPathRequestMatcher(adminServer.path("/instances/*"), HttpMethod.DELETE.toString()),
                        AntPathRequestMatcher(adminServer.path("/actuator/**"))
                    )
            }
            .rememberMe { rememberMe: RememberMeConfigurer<HttpSecurity?> ->
                rememberMe.key(
                    UUID.randomUUID().toString()
                ).tokenValiditySeconds(1209600)
            }
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.inMemoryAuthentication().withUser("user").password("{noop}password").roles("USER");
    }
}
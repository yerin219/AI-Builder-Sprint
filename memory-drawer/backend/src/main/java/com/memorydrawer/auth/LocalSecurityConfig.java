package com.memorydrawer.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalSecurityConfig {

	@Bean
	public SecurityFilterChain localSecurityFilterChain(
		HttpSecurity http,
		ApiAuthenticationEntryPoint authenticationEntryPoint
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/auth/signup", "/auth/login").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> {})
				.authenticationEntryPoint(authenticationEntryPoint)
			)
			.httpBasic(httpBasic -> httpBasic
				.authenticationEntryPoint(authenticationEntryPoint)
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.build();
	}

	@Bean
	public UserDetailsService localUserDetailsService(
		@Value("${spring.security.user.name}") String username,
		@Value("${spring.security.user.password}") String password
	) {
		return new InMemoryUserDetailsManager(
			User.withUsername(username)
				.password("{noop}" + password)
				.roles("LOCAL_TEST")
				.build()
		);
	}
}

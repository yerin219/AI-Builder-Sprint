package com.memorydrawer.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "memory-drawer.auth")
public class AuthTokenProperties {

	@NotBlank
	@Size(min = 32)
	private String jwtSecret;

	@Min(1)
	private long accessTokenExpirationSeconds;

	private String issuer = "memory-drawer";

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationSeconds;
	}

	public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
		this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}

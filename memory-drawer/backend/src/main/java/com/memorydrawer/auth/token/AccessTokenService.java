package com.memorydrawer.auth.token;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

	private final JwtEncoder jwtEncoder;
	private final AuthTokenProperties properties;

	public AccessTokenService(JwtEncoder jwtEncoder, AuthTokenProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public IssuedAccessToken issue(UUID userId) {
		long expiresIn = properties.getAccessTokenExpirationSeconds();
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plusSeconds(expiresIn);

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
			.type("JWT")
			.build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.getIssuer())
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
			.getTokenValue();

		return new IssuedAccessToken(token, expiresIn);
	}
}

package com.memorydrawer.ai.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UpstageProperties.class)
public class UpstageClientConfig {

	@Bean
	RestClient upstageRestClient(UpstageProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(properties.getConnectTimeout())
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.getRequestTimeout());

		return RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
			.build();
	}
}

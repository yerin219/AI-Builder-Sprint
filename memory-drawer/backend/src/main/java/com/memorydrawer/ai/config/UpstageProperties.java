package com.memorydrawer.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upstage")
public class UpstageProperties {

	private String apiKey;
	private String baseUrl;
	private String documentParseModel;
	private String informationExtractModel;
	private String solarModel;
	private Duration connectTimeout;
	private Duration requestTimeout;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getDocumentParseModel() {
		return documentParseModel;
	}

	public void setDocumentParseModel(String documentParseModel) {
		this.documentParseModel = documentParseModel;
	}

	public String getInformationExtractModel() {
		return informationExtractModel;
	}

	public void setInformationExtractModel(String informationExtractModel) {
		this.informationExtractModel = informationExtractModel;
	}

	public String getSolarModel() {
		return solarModel;
	}

	public void setSolarModel(String solarModel) {
		this.solarModel = solarModel;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Duration requestTimeout) {
		this.requestTimeout = requestTimeout;
	}
}

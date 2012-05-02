package org.springframework.security.oauth2.client.token;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.auth.ClientAuthenticationHandler;
import org.springframework.security.oauth2.client.token.auth.DefaultClientAuthenticationHandler;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.http.converter.FormOAuth2AccessTokenMessageConverter;
import org.springframework.security.oauth2.http.converter.FormOAuth2ExceptionHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Base support logic for obtaining access tokens.
 * 
 * @author Ryan Heaton
 * @author Dave Syer
 */
public abstract class OAuth2AccessTokenSupport {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final FormHttpMessageConverter FORM_MESSAGE_CONVERTER = new FormHttpMessageConverter();

	private final RestTemplate restTemplate;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private ClientAuthenticationHandler authenticationHandler = new DefaultClientAuthenticationHandler();

	protected OAuth2AccessTokenSupport() {
		this.restTemplate = new RestTemplate();
		this.restTemplate.setErrorHandler(getResponseErrorHandler());
		this.restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
			@Override
			protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
				super.prepareConnection(connection, httpMethod);
				connection.setInstanceFollowRedirects(false);
			}
		});
		setMessageConverters(this.restTemplate.getMessageConverters());
	}

	protected RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public void setAuthenticationHandler(ClientAuthenticationHandler authenticationHandler) {
		this.authenticationHandler = authenticationHandler;
	}

	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = new ArrayList<HttpMessageConverter<?>>(messageConverters);
		this.messageConverters.add(new FormOAuth2AccessTokenMessageConverter());
		this.messageConverters.add(new FormOAuth2ExceptionHttpMessageConverter());
	}

	protected OAuth2AccessToken retrieveToken(MultiValueMap<String, String> form, HttpHeaders headers,
			OAuth2ProtectedResourceDetails resource) throws OAuth2AccessDeniedException {

		try {
			// Prepare headers and form before going into rest template call in case the URI is affected by the result
			authenticationHandler.authenticateTokenRequest(resource, form, headers);

			return getRestTemplate().execute(getAccessTokenUri(resource, form), getHttpMethod(),
					getRequestCallback(resource, form, headers), getResponseExtractor(), form.toSingleValueMap());

		}
		catch (OAuth2Exception oe) {
			throw new OAuth2AccessDeniedException("Access token denied.", resource, oe);
		}
		catch (RestClientException rce) {
			throw new OAuth2AccessDeniedException("Error requesting access token.", resource, rce);
		}

	}

	protected HttpMethod getHttpMethod() {
		return HttpMethod.POST;
	}

	protected String getAccessTokenUri(OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form) {

		String accessTokenUri = resource.getAccessTokenUri();

		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving token from " + accessTokenUri);
		}

		StringBuilder builder = new StringBuilder(accessTokenUri);

		if (getHttpMethod() == HttpMethod.GET) {
			String separator = "?";
			if (accessTokenUri.contains("?")) {
				separator = "&";
			}

			for (String key : form.keySet()) {
				builder.append(separator);
				builder.append(key + "={" + key + "}");
				separator = "&";
			}
		}

		return builder.toString();

	}

	protected ResponseErrorHandler getResponseErrorHandler() {
		return new AccessTokenErrorHandler();
	}

	protected ResponseExtractor<OAuth2AccessToken> getResponseExtractor() {
		return new HttpMessageConverterExtractor<OAuth2AccessToken>(OAuth2AccessToken.class, this.messageConverters);
	}

	protected RequestCallback getRequestCallback(OAuth2ProtectedResourceDetails resource,
			MultiValueMap<String, String> form, HttpHeaders headers) {
		return new OAuth2AuthTokenCallback(form, headers);
	}

	/**
	 * Request callback implementation that writes the given object to the request stream.
	 */
	private class OAuth2AuthTokenCallback implements RequestCallback {

		private final MultiValueMap<String, String> form;

		private final HttpHeaders headers;

		private OAuth2AuthTokenCallback(MultiValueMap<String, String> form, HttpHeaders headers) {
			this.form = form;
			this.headers = headers;
		}

		public void doWithRequest(ClientHttpRequest request) throws IOException {
			request.getHeaders().putAll(this.headers);
			request.getHeaders().setAccept(
					Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED));
			FORM_MESSAGE_CONVERTER.write(this.form, MediaType.APPLICATION_FORM_URLENCODED, request);
		}
	}

	private class AccessTokenErrorHandler extends DefaultResponseErrorHandler {

		@SuppressWarnings("unchecked")
		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			for (HttpMessageConverter<?> converter : messageConverters) {
				if (converter.canRead(OAuth2Exception.class, response.getHeaders().getContentType())) {
					OAuth2Exception ex;
					try {
						ex = ((HttpMessageConverter<OAuth2Exception>) converter).read(OAuth2Exception.class, response);
					}
					catch (Exception e) {
						// ignore
						continue;
					}
					throw ex;
				}
			}
			super.handleError(response);
		}

	}

}

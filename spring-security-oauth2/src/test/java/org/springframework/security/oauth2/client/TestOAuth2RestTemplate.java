package org.springframework.security.oauth2.client;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * @author Ryan Heaton
 * @author Dave Syer
 */
public class TestOAuth2RestTemplate {

	private BaseOAuth2ProtectedResourceDetails resource;

	private OAuth2RestTemplate fac;
	
	private AccessTokenProvider accessTokenProvider = Mockito.mock(AccessTokenProvider.class);

	@Before
	public void open() {
		resource = new BaseOAuth2ProtectedResourceDetails();
		// Facebook and older specs:
		resource.setTokenName("bearer_token");
		fac = new OAuth2RestTemplate(resource);
		fac.setAccessTokenProvider(accessTokenProvider);
	}

	/**
	 * tests appendQueryParameter
	 */
	@Test
	public void testAppendQueryParameter() throws Exception {
		OAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
		URI appended = fac.appendQueryParameter(URI.create("https://graph.facebook.com/search?type=checkin"), token);
		assertEquals("https://graph.facebook.com/search?type=checkin&bearer_token=12345", appended.toString());
	}

	/**
	 * tests appendQueryParameter
	 */
	@Test
	public void testAppendQueryParameterWithNoExistingParameters() throws Exception {
		OAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
		URI appended = fac.appendQueryParameter(URI.create("https://graph.facebook.com/search"), token);
		assertEquals("https://graph.facebook.com/search?bearer_token=12345", appended.toString());
	}

	/**
	 * tests encoding of access token value
	 */
	@Test
	public void testDoubleEncodingOfParameterValue() throws Exception {
		OAuth2AccessToken token = new DefaultOAuth2AccessToken("1/qIxxx");
		URI appended = fac.appendQueryParameter(URI.create("https://graph.facebook.com/search"), token);
		assertEquals("https://graph.facebook.com/search?bearer_token=1%2FqIxxx", appended.toString());
	}

	/**
	 * tests URI with fragment value
	 */
	@Test
	public void testFragmentUri() throws Exception {
		OAuth2AccessToken token = new DefaultOAuth2AccessToken("1234");
		URI appended = fac.appendQueryParameter(URI.create("https://graph.facebook.com/search#foo"), token);
		assertEquals("https://graph.facebook.com/search?bearer_token=1234#foo", appended.toString());
	}

	/**
	 * tests encoding of access token value passed in protected requests ref: SECOAUTH-90
	 */
	@Test
	public void testDoubleEncodingOfAccessTokenValue() throws Exception {
		// try with fictitious token value with many characters to encode
		OAuth2AccessToken token = new DefaultOAuth2AccessToken("1 qI+x:y=z");
		// System.err.println(UriUtils.encodeQueryParam(token.getValue(), "UTF-8"));
		URI appended = fac.appendQueryParameter(URI.create("https://graph.facebook.com/search"), token);
		assertEquals("https://graph.facebook.com/search?bearer_token=1+qI%2Bx%3Ay%3Dz", appended.toString());
	}

}

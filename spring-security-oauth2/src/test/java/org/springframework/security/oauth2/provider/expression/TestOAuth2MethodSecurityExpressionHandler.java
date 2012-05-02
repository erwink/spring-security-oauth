/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.oauth2.provider.expression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * 
 */
public class TestOAuth2MethodSecurityExpressionHandler {

	private OAuth2MethodSecurityExpressionHandler handler = new OAuth2MethodSecurityExpressionHandler();

	@Test
	public void testOauthClient() throws Exception {
		AuthorizationRequest clientAuthentication = new AuthorizationRequest("foo", Collections.singleton("read"), Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority(
				"ROLE_USER")),
				Collections.singleton("bar"));
		Authentication userAuthentication = null;
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
		MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
				"testOauthClient"));
		EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
		Expression expression = handler.getExpressionParser().parseExpression("oauthClientHasAnyRole('ROLE_USER')");
		assertTrue((Boolean) expression.getValue(context));
	}

	@Test
	public void testScopes() throws Exception {
		AuthorizationRequest clientAuthentication = new AuthorizationRequest("foo", Collections.singleton("read"),
				Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority("ROLE_USER")),
				Collections.singleton("bar"));
		Authentication userAuthentication = null;
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
		MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
				"testOauthClient"));
		EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
		Expression expression = handler.getExpressionParser().parseExpression("oauthHasAnyScope('read')");
		assertTrue((Boolean) expression.getValue(context));
	}

	@Test
	public void testNonOauthClient() throws Exception {
		Authentication clientAuthentication = new UsernamePasswordAuthenticationToken("foo", "bar");
		MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
				"testNonOauthClient"));
		EvaluationContext context = handler.createEvaluationContext(clientAuthentication, invocation);
		Expression expression = handler.getExpressionParser().parseExpression("oauthClientHasAnyRole()");
		assertFalse((Boolean) expression.getValue(context));
	}

	@Test
	public void testStandardSecurityRoot() throws Exception {
		Authentication clientAuthentication = new UsernamePasswordAuthenticationToken("foo", "bar", null);
		assertTrue(clientAuthentication.isAuthenticated());
		MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
				"testStandardSecurityRoot"));
		EvaluationContext context = handler.createEvaluationContext(clientAuthentication, invocation);
		Expression expression = handler.getExpressionParser().parseExpression("isAuthenticated()");
		assertTrue((Boolean) expression.getValue(context));
	}
        
        @Test
        public void testOauthIsClient() throws Exception {
                AuthorizationRequest clientAuthentication =
                new AuthorizationRequest("foo", Collections.singleton("read"),
                                         Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority(
                                             "ROLE_CLIENT")),
                                         Collections.singleton("bar"));
                Authentication userAuthentication = null;
                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
                MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
                                "testOauthIsClient"));
                EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
                Expression expression = handler.getExpressionParser().parseExpression("oauthIsClient()");
                assertTrue((Boolean) expression.getValue(context));
        }

        @Test
        public void testOauthIsClientUserAuth() throws Exception {
                AuthorizationRequest clientAuthentication =
                new AuthorizationRequest("foo", Collections.singleton("read"),
                                         Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority(
                                             "ROLE_CLIENT")),
                                         Collections.singleton("bar"));
		Authentication userAuthentication =
		    new UsernamePasswordAuthenticationToken("foobar","foobar",
		        Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority("ROLE_USER")));
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
                MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
                                "testOauthIsClientUserAuth"));
                EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
                Expression expression = handler.getExpressionParser().parseExpression("oauthIsClient()");
                assertFalse((Boolean) expression.getValue(context));
        }

        @Test
        public void testOauthIsUser() throws Exception {
                AuthorizationRequest clientAuthentication =
                new AuthorizationRequest("foo", Collections.singleton("read"),
                                         Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority(
                                             "ROLE_CLIENT")),
                                         Collections.singleton("bar"));
		Authentication userAuthentication =
		    new UsernamePasswordAuthenticationToken("foobar","foobar",
                        Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority("ROLE_USER")));
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
                MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
                                "testOauthIsUser"));
                EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
                Expression expression = handler.getExpressionParser().parseExpression("oauthIsUser()");
                assertTrue((Boolean) expression.getValue(context));
        }

        @Test
        public void testOauthIsUserClientAuth() throws Exception {
                AuthorizationRequest clientAuthentication =
                new AuthorizationRequest("foo", Collections.singleton("read"),
                                         Collections.<GrantedAuthority> singleton(new SimpleGrantedAuthority(
					 "ROLE_CLIENT")), Collections.singleton("bar"));
                Authentication userAuthentication = null;
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(clientAuthentication, userAuthentication);
                MethodInvocation invocation = new SimpleMethodInvocation(this, ReflectionUtils.findMethod(getClass(),
                                "testOauthIsUserClientAuth"));
                EvaluationContext context = handler.createEvaluationContext(oAuth2Authentication, invocation);
                Expression expression = handler.getExpressionParser().parseExpression("oauthIsUser()");
                assertFalse((Boolean) expression.getValue(context));
        }



}

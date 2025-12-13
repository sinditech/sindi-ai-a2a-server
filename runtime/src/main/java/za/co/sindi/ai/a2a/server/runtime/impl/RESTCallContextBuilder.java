/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.impl;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.SecurityContext;
import za.co.sindi.ai.a2a.extensions.A2AExtensions;
import za.co.sindi.ai.a2a.server.CallContextBuilder;
import za.co.sindi.ai.a2a.server.ServerCallContext;
import za.co.sindi.ai.a2a.server.auth.UnauthenticatedUserPrincipal;
import za.co.sindi.ai.a2a.server.auth.UserPrincipal;

/**
 * @author Buhake Sindi
 * @since 17 November 2025
 */
public class RESTCallContextBuilder implements CallContextBuilder {

	private final SecurityContext securityContext;
	private final Map<String, List<String>> requestHeaders;
	
	/**
	 * @param securityContext
	 * @param requestHeaders
	 */
	public RESTCallContextBuilder(SecurityContext securityContext, Map<String, List<String>> requestHeaders) {
		super();
		this.securityContext = securityContext;
		this.requestHeaders = requestHeaders;
	}

	@Override
	public ServerCallContext build() {
		// TODO Auto-generated method stub
		UserPrincipal user = securityContext.getUserPrincipal() != null ? new AuthenticatedUserPrincipal(securityContext.getAuthenticationScheme(), securityContext.getUserPrincipal()) : new UnauthenticatedUserPrincipal();
		ServerCallContext context = new ServerCallContext(new ConcurrentHashMap<>(), user, A2AExtensions.getRequestedExtensions(requestHeaders.get(A2AExtensions.HTTP_EXTENSION_HEADER)), null);
		context.getState().put("headers", requestHeaders);
		return context;
	}
	
	private static class AuthenticatedUserPrincipal implements UserPrincipal {
		
		private final String authenticationScheme;
		private final Principal principal;

		/**
		 * @param authenticationScheme
		 * @param principal
		 */
		public AuthenticatedUserPrincipal(String authenticationScheme, Principal principal) {
			super();
			this.authenticationScheme = authenticationScheme;
			this.principal = principal;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return principal == null ? null : principal.getName();
		}

		/**
		 * @return the authenticationScheme
		 */
		public String getAuthenticationScheme() {
			return authenticationScheme;
		}

		@Override
		public boolean isAuthenticated() {
			// TODO Auto-generated method stub
			return principal != null;
		}
	}
}

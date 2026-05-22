package com.neullabs.regulus.identity.oidc;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.IdentityHolder;
import com.neullabs.regulus.identity.spi.AuthenticationException;
import com.neullabs.regulus.identity.spi.IdentityAdapter;
import com.neullabs.regulus.identity.spi.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet filter that reads Spring Security's {@code SecurityContext}, hands
 * the {@link JwtAuthenticationToken} to the {@link IdentityAdapter}, and
 * places the resulting {@link Identity} into {@link IdentityHolder} for the
 * lifetime of the request thread.
 *
 * <p>The filter runs AFTER Spring Security's own authentication filter but
 * BEFORE any policy/plugin code, so by the time a Regulus plugin reads
 * {@code IdentityHolder.require()} the canonical Identity is always
 * present (or the filter has already rejected the request).
 */
public final class OidcSecurityContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OidcSecurityContextFilter.class);

    private final IdentityAdapter adapter;

    public OidcSecurityContextFilter(IdentityAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            chain.doFilter(request, response);
            return;
        }

        RequestContext ctx = new RequestContext(
                request.getScheme(),
                headersOf(request),
                java.util.Optional.empty(),
                Map.of(OidcIdentityAdapter.SPRING_AUTH_KEY, jwt));

        try {
            Identity identity = adapter.authenticate(ctx);
            IdentityHolder.set(identity);
        } catch (AuthenticationException e) {
            log.warn("Regulus IdentityAdapter rejected the incoming JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"identity_rejected\"}");
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            IdentityHolder.clear();
        }
    }

    private static Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> out = new HashMap<>();
        var names = request.getHeaderNames();
        if (names == null) return Map.of();
        for (String name : Collections.list(names)) {
            String value = request.getHeader(name);
            if (value != null) out.put(name.toLowerCase(), value);
        }
        return Map.copyOf(out);
    }
}

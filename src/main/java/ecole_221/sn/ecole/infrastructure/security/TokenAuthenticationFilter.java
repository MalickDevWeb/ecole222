package ecole_221.sn.ecole.infrastructure.security;

import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepositoryPort userRepositoryPort;
    /** Révocation (déconnexion, changement de mot de passe) — même contrôle que JwtAuthenticationFilter. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.infrastructure.security.revocation.RevocationJetons revocation;

    public TokenAuthenticationFilter(JwtService jwtService, UserRepositoryPort userRepositoryPort) {
        this.jwtService = jwtService;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Ce filtre ne gère que l'authentification via header Authorization: Bearer <token>
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userRepositoryPort.findByEmail(userEmail).orElse(null);

                if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)
                        && (revocation == null || !revocation.estRefuse(jwt,
                                userDetails instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u ? u.getId() : null,
                                jwtService.extractIssuedAt(jwt)))) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalide ou expiré — la requête sera bloquée par Spring Security si la route est protégée
        }

        filterChain.doFilter(request, response);
    }
}

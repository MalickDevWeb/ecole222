package ecole_221.sn.ecole.shared.infrastructure.tenant.domaine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * Résolveur DNS réel, fourni par le JDK (JNDI, fabrique {@code com.sun.jndi.dns.DnsContextFactory}) : aucune
 * dépendance ajoutée. Serveurs DNS du système, délai court (2 s, une seule nouvelle tentative).
 *
 * <p>DevSecOps : au plus {@value #REQUETES_SIMULTANEES} lectures DNS ont lieu en même temps, pour qu'une rafale de
 * noms ne bloque pas les fils de traitement du serveur. Au-delà, comme en cas de délai dépassé ou de serveur
 * injoignable, {@link ResolveurDns.Indisponible} est levée : l'appelant ne garde rien en mémoire et ne compte pas
 * d'échec de pointage (seuls « nom inexistant » et « pas de CNAME » valent réponse « absent »).</p>
 */
@Component
public class ResolveurDnsJndi implements ResolveurDns {

    private static final Logger log = LoggerFactory.getLogger(ResolveurDnsJndi.class);
    static final int REQUETES_SIMULTANEES = 16;
    static final String DELAI_MS = "2000";

    private final Semaphore places = new Semaphore(REQUETES_SIMULTANEES);

    @Override
    public Optional<String> cname(String hote) {
        if (hote == null || hote.isBlank()) {
            return Optional.empty();
        }
        if (!places.tryAcquire()) {
            log.warn("Lecture DNS refusée (trop de lectures simultanées) pour {}", hote);
            throw new ResolveurDns.Indisponible("Trop de lectures DNS simultanées.");
        }
        DirContext ctx = null;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            env.put("com.sun.jndi.dns.timeout.initial", DELAI_MS);
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            ctx = new InitialDirContext(env);
            Attributes attributs = ctx.getAttributes(hote, new String[]{"CNAME"});
            Attribute cname = attributs == null ? null : attributs.get("CNAME");
            if (cname == null || cname.size() == 0 || cname.get() == null) {
                return Optional.empty();
            }
            String cible = String.valueOf(cname.get()).trim();
            while (cible.endsWith(".")) {
                cible = cible.substring(0, cible.length() - 1);
            }
            return cible.isEmpty() ? Optional.empty() : Optional.of(cible);
        } catch (NameNotFoundException e) {
            return Optional.empty();   // réponse DNS : le nom n'existe pas
        } catch (NamingException | RuntimeException e) {
            log.debug("Lecture DNS impossible pour {} : {}", hote, e.toString());
            throw new ResolveurDns.Indisponible("Lecture DNS impossible : " + e.getClass().getSimpleName());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignore) {
                    // rien à faire
                }
            }
            places.release();
        }
    }
}

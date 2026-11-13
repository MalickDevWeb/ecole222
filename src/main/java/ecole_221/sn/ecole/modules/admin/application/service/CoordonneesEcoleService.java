package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.partage.ChampsRequete;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Coordonnées officielles de l'école courante (Personnalisation → Identité & coordonnées) — 2026-11-13.
 *
 * <p>Avant : l'écran de l'administrateur lisait et enregistrait via {@code /v1/super-admin/tenants/{id}}, route réservée
 * au super-administrateur : « Coordonnées indisponibles » pour l'école, rien n'était modifiable. L'école ne touche ici
 * qu'à sa propre fiche (l'identifiant vient de la session, jamais du client).</p>
 *
 * <p>Seuls les champs envoyés changent ; « » efface un champ facultatif ; le nom ne peut pas être effacé.</p>
 */
@Service
public class CoordonneesEcoleService {

    static final Set<String> CHAMPS = Set.of("name", "email", "phone", "address", "city", "website");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern TELEPHONE = Pattern.compile("^\\+?[0-9 .-]{6,20}$");
    private static final Pattern SITE = Pattern.compile("^(?i)https?://[^\\s/$.?#][^\\s]{1,250}$");
    private static final Pattern CHIFFRES = Pattern.compile("^[0-9\\s.,+-]+$");

    private final OrganizationRepositoryPort ecoles;

    public CoordonneesEcoleService(OrganizationRepositoryPort ecoles) {
        this.ecoles = ecoles;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lire() {
        return vers(ecoleCourante());
    }

    @Transactional
    public Map<String, Object> enregistrer(Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        if (corps.keySet().stream().noneMatch(CHAMPS::contains)) {
            throw ChampsRequete.refus("Aucun champ reconnu : name, email, phone, address, city ou website attendu.");
        }
        Organization ecole = ecoleCourante();
        if (corps.containsKey("name")) {
            String nom = ChampsRequete.texteOptionnel(corps, "name", "Le nom de l'établissement", 150);
            if (nom == null) {
                throw ChampsRequete.refus("Le nom de l'établissement ne peut pas être effacé.");
            }
            if (CHIFFRES.matcher(nom).matches()) {
                throw ChampsRequete.refus("Le nom de l'établissement doit contenir des lettres.");
            }
            ecole.setName(nom);
        }
        if (corps.containsKey("email")) {
            String email = ChampsRequete.texteOptionnel(corps, "email", "L'email officiel", 150);
            if (email != null) {
                email = email.toLowerCase(Locale.ROOT);
                if (!EMAIL.matcher(email).matches()) {
                    throw ChampsRequete.refus("L'email officiel n'est pas valide (exemple : contact@ecole.sn).");
                }
                if (!email.equalsIgnoreCase(ecole.getEmail()) && ecoles.existsByEmail(email)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "L'email " + email + " est déjà utilisé par un autre établissement.");
                }
            }
            ecole.setEmail(email);
        }
        if (corps.containsKey("phone")) {
            String tel = ChampsRequete.texteOptionnel(corps, "phone", "Le téléphone", 20);
            if (tel != null && !TELEPHONE.matcher(tel).matches()) {
                throw ChampsRequete.refus("Le téléphone n'est pas valide : chiffres, espaces, points ou tirets, 6 à 20 caractères (ex. +221 33 800 00 00).");
            }
            ecole.setPhone(tel);
        }
        if (corps.containsKey("address")) {
            ecole.setAddress(ChampsRequete.texteOptionnel(corps, "address", "L'adresse", 300));
        }
        if (corps.containsKey("city")) {
            String ville = ChampsRequete.texteOptionnel(corps, "city", "La ville", 100);
            if (ville != null && CHIFFRES.matcher(ville).matches()) {
                throw ChampsRequete.refus("La ville doit contenir des lettres.");
            }
            ecole.setCity(ville);
        }
        if (corps.containsKey("website")) {
            String site = ChampsRequete.texteOptionnel(corps, "website", "Le site web", 250);
            if (site != null && !SITE.matcher(site).matches()) {
                throw ChampsRequete.refus("Le site web doit être un lien complet commençant par http:// ou https://.");
            }
            ecole.setWebsite(site);
        }
        return vers(ecoles.save(ecole));
    }

    private Organization ecoleCourante() {
        String id = TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "École courante inconnue."));
        return ecoles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
    }

    private static Map<String, Object> vers(Organization o) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", o.getName());
        r.put("email", o.getEmail());
        r.put("phone", o.getPhone());
        r.put("address", o.getAddress());
        r.put("city", o.getCity());
        r.put("website", o.getWebsite());
        return r;
    }
}

package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.RegistreDomaines;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes publiques du domaine propre (15/11) : école désignée par une adresse, autorisation de certificat
 * (point « ask » du TLS à la demande de Caddy).
 */
class DomainesPublicsControllerTest {

    private MockMvc mvc;
    private DomainesPublicsController controleur;

    static Organization ecole(String slug, String nom, OrganizationStatus statut) {
        return OrganizationFactory.create(nom, OrganizationType.ECOLE, statut, slug,
                "SN", "Thiès", null, null, slug + "@exemple.sn", null, null, null, null, null);
    }

    @BeforeEach
    void preparer() {
        OrganizationRepositoryPort repo = Mockito.mock(OrganizationRepositoryPort.class);
        Mockito.when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.findBySlug("lycee-de-thies")).thenReturn(Optional.of(ecole("lycee-de-thies", "Lycée de Thiès", OrganizationStatus.ACTIVE)));
        Mockito.when(repo.findBySlug("ecole-attente")).thenReturn(Optional.of(ecole("ecole-attente", "École en attente", OrganizationStatus.PENDING)));
        Mockito.when(repo.findBySlug("ecole-suspendue")).thenReturn(Optional.of(ecole("ecole-suspendue", "École suspendue", OrganizationStatus.SUSPENDED)));
        Organization thies = repo.findBySlug("lycee-de-thies").orElseThrow();
        Organization susp = repo.findBySlug("ecole-suspendue").orElseThrow();
        Mockito.when(repo.findById(thies.getId())).thenReturn(Optional.of(thies));
        Map<String, String> dns = Map.of("www.lycee-thies.sn", "lycee-de-thies.myschool.sn",
                "portail.suspendue.sn", "ecole-suspendue.myschool.sn",
                "pointe-non-declare.sn", "lycee-de-thies.myschool.sn");
        Map<String, RegistreDomaines.DomaineDeclare> declares = Map.of(
                "www.lycee-thies.sn", new RegistreDomaines.DomaineDeclare("www.lycee-thies.sn", thies.getId(), RegistreDomaines.ACTIF),
                "portail.test.sn", new RegistreDomaines.DomaineDeclare("portail.test.sn", thies.getId(), RegistreDomaines.EN_ATTENTE_POINTAGE),
                "portail.suspendue.sn", new RegistreDomaines.DomaineDeclare("portail.suspendue.sn", susp.getId(), RegistreDomaines.ACTIF));
        ResolutionHoteEcole resolution = new ResolutionHoteEcole(repo, h -> Optional.ofNullable(dns.get(h)),
                h -> Optional.ofNullable(declares.get(h)), "myschool.sn",
                "portail.test.sn=lycee-de-thies.myschool.sn", "http://localhost:5173");
        controleur = new DomainesPublicsController(resolution);
        mvc = MockMvcBuilders.standaloneSetup(controleur).build();
    }

    @Test
    void parHoteEcoleActive200() throws Exception {
        mvc.perform(get("/public/tenants/par-hote").param("host", "lycee-de-thies.myschool.sn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("lycee-de-thies"))
                .andExpect(jsonPath("$.nom").value("Lycée de Thiès"));
        mvc.perform(get("/public/tenants/par-hote").param("host", "WWW.Lycee-Thies.sn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("lycee-de-thies"));
        mvc.perform(get("/public/tenants/par-hote").param("host", "portail.test.sn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("lycee-de-thies"));
    }

    @Test
    void parHoteSansEcoleActive404() throws Exception {
        for (String h : new String[]{"ecole-attente.myschool.sn", "ecole-suspendue.myschool.sn", "portail.suspendue.sn",
                "inconnue.myschool.sn", "www.myschool.sn", "portail.ailleurs.sn", "pointe-non-declare.sn"}) {
            mvc.perform(get("/public/tenants/par-hote").param("host", h))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Aucun établissement n'est rattaché à cette adresse."));
        }
        mvc.perform(get("/public/tenants/par-hote")).andExpect(status().isBadRequest());
    }

    @Test
    void autorisationDeCertificat200Ou404() throws Exception {
        mvc.perform(get("/public/domaines/autorisation").param("domain", "lycee-de-thies.myschool.sn")).andExpect(status().isOk());
        mvc.perform(get("/public/domaines/autorisation").param("domain", "www.lycee-thies.sn")).andExpect(status().isOk());
        for (String d : new String[]{"ecole-attente.myschool.sn", "portail.suspendue.sn", "www.myschool.sn", "myschool.sn",
                "api.myschool.sn", "inconnue.myschool.sn", "portail.ailleurs.sn", "mauvais_nom.sn", "",
                "pointe-non-declare.sn",                   // pointé mais non déclaré : jamais de certificat
                "WWW.Lycee-Thies.sn", "www.lycee-thies.sn.", "www.lycee-thies.sn:443",   // nom EXACT exigé
                "x.lycee-de-thies.myschool.sn", "*.myschool.sn"}) {
            mvc.perform(get("/public/domaines/autorisation").param("domain", d)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/public/domaines/autorisation")).andExpect(status().isNotFound());
    }

    @Test
    void autorisationLimiteeParNomEtGlobalement() throws Exception {
        for (int i = 0; i < DomainesPublicsController.DEMANDES_MAX_PAR_NOM; i++) {
            mvc.perform(get("/public/domaines/autorisation").param("domain", "www.lycee-thies.sn")
                    .with(r -> { r.setRemoteAddr("198.51.100.1"); return r; })).andExpect(status().isOk());
        }
        mvc.perform(get("/public/domaines/autorisation").param("domain", "www.lycee-thies.sn")
                        .with(r -> { r.setRemoteAddr("198.51.100.2"); return r; }))
                .andExpect(status().isTooManyRequests());
        for (int i = 0; i < DomainesPublicsController.DEMANDES_MAX_GLOBAL; i++) {
            final String ip = "192.0.2." + (i % 250);
            mvc.perform(get("/public/domaines/autorisation").param("domain", "g" + i + ".ailleurs.sn")
                    .with(r -> { r.setRemoteAddr(ip); return r; }));
        }
        mvc.perform(get("/public/domaines/autorisation").param("domain", "lycee-de-thies.myschool.sn")
                        .with(r -> { r.setRemoteAddr("198.51.100.3"); return r; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void autorisationLimiteeParIp() throws Exception {
        for (int i = 0; i < DomainesPublicsController.DEMANDES_MAX; i++) {
            mvc.perform(get("/public/domaines/autorisation").param("domain", "x" + i + ".ailleurs.sn")
                    .with(r -> { r.setRemoteAddr("203.0.113.7"); return r; }));
        }
        mvc.perform(get("/public/domaines/autorisation").param("domain", "lycee-de-thies.myschool.sn")
                        .with(r -> { r.setRemoteAddr("203.0.113.7"); return r; }))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get("/public/domaines/autorisation").param("domain", "lycee-de-thies.myschool.sn")
                        .with(r -> { r.setRemoteAddr("203.0.113.8"); return r; }))
                .andExpect(status().isOk());
    }
}

package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Messagerie parent ↔ école : messages réellement enregistrés, chacun ne voit que ses échanges. */
class MessagerieParentsServiceTest {

    private final Map<String, MessageParentJpaEntity> base = new LinkedHashMap<>();
    private MessagerieParentsService service;

    @BeforeEach
    void preparer() {
        MessageParentJpaRepository messages = mock(MessageParentJpaRepository.class);
        when(messages.save(any())).thenAnswer(i -> { MessageParentJpaEntity m = i.getArgument(0); base.put(m.getId(), m); return m; });
        when(messages.findById(anyString())).thenAnswer(i -> Optional.ofNullable(base.get((String) i.getArgument(0))));
        when(messages.findByParentIdOrderByEnvoyeLeDesc(anyString())).thenAnswer(i -> base.values().stream()
                .filter(m -> m.getParentId().equals(i.getArgument(0))).toList());
        when(messages.findByInterlocuteurIdOrderByEnvoyeLeDesc(anyString())).thenAnswer(i -> base.values().stream()
                .filter(m -> m.getInterlocuteurId().equals(i.getArgument(0))).toList());

        UserJpaRepository comptes = mock(UserJpaRepository.class);
        when(comptes.findById(anyString())).thenAnswer(i -> Optional.ofNullable(compte((String) i.getArgument(0))));

        OrganizationUserRepositoryPort appartenances = mock(OrganizationUserRepositoryPort.class);
        OrganizationUser actif = mock(OrganizationUser.class);
        when(actif.isActive()).thenReturn(true);
        when(actif.getUserId()).thenReturn("prof-1");
        OrganizationUser parti = mock(OrganizationUser.class);
        when(parti.isActive()).thenReturn(false);
        when(parti.getUserId()).thenReturn("prof-parti");
        when(appartenances.findByOrganizationIdAndRole("ecole-1", "PROFESSEUR")).thenReturn(List.of(actif, parti));

        Organization ecole = mock(Organization.class);
        when(ecole.getId()).thenReturn("ecole-1");
        TenantContext.setCurrentOrganization(ecole);

        service = new MessagerieParentsService(messages, comptes, appartenances,
                Clock.fixed(Instant.parse("2026-11-11T09:30:00Z"), ZoneId.of("Africa/Dakar")));
    }

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
    }

    private static UserJpaEntity compte(String id) {
        Map<String, String[]> noms = Map.of("parent-1", new String[]{"Fatou", "Ndiaye"}, "parent-2", new String[]{"Modou", "Fall"},
                "prof-1", new String[]{"Awa", "Sarr"}, "admin-1", new String[]{"Ibrahima", "Diop"});
        if (!noms.containsKey(id)) return null;
        UserJpaEntity u = mock(UserJpaEntity.class);
        when(u.getPrenom()).thenReturn(noms.get(id)[0]);
        when(u.getNom()).thenReturn(noms.get(id)[1]);
        return u;
    }

    private static void statut(Runnable action, HttpStatus attendu, String extrait) {
        assertThatThrownBy(action::run).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(attendu))
                .hasMessageContaining(extrait);
    }

    @Test
    void destinatairesAdministrationEtProfesseursActifsSeulement() {
        List<Map<String, Object>> d = service.destinataires();
        assertThat(d).extracting(m -> m.get("id")).containsExactly("ADMINISTRATION", "prof-1");
        assertThat(d.get(1).get("nom")).isEqualTo("Awa Sarr");
    }

    @Test
    void echangeCompletParentProfesseur() {
        Map<String, Object> envoi = service.envoyerParParent("parent-1", "prof-1", "  Devoir de maths ", "Bonjour, Aminata était malade.");
        assertThat(envoi.get("envoye")).isEqualTo(true);
        assertThat(envoi.get("sujet")).isEqualTo("Devoir de maths");
        assertThat(envoi.get("interlocuteur")).isEqualTo("Awa Sarr");
        String id = (String) envoi.get("id");

        Map<String, Object> recu = service.boiteEcole("prof-1").get(0);
        assertThat(recu.get("lu")).isEqualTo(false);                 // non lu par le professeur
        assertThat(recu.get("de")).isEqualTo("Fatou Ndiaye");

        Map<String, Object> rep = service.repondre("prof-1", "prof-1", id, "Merci, bon rétablissement.");
        assertThat(rep.get("sujet")).isEqualTo("Re : Devoir de maths");
        assertThat(base.get(id).getLuLe()).isNotNull();                // répondre, c'est avoir lu

        List<Map<String, Object>> fil = service.filDuParent("parent-1");
        Map<String, Object> reponse = fil.stream().filter(m -> m.get("id").equals(rep.get("id"))).findFirst().orElseThrow();
        assertThat(reponse.get("envoye")).isEqualTo(false);
        assertThat(reponse.get("lu")).isEqualTo(false);
        assertThat(reponse.get("role")).isEqualTo("PROFESSEUR");
        assertThat(service.marquerLuParParent("parent-1", (String) rep.get("id")).get("lu")).isEqualTo(true);
    }

    @Test
    void administrationBoitePartageeEtReponseSigneeParLAdministrateur() {
        String id = (String) service.envoyerParParent("parent-1", "ADMINISTRATION", "Certificat de scolarité", "Pouvez-vous me l'envoyer ?").get("id");
        assertThat(service.boiteEcole("prof-1")).isEmpty();
        Map<String, Object> rep = service.repondre("ADMINISTRATION", "admin-1", id, "Il est prêt au secrétariat.");
        assertThat(rep.get("de")).isEqualTo("Administration — Ibrahima Diop");
        assertThat(rep.get("role")).isEqualTo("ADMINISTRATION");
        assertThat(base.get((String) rep.get("id")).getParentId()).isEqualTo("parent-1");
    }

    @Test
    void chacunNeVoitEtNeToucheQueSesEchanges() {
        String id = (String) service.envoyerParParent("parent-1", "prof-1", "Absence", "Mon fils sera absent lundi.").get("id");
        assertThat(service.filDuParent("parent-2")).isEmpty();
        statut(() -> service.marquerLuParParent("parent-2", id), HttpStatus.NOT_FOUND, "introuvable");
        statut(() -> service.repondre("ADMINISTRATION", "admin-1", id, "x"), HttpStatus.NOT_FOUND, "introuvable");
        statut(() -> service.marquerLuParEcole("prof-autre", id), HttpStatus.NOT_FOUND, "introuvable");
        statut(() -> service.repondre("prof-1", "prof-1", "msg-inexistant", "x"), HttpStatus.NOT_FOUND, "introuvable");
    }

    @Test
    void saisiesInvalidesRefuseesSansRienEnregistrer() {
        statut(() -> service.envoyerParParent("parent-1", " ", "Sujet", "Texte"), HttpStatus.BAD_REQUEST, "destinataire");
        statut(() -> service.envoyerParParent("parent-1", "prof-parti", "Sujet", "Texte"), HttpStatus.BAD_REQUEST, "pas un professeur");
        statut(() -> service.envoyerParParent("parent-1", "parent-2", "Sujet", "Texte"), HttpStatus.BAD_REQUEST, "pas un professeur");
        statut(() -> service.envoyerParParent("parent-1", "prof-1", "  ", "Texte"), HttpStatus.BAD_REQUEST, "objet");
        statut(() -> service.envoyerParParent("parent-1", "prof-1", "Sujet", ""), HttpStatus.BAD_REQUEST, "vide");
        statut(() -> service.envoyerParParent("parent-1", "prof-1", "Sujet", "x".repeat(5001)), HttpStatus.BAD_REQUEST, "5001 caractères");
        assertThat(base).isEmpty();
    }

    @Test
    void sesPropresEnvoisNeSontJamaisNonLus() {
        String id = (String) service.envoyerParParent("parent-1", "prof-1", "Question", "Texte").get("id");
        assertThat(service.filDuParent("parent-1").get(0).get("lu")).isEqualTo(true);
        service.marquerLuParParent("parent-1", id);
        assertThat(base.get(id).getLuLe()).isNull();   // le parent ne marque pas « lu » à la place du professeur
    }
}

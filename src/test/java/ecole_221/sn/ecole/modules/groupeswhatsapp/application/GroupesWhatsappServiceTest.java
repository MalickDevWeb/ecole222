package ecole_221.sn.ecole.modules.groupeswhatsapp.application;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.GroupeWhatsappJpaEntity;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.GroupeWhatsappJpaRepository;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.PartageWhatsappJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Groupes WhatsApp de l'école (FEAT-007, BR-01 à BR-10). */
class GroupesWhatsappServiceTest {

    private final GroupeWhatsappJpaRepository groupes = Mockito.mock(GroupeWhatsappJpaRepository.class);
    private final PartageWhatsappJpaRepository partages = Mockito.mock(PartageWhatsappJpaRepository.class);
    private final ClasseJpaRepository classes = Mockito.mock(ClasseJpaRepository.class);
    private final InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
    private final ParentEnfantJpaRepository liens = Mockito.mock(ParentEnfantJpaRepository.class);
    private final List<GroupeWhatsappJpaEntity> base = new ArrayList<>();
    private GroupesWhatsappService service;

    @BeforeEach
    void setUp() {
        Mockito.when(groupes.save(any())).thenAnswer(a -> { GroupeWhatsappJpaEntity g = a.getArgument(0); base.removeIf(x -> x.getId().equals(g.getId())); base.add(g); return g; });
        Mockito.when(groupes.findAllByOrderByNomAsc()).thenAnswer(a -> base.stream().sorted((x, y) -> x.getNom().compareTo(y.getNom())).toList());
        Mockito.when(groupes.existsByLien(anyString())).thenAnswer(a -> base.stream().anyMatch(g -> g.getLien().equals(a.getArgument(0))));
        Mockito.when(groupes.findById(anyString())).thenAnswer(a -> base.stream().filter(g -> g.getId().equals(a.getArgument(0))).findFirst());
        Mockito.when(partages.save(any())).thenAnswer(a -> a.getArgument(0));
        ClasseJpaEntity l1 = new ClasseJpaEntity(); l1.setId("c-l1"); l1.setName("L1 Informatique");
        ClasseJpaEntity l2 = new ClasseJpaEntity(); l2.setId("c-l2"); l2.setName("L2 Informatique");
        Mockito.when(classes.findById("c-l1")).thenReturn(Optional.of(l1));
        Mockito.when(classes.findById("c-l2")).thenReturn(Optional.of(l2));
        Mockito.when(classes.findById("c-inconnue")).thenReturn(Optional.empty());
        service = new GroupesWhatsappService(groupes, partages, classes, inscriptions, liens);
        connecte("adm", RoleUtilisateur.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void connecte(String id, RoleUtilisateur role) {
        User u = UserFactory.reconstructSimple(id, "Ndiaye", "Awa", id + "@ex.sn", "a", null, role, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    private static Map<String, Object> corps(String nom, String pub, String classeId, String lien) {
        Map<String, Object> m = new HashMap<>();
        m.put("nom", nom); m.put("public", pub); m.put("lien", lien);
        if (classeId != null) m.put("classeId", classeId);
        return m;
    }

    private static HttpStatus statut(Runnable r) {
        try { r.run(); } catch (ResponseStatusException e) { return HttpStatus.valueOf(e.getStatusCode().value()); }
        return null;
    }

    @Test
    void lienDInvitationSeulEtNormalise() {
        assertEquals("https://chat.whatsapp.com/AbCdEf1234567890", GroupesWhatsappService.lienNormalise("chat.whatsapp.com/AbCdEf1234567890"));
        assertEquals("https://chat.whatsapp.com/AbCdEf1234567890", GroupesWhatsappService.lienNormalise(" https://chat.whatsapp.com/invite/AbCdEf1234567890/ "));
        for (String faux : List.of("https://wa.me/221770000000", "https://chat.whatsapp.com.pirate.sn/AbCdEf1234567890",
                "https://exemple.sn/chat.whatsapp.com/AbCdEf1234567890", "https://chat.whatsapp.com/court", "javascript:alert(1)", "")) {
            assertEquals(HttpStatus.BAD_REQUEST, statut(() -> GroupesWhatsappService.lienNormalise(faux)), faux);
        }
    }

    @Test
    void creationControleeEtLienUnique() {
        Map<String, Object> g = service.creer(corps("Parents — L1", "parents", "c-l1", "https://chat.whatsapp.com/AbCdEf1234567890"));
        assertEquals("PARENTS", g.get("public"));
        assertEquals("L1 Informatique", g.get("classe"));
        assertEquals(true, g.get("actif"));
        assertEquals(HttpStatus.CONFLICT, statut(() -> service.creer(corps("Doublon", "PARENTS", null, "chat.whatsapp.com/AbCdEf1234567890"))));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.creer(corps("Profs L1", "PROFESSEURS", "c-l1", "https://chat.whatsapp.com/ZzZz1234567890"))));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.creer(corps("X", "VISITEURS", null, "https://chat.whatsapp.com/YyYy1234567890"))));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.creer(corps("X", "ELEVES", "c-inconnue", "https://chat.whatsapp.com/XxXx1234567890"))));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.creer(corps("", "ELEVES", null, "https://chat.whatsapp.com/WwWw1234567890"))));
    }

    @Test
    void partageNoteAvecHeureEtAuteurMaisPasDansUnGroupeDesactive() {
        Map<String, Object> g = service.creer(corps("Toute l'école", "TOUTE_ECOLE", null, "https://chat.whatsapp.com/AbCdEf1234567890"));
        Map<String, Object> p = service.noterPartage((String) g.get("id"), Map.of("titre", "Rentrée"));
        assertEquals("Toute l'école", p.get("groupe"));
        assertEquals("Awa Ndiaye", p.get("auteur"));
        assertNotNull(p.get("partageLe"));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.noterPartage((String) g.get("id"), Map.of())));
        service.modifier((String) g.get("id"), Map.of("actif", false));
        assertEquals(HttpStatus.BAD_REQUEST, statut(() -> service.noterPartage((String) g.get("id"), Map.of("titre", "x"))));
    }

    @Test
    void eleveEtParentVoientSeulementLeursGroupes() {
        service.creer(corps("Toute l'école", "TOUTE_ECOLE", null, "https://chat.whatsapp.com/Aaaa1234567890"));
        service.creer(corps("Élèves — L1", "ELEVES", "c-l1", "https://chat.whatsapp.com/Bbbb1234567890"));
        service.creer(corps("Parents — L1", "PARENTS", "c-l1", "https://chat.whatsapp.com/Cccc1234567890"));
        service.creer(corps("Parents — L2", "PARENTS", "c-l2", "https://chat.whatsapp.com/Dddd1234567890"));
        service.creer(corps("Professeurs", "PROFESSEURS", null, "https://chat.whatsapp.com/Eeee1234567890"));
        Map<String, Object> inactif = service.creer(corps("Parents (ancien)", "PARENTS", null, "https://chat.whatsapp.com/Ffff1234567890"));
        service.modifier((String) inactif.get("id"), Map.of("actif", false));
        Mockito.when(inscriptions.findByUserId("e1")).thenReturn(List.of(new InscriptionClasseJpaEntity("i1", "e1", "c-l1")));
        ParentEnfantJpaEntity lien = new ParentEnfantJpaEntity(); lien.setParentId("p1"); lien.setEnfantId("e1");
        Mockito.when(liens.findByParentId("p1")).thenReturn(List.of(lien));

        connecte("e1", RoleUtilisateur.ETUDIANT);
        assertEquals(List.of("Toute l'école", "Élèves — L1"), service.mesGroupes().stream().map(m -> m.get("nom")).toList());
        connecte("p1", RoleUtilisateur.PARENT);
        assertEquals(List.of("Parents — L1", "Toute l'école"), service.mesGroupes().stream().map(m -> m.get("nom")).toList());
        connecte("prof", RoleUtilisateur.PROFESSEUR);
        assertTrue(service.mesGroupes().isEmpty());
    }
}

package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** APP-TL-24 : synthèse du suivi calculée par règles (R1 à R4), sans IA ni score, à partir des données enregistrées. */
class SyntheseSuiviEleveServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 14);

    private BulletinEleveService bulletins;
    private AssiduiteEleveService assiduites;
    private RendusDevoirsService devoirs;
    private SyntheseSuiviEleveService service;

    @BeforeEach
    void setUp() {
        bulletins = Mockito.mock(BulletinEleveService.class);
        assiduites = Mockito.mock(AssiduiteEleveService.class);
        devoirs = Mockito.mock(RendusDevoirsService.class);
        service = new SyntheseSuiviEleveService(bulletins, assiduites, devoirs);
    }

    private static Map<String, Object> bulletin(Double generale, Object... coursMoyenne) {
        List<Map<String, Object>> matieres = new ArrayList<>();
        int notees = 0;
        for (int i = 0; i < coursMoyenne.length; i += 2) {
            Map<String, Object> m = new HashMap<>();
            m.put("cours", coursMoyenne[i]);
            m.put("moyenne", coursMoyenne[i + 1]);
            matieres.add(m);
            if (coursMoyenne[i + 1] != null) notees++;
        }
        Map<String, Object> b = new HashMap<>();
        b.put("moyenneGenerale", generale);
        b.put("matieresNotees", notees);
        b.put("provisoire", false);
        b.put("matieres", matieres);
        return b;
    }

    private static Map<String, Object> assiduite(int present, int retard, int absent, int excuse) {
        Map<String, Object> g = AssiduiteEleveService.compteurs(new int[]{present, retard, absent, excuse});
        return Map.of("global", g);
    }

    private static Map<String, Object> devoir(String titre, String deadline, String statut) {
        Map<String, Object> d = new HashMap<>();
        d.put("titre", titre);
        d.put("coursTitre", "Français");
        d.put("deadline", deadline);
        d.put("statut", statut);
        return d;
    }

    @Test
    void sansDonneesEnregistreesRienNEstInvente() {
        Mockito.when(bulletins.bulletin("e1")).thenReturn(bulletin(null, "Mathématiques", null));
        Mockito.when(assiduites.assiduite("e1")).thenReturn(assiduite(0, 0, 0, 0));
        Mockito.when(devoirs.devoirsEleve("e1")).thenReturn(List.of());

        Map<String, Object> s = service.synthese("e1", AUJOURDHUI);

        assertEquals("donnees_insuffisantes", s.get("niveau"));
        assertNull(s.get("moyenneGenerale"));
        assertNull(((Map<?, ?>) s.get("assiduite")).get("taux"));
        assertEquals(List.of(), s.get("alertes"));
        assertEquals(0, s.get("devoirsEchus"));
        assertFalse(s.containsKey("score"));
        assertFalse(s.containsKey("probabiliteReussite"));
        assertFalse(s.containsKey("risque"));
    }

    @Test
    void chaqueRegleDeclencheeEstCiteeAvecSaValeur() {
        Mockito.when(bulletins.bulletin("e2")).thenReturn(bulletin(8.5, "Mathématiques", 6.25, "Histoire", 12.0));
        // 4 présents, 0 retard, 4 absents, 0 excusé → 50 % sur 8 séances
        Mockito.when(assiduites.assiduite("e2")).thenReturn(assiduite(4, 0, 4, 0));
        Mockito.when(devoirs.devoirsEleve("e2")).thenReturn(List.of(
                devoir("Dissertation", "2026-11-10", "NON_RENDU"),          // échu, non rendu → R4
                devoir("Résumé", "2026-11-12T23:59", "EN_RETARD"),          // échu, rendu en retard → pas R4
                devoir("Exposé", "2026-11-20", "NON_RENDU")));               // pas encore échu

        Map<String, Object> s = service.synthese("e2", AUJOURDHUI);
        @SuppressWarnings("unchecked")
        List<String> alertes = (List<String>) s.get("alertes");

        assertEquals("a_suivre", s.get("niveau"));
        assertTrue(alertes.contains("Moyenne générale de 8,5/20, inférieure à 10/20 (R1)."), alertes.toString());
        assertTrue(alertes.contains("Moyenne de 6,25/20 en Mathématiques (R2)."), alertes.toString());
        assertTrue(alertes.contains("Taux d'assiduité de 50 % sur 8 séances notées, inférieur à 80 % (R3)."), alertes.toString());
        assertTrue(alertes.contains("4 absences non excusées (R3)."), alertes.toString());
        assertTrue(alertes.contains("1 devoir non rendu après la date limite (R4)."), alertes.toString());
        assertEquals(5, alertes.size(), alertes.toString());
        assertEquals(1, ((List<?>) s.get("matieresSous10")).size());
        assertEquals(2, s.get("devoirsEchus"));
        assertEquals(List.of(Map.of("titre", "Dissertation", "cours", "Français", "echeance", "2026-11-10")), s.get("devoirsNonRendus"));
        assertEquals(SyntheseSuiviEleveService.REGLES, s.get("regles"));
    }

    @Test
    void sousLesSeuilsAucuneAlerte() {
        Mockito.when(bulletins.bulletin("e3")).thenReturn(bulletin(13.4, "Mathématiques", 14.0, "Histoire", 12.8));
        // 3 séances seulement à 33 % : sous le minimum de 5 séances, le taux ne déclenche pas R3 ; 2 absences < 3
        Mockito.when(assiduites.assiduite("e3")).thenReturn(assiduite(1, 0, 2, 0));
        Mockito.when(devoirs.devoirsEleve("e3")).thenReturn(List.of(devoir("Dissertation", "2026-11-10", "RENDU")));

        Map<String, Object> s = service.synthese("e3", AUJOURDHUI);

        assertEquals("sans_alerte", s.get("niveau"));
        assertEquals(List.of(), s.get("alertes"));
        assertEquals(13.4, s.get("moyenneGenerale"));
        assertEquals(33.3, ((Map<?, ?>) s.get("assiduite")).get("taux"));
    }
}

package ecole_221.sn.ecole.modules.admin.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Doubles réservations détectées par le serveur ; un cours magistral commun n'est pas un conflit. */
class ConflitsPlanningTest {

    private static TimeSlot c(String id, String jour, String debut, String fin, String matiere, String prof, String salle, String classe) {
        return TimeSlotFactory.reconstruct(id, jour, debut, fin, matiere, "CM", prof, salle, null, classe);
    }

    private static final Map<String, String> NOMS = Map.of("cl-l1", "L1 Info", "cl-l2", "L2 Info");

    @Test
    void salleProfesseurEtClasseDoublementReserves() {
        List<String> r = ConflitsPlanning.detecter(List.of(
                c("1", "Lundi", "08:00", "10:00", "Algorithmique", "Awa Sarr", "Salle 101", "cl-l1"),
                c("2", "Lundi", "09:00", "11:00", "Réseaux", "Modou Fall", "Salle 101", "cl-l2"),     // même salle, chevauche
                c("3", "Lundi", "09:30", "10:30", "Bases de données", "Awa Sarr", "Salle 202", "cl-l2"), // même prof + même classe que 2
                c("4", "Lundi", "10:00", "12:00", "Anglais", "Fatou Sy", "Salle 303", "cl-l1")),        // commence quand 1 finit : OK
                NOMS);
        assertThat(r).anyMatch(m -> m.startsWith("Salle « Salle 101 » occupée deux fois le Lundi 08:00–10:00 / 09:00–11:00"))
                .anyMatch(m -> m.startsWith("Professeur « Awa Sarr »") && m.contains("Algorithmique – L1 Info"))
                .anyMatch(m -> m.startsWith("Classe « L2 Info »"));
        assertThat(r).noneMatch(m -> m.contains("Anglais"));
    }

    @Test
    void coursMagistralCommunAPlusieursClassesAutorise() {
        assertThat(ConflitsPlanning.detecter(List.of(
                c("1", "Mardi", "08:00", "10:00", "Mathématiques", "Awa Sarr", "Amphi A", "cl-l1"),
                c("2", "Mardi", "08:00", "10:00", "Mathématiques", "Awa Sarr", "Amphi A", "cl-l2")), NOMS)).isEmpty();
    }

    @Test
    void jourDifferentSalleNonAssigneeEtHeuresVariees() {
        assertThat(ConflitsPlanning.detecter(List.of(
                c("1", "Lundi", "08:00", "10:00", "A", "P1", "Salle 101", "cl-l1"),
                c("2", "Mardi", "08:00", "10:00", "B", "P2", "Salle 101", "cl-l2"),
                c("3", "Lundi", "8h", "10h", "C", "P3", "Salle non assignée", "cl-l2"),
                c("4", "Lundi", "08:00", "", "D", "P4", "Salle non assignée", "x")), NOMS)).isEmpty();
        assertThat(ConflitsPlanning.detecter(List.of(
                c("1", "Lundi", "8h", "10h", "A", "P1", "Salle 101", "cl-l1"),
                c("2", "Lundi", "09:15", "09:45", "B", "P2", "salle 101", "cl-l2")), NOMS)).hasSize(1);
    }
}

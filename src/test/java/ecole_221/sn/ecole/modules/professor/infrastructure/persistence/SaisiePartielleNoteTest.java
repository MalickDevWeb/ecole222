package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** FEAT-003 Q2 (Agent 01) : saisir une seule note ne doit pas effacer l'autre, ni inventer de zéro. */
class SaisiePartielleNoteTest {

    @Test
    void examenSaisiApresLeCcConserveLeCc() {
        var repo = new InMemoryProfessorRepository();
        repo.saveGrade("e1", "c1", 14.0, null);
        repo.saveGrade("e1", "c1", null, 16.0);
        var note = repo.getGrade("e1", "c1");
        assertEquals(14.0, note.get("cc"));
        assertEquals(16.0, note.get("examen"));
        assertEquals(15.2, note.get("average"));
    }

    @Test
    void premiereSaisiePartielleNInventePasDeZero() {
        var repo = new InMemoryProfessorRepository();
        repo.saveGrade("e2", "c1", null, 12.0);
        assertNull(repo.getGrade("e2", "c1").get("cc"));
    }
}

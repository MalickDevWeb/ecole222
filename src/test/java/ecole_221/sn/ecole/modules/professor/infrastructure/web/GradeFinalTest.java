package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.professor.application.port.dto.GradeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Moyenne 40 % CC + 60 % examen, arrondie au centième. */
class GradeFinalTest {

    @Test
    void moyenneArrondie() {
        assertEquals(15.6, GradeDto.calculateFinalGrade(16.5, 15.0));
        assertEquals(13.33, GradeDto.calculateFinalGrade(13.33, 13.33));
        assertEquals(6.0, GradeDto.calculateFinalGrade(15.0, null));
    }
}

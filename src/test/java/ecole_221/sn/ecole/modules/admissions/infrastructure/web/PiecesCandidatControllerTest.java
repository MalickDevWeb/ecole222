package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort.FichierRelu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pièces d'un dossier remises par le serveur (les PDF refusés par Cloudinary s'ouvrent). */
class PiecesCandidatControllerTest {

    private static final String LIEN_PDF = "https://res.cloudinary.com/moncompte/image/upload/v1/file-releves.pdf";

    private CandidateJpaRepository candidats;
    private MediaStoragePort stockage;
    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        candidats = Mockito.mock(CandidateJpaRepository.class);
        stockage = Mockito.mock(MediaStoragePort.class);
        mvc = MockMvcBuilders.standaloneSetup(new PiecesCandidatController(candidats, stockage)).build();
        CandidateJpaEntity dossier = new CandidateJpaEntity();
        dossier.setId("CAND-1");
        dossier.setAdditionalInfo("{\"relevesNotesUrl\":\"" + LIEN_PDF + "\",\"diplomeUrl\":\"https://res.cloudinary.com/moncompte/raw/upload/v1/cv.docx\"}");
        when(candidats.findById("CAND-1")).thenReturn(Optional.of(dossier));
        when(candidats.findById("CAND-AUTRE-ECOLE")).thenReturn(Optional.empty());
    }

    @Test
    void pdfRemisAfficheDansLeNavigateur() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(stockage.relire(LIEN_PDF)).thenReturn(Optional.of(new FichierRelu(pdf, "application/pdf")));
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/releves"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"releves.pdf\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void fichierNonAffichableProposeAuTelechargement() throws Exception {
        when(stockage.relire(Mockito.contains("cv.docx"))).thenReturn(Optional.of(new FichierRelu(new byte[]{1}, "text/html")));
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/diplome"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"diplome\""));
    }

    @Test
    void refusClairs() throws Exception {
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/autre")).andExpect(status().isNotFound());
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/photo")).andExpect(status().isNotFound());   // pas jointe
        mvc.perform(get("/admissions/candidats/CAND-AUTRE-ECOLE/pieces/releves")).andExpect(status().isNotFound());
        when(stockage.relire(LIEN_PDF)).thenReturn(Optional.empty());
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/releves")).andExpect(status().isNotFound());
    }

    @Test
    void stockageIndisponible() throws Exception {
        when(stockage.relire(LIEN_PDF)).thenThrow(new IOException("HTTP 500"));
        mvc.perform(get("/admissions/candidats/CAND-1/pieces/releves")).andExpect(status().isBadGateway());
    }
}

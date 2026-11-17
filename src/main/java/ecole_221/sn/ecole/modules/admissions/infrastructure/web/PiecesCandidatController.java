package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Pièces d'un dossier de candidature, remises par le serveur à l'administration de l'école (16/11).
 * <p>
 * Avant : l'écran ouvrait le lien Cloudinary public. Les PDF étaient refusés (401, diffusion des PDF bloquée sur
 * les comptes gratuits) et la carte d'identité n'était qu'une image. Ici le fichier est relu par l'API authentifiée
 * du stockage. Accès : droit « admissions » (préfixe /admissions, PermissionsInterceptor) ; dossier filtré par école
 * (@TenantId sur CandidateJpaEntity) : le dossier d'une autre école est « introuvable ».
 */
@RestController
@RequestMapping({"/admissions", "/v1/admissions"})
@Tag(name = "Admissions")
public class PiecesCandidatController {

    /** Pièce demandée → clé du lien dans candidates.additional_info (portail public et dossier déposé par un parent). */
    static final Map<String, String> CLES = Map.of(
            "pieceIdentite", "pieceIdentiteUrl",
            "diplome", "diplomeUrl",
            "releves", "relevesNotesUrl",
            "photo", "photoIdentiteUrl");

    private final CandidateJpaRepository candidats;
    private final MediaStoragePort stockage;

    public PiecesCandidatController(CandidateJpaRepository candidats, MediaStoragePort stockage) {
        this.candidats = candidats;
        this.stockage = stockage;
    }

    @GetMapping("/candidats/{id}/pieces/{piece}")
    @Operation(summary = "Pièce d'un dossier de candidature",
            description = "pieceIdentite, diplome, releves ou photo ; le fichier est relu par le serveur (PDF compris)")
    public ResponseEntity<byte[]> piece(@PathVariable String id, @PathVariable String piece) {
        // Pièces du formulaire, ou pièce exigée par une règle de l'école (« passeport » → pieceSupp.passeport)
        String cle = CLES.containsKey(piece) ? CLES.get(piece)
                : piece != null && piece.matches("[a-z0-9-]{1,40}") ? "pieceSupp." + piece : null;
        if (cle == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce inconnue.");
        }
        CandidateJpaEntity dossier = candidats.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier de candidature introuvable."));
        return ecole_221.sn.ecole.modules.media.infrastructure.web.RemiseFichier.remettre(
                stockage, AdmissionController.lireInfo(dossier.getAdditionalInfo(), cle), piece);
    }
}

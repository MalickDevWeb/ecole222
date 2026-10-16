package ecole_221.sn.ecole.modules.media.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ecole_221.sn.ecole.modules.media.application.port.usecase.UploadMediaUseCase;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/media")
@Tag(name = "Media", description = "Gestion des médias — upload sécurisé de fichiers vers Cloudinary")
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 Mo
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "application/pdf"
    );

    private final UploadMediaUseCase uploadMediaUseCase;
    private final ActionLogger actionLogger;

    public UploadController(UploadMediaUseCase uploadMediaUseCase, ActionLogger actionLogger) {
        this.uploadMediaUseCase = uploadMediaUseCase;
        this.actionLogger = actionLogger;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(
        summary = "Upload sécurisé d'un fichier",
        description = "Téléverse un document (PDF, PNG, JPG) vers Cloudinary avec validation de taille et format."
    )
    public ResponseEntity<UploadedFile> uploadFile(
            @Parameter(description = "Fichier à uploader") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Identifiant de l'utilisateur (optionnel)") @RequestParam(value = "userId", required = false) String userId)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Le fichier sélectionné est vide.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("Taille de fichier excessive (" + (file.getSize() / 1024 / 1024) + " Mo). Limite autorisée : 10 Mo.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Format non autorisé (" + contentType + "). Seuls PDF, PNG, JPG et WEBP sont acceptés.");
        }

        String fileName = file.getOriginalFilename();
        UploadedFile uploadedFile = uploadMediaUseCase.upload(
                file.getBytes(),
                fileName,
                contentType,
                userId
        );

        actionLogger.logMediaAction("UPLOAD_FILE", userId != null ? userId : "anonymous", fileName,
            "Upload fichier sécurisé: " + fileName + " (" + contentType + ", " + file.getSize() + " octets)");

        return ResponseEntity.ok(uploadedFile);
    }
}

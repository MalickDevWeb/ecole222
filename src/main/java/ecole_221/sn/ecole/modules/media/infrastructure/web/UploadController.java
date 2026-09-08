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
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.io.IOException;

@RestController
@RequestMapping("/media")
@CrossOrigin(origins = "*")
@Tag(name = "Media", description = "Gestion des médias — upload fichiers vers Cloudinary")
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final UploadMediaUseCase uploadMediaUseCase;
    private final ActionLogger actionLogger;

    public UploadController(UploadMediaUseCase uploadMediaUseCase, ActionLogger actionLogger) {
        this.uploadMediaUseCase = uploadMediaUseCase;
        this.actionLogger = actionLogger;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(
        summary = "Upload d'un fichier",
        description = "Télécharge un fichier vers Cloudinary et retourne l'URL publique ainsi que les métadonnées du fichier."
    )
    public ResponseEntity<UploadedFile> uploadFile(
            @Parameter(description = "Fichier à uploader") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Identifiant de l'utilisateur (optionnel)") @RequestParam(value = "userId", required = false) String userId)
            throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        UploadedFile uploadedFile = uploadMediaUseCase.upload(
                file.getBytes(),
                fileName,
                contentType,
                userId
        );
        
        actionLogger.logMediaAction("UPLOAD_FILE", userId != null ? userId : "anonymous", fileName, 
            "Upload fichier: " + fileName + " (" + contentType + ")");
        
        return ResponseEntity.ok(uploadedFile);
    }
}

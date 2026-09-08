package ecole_221.sn.ecole.modules.media.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.media.application.port.usecase.UploadMediaUseCase;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFileFactory;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;
import ecole_221.sn.ecole.modules.media.domain.port.repository.MediaRepositoryPort;
import ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException;

import java.io.IOException;
import java.util.Optional;

@Service
public class UploadMediaService implements UploadMediaUseCase {

    private final MediaStoragePort mediaStoragePort;
    private final UserRepositoryPort userRepositoryPort;
    private final MediaRepositoryPort mediaRepositoryPort;

    public UploadMediaService(MediaStoragePort mediaStoragePort, UserRepositoryPort userRepositoryPort, 
                              MediaRepositoryPort mediaRepositoryPort) {
        this.mediaStoragePort = mediaStoragePort;
        this.userRepositoryPort = userRepositoryPort;
        this.mediaRepositoryPort = mediaRepositoryPort;
    }

    @Override
    public UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException {
        return upload(fileBytes, fileName, contentType, null);
    }

    @Override
    public UploadedFile upload(byte[] fileBytes, String fileName, String contentType, String userId) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new InvalidFileException("Le fichier ne peut pas être vide");
        }
        
        UploadedFile uploadedFile = mediaStoragePort.upload(fileBytes, fileName, contentType);
        
        // Sauvegarder les métadonnées du fichier
        mediaRepositoryPort.save(uploadedFile);

        if (userId != null && !userId.isBlank()) {
            Optional<User> userOpt = userRepositoryPort.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setPhotoUrl(uploadedFile.getUrl());
                userRepositoryPort.save(user);
                System.out.println(">>> Photo de l'utilisateur " + userId + " mise à jour : " + uploadedFile.getUrl());
            } else {
                System.out.println("[WARN] Utilisateur " + userId + " introuvable pour l'association d'image.");
            }
        }

        return uploadedFile;
    }
}

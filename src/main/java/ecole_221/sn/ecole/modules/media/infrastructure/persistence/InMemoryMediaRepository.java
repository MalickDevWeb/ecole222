package ecole_221.sn.ecole.modules.media.infrastructure.persistence;

import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import ecole_221.sn.ecole.modules.media.domain.port.repository.MediaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMediaRepository implements MediaRepositoryPort {
    
    private final Map<String, UploadedFile> filesById = new ConcurrentHashMap<>();
    private final Map<String, UploadedFile> filesByPublicId = new ConcurrentHashMap<>();
    private final Map<String, List<UploadedFile>> filesByUserId = new ConcurrentHashMap<>();
    private final Map<String, List<UploadedFile>> filesByType = new ConcurrentHashMap<>();

    @Override
    public UploadedFile save(UploadedFile uploadedFile) {
        filesById.put(uploadedFile.getId(), uploadedFile);
        filesByPublicId.put(uploadedFile.getPublicId(), uploadedFile);
        
        // Index par utilisateur (si applicable)
        // Note: UploadedFile n'a pas de champ userId direct, 
        // c'est géré au niveau de l'application via User.setPhotoUrl()
        
        // Index par type
        filesByType.computeIfAbsent(uploadedFile.getType().mimeType(), k -> new ArrayList<>())
                   .add(uploadedFile);
        
        return uploadedFile;
    }

    @Override
    public Optional<UploadedFile> findById(String id) {
        return Optional.ofNullable(filesById.get(id));
    }

    @Override
    public Optional<UploadedFile> findByPublicId(String publicId) {
        return Optional.ofNullable(filesByPublicId.get(publicId));
    }

    @Override
    public List<UploadedFile> findByUserId(String userId) {
        // Pour l'instant, cette méthode retourne une liste vide
        // car la relation utilisateur-fichier est gérée via User.setPhotoUrl()
        // Une implémentation future pourrait ajouter une table de liaison
        return Collections.emptyList();
    }

    @Override
    public List<UploadedFile> findByType(String mimeType) {
        return filesByType.getOrDefault(mimeType, Collections.emptyList());
    }

    @Override
    public void deleteById(String id) {
        UploadedFile file = filesById.remove(id);
        if (file != null) {
            filesByPublicId.remove(file.getPublicId());
            filesByType.getOrDefault(file.getType().mimeType(), Collections.emptyList())
                       .remove(file);
        }
    }

    @Override
    public void deleteByPublicId(String publicId) {
        UploadedFile file = filesByPublicId.remove(publicId);
        if (file != null) {
            filesById.remove(file.getId());
            filesByType.getOrDefault(file.getType().mimeType(), Collections.emptyList())
                       .remove(file);
        }
    }
}
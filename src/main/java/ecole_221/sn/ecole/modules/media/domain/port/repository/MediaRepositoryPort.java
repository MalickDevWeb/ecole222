package ecole_221.sn.ecole.modules.media.domain.port.repository;

import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import java.util.List;
import java.util.Optional;

public interface MediaRepositoryPort {
    UploadedFile save(UploadedFile uploadedFile);
    Optional<UploadedFile> findById(String id);
    Optional<UploadedFile> findByPublicId(String publicId);
    List<UploadedFile> findByUserId(String userId);
    List<UploadedFile> findByType(String mimeType);
    void deleteById(String id);
    void deleteByPublicId(String publicId);
}
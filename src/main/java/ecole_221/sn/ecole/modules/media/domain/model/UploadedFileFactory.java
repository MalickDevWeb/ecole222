package ecole_221.sn.ecole.modules.media.domain.model;

import ecole_221.sn.ecole.modules.media.domain.valueobject.FileSize;
import ecole_221.sn.ecole.modules.media.domain.valueobject.MediaType;
import ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException;
import ecole_221.sn.ecole.modules.media.domain.exception.FileSizeExceededException;

import java.time.LocalDateTime;
import java.util.UUID;

public class UploadedFileFactory {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    // Package-private constructor to prevent direct instantiation
    UploadedFileFactory() {
    }

    public static UploadedFile create(String name, String url, String publicId, long size, String mimeType) {
        validateFileSize(size);
        validateMimeType(mimeType);
        
        String id = UUID.randomUUID().toString();
        FileSize fileSize = new FileSize(size);
        MediaType mediaType = new MediaType(mimeType);
        LocalDateTime createdAt = LocalDateTime.now();
        
        return UploadedFile.createInternal(id, name, url, publicId, fileSize, mediaType, createdAt);
    }

    public static UploadedFile reconstitute(String id, String name, String url, String publicId, 
                                           long size, String mimeType, LocalDateTime createdAt) {
        FileSize fileSize = new FileSize(size);
        MediaType mediaType = new MediaType(mimeType);
        
        return UploadedFile.createInternal(id, name, url, publicId, fileSize, mediaType, createdAt);
    }

    private static void validateFileSize(long size) {
        if (size <= 0) {
            throw new InvalidFileException("La taille du fichier doit être positive");
        }
        if (size > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(MAX_FILE_SIZE, size);
        }
    }

    private static void validateMimeType(String mimeType) {
        MediaType mediaType = new MediaType(mimeType);
        if (!mediaType.isAllowed()) {
            throw new InvalidFileException("Type de fichier non autorisé: " + mimeType);
        }
    }
}
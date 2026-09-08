package ecole_221.sn.ecole.modules.media.domain.model;

import ecole_221.sn.ecole.modules.media.domain.valueobject.FileSize;
import ecole_221.sn.ecole.modules.media.domain.valueobject.MediaType;

import java.time.LocalDateTime;

public class UploadedFile {
    private final String id;
    private final String name;
    private final String url;
    private final String publicId;
    private final FileSize size;
    private final MediaType type;
    private final LocalDateTime createdAt;

    private UploadedFile(String id, String name, String url, String publicId, FileSize size, MediaType type, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.publicId = publicId;
        this.size = size;
        this.type = type;
        this.createdAt = createdAt;
    }
    
    // Méthode package-private pour la Factory
    static UploadedFile createInternal(String id, String name, String url, String publicId, FileSize size, MediaType type, LocalDateTime createdAt) {
        return new UploadedFile(id, name, url, publicId, size, type, createdAt);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getPublicId() { return publicId; }
    public FileSize getSize() { return size; }
    public MediaType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

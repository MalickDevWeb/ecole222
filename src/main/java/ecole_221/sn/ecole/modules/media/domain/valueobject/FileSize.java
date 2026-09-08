package ecole_221.sn.ecole.modules.media.domain.valueobject;

public record FileSize(long bytes) {
    public FileSize {
        if (bytes < 0) {
            throw new IllegalArgumentException("La taille du fichier ne peut pas être négative");
        }
    }

    public long toKilobytes() {
        return bytes / 1024;
    }

    public long toMegabytes() {
        return bytes / (1024 * 1024);
    }

    public boolean isEmpty() {
        return bytes == 0;
    }

    public boolean exceeds(long maxBytes) {
        return bytes > maxBytes;
    }
}
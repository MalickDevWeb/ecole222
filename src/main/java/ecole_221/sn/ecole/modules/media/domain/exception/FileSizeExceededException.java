package ecole_221.sn.ecole.modules.media.domain.exception;

public class FileSizeExceededException extends InvalidFileException {
    private final long maxSize;
    private final long actualSize;

    public FileSizeExceededException(long maxSize, long actualSize) {
        super(String.format("La taille du fichier (%d bytes) dépasse la taille maximale autorisée (%d bytes)", 
                           actualSize, maxSize));
        this.maxSize = maxSize;
        this.actualSize = actualSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getActualSize() {
        return actualSize;
    }
}
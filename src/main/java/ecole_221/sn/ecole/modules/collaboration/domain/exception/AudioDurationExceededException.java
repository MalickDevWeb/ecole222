package ecole_221.sn.ecole.modules.collaboration.domain.exception;

public class AudioDurationExceededException extends RuntimeException {
    private final long maxDuration;
    private final long actualDuration;

    public AudioDurationExceededException(long maxDuration, long actualDuration) {
        super(String.format("La durée de l'audio (%d secondes) dépasse la durée maximale autorisée (%d secondes)", 
                           actualDuration, maxDuration));
        this.maxDuration = maxDuration;
        this.actualDuration = actualDuration;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    public long getActualDuration() {
        return actualDuration;
    }
}
package ecole_221.sn.ecole.modules.planning.domain.exception;

/**
 * Exception métier lancée lorsqu'un créneau horaire n'est pas trouvé.
 */
public class SlotNotFoundException extends RuntimeException {
    
    public SlotNotFoundException(String message) {
        super(message);
    }
}
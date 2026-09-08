package ecole_221.sn.ecole.modules.planning.domain.exception;

/**
 * Exception métier lancée lorsqu'une salle n'est pas trouvée.
 */
public class RoomNotFoundException extends RuntimeException {
    
    public RoomNotFoundException(String message) {
        super(message);
    }
}
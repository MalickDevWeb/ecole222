package ecole_221.sn.ecole.modules.planning.application.port.exception;

/**
 * Exception levée lorsqu'une salle n'est pas trouvée.
 */
public class RoomNotFoundException extends RuntimeException {
    
    private final String roomId;
    
    public RoomNotFoundException(String roomId) {
        super("Salle non trouvée avec l'identifiant: " + roomId);
        this.roomId = roomId;
    }
    
    public String getRoomId() {
        return roomId;
    }
}
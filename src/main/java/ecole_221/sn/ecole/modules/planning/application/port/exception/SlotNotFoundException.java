package ecole_221.sn.ecole.modules.planning.application.port.exception;

/**
 * Exception levée lorsqu'un créneau horaire n'est pas trouvé.
 */
public class SlotNotFoundException extends RuntimeException {
    
    private final String slotId;
    
    public SlotNotFoundException(String slotId) {
        super("Créneau horaire non trouvé avec l'identifiant: " + slotId);
        this.slotId = slotId;
    }
    
    public String getSlotId() {
        return slotId;
    }
}
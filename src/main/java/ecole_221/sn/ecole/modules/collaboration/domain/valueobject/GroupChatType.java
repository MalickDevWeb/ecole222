package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

public record GroupChatType(String type) {
    public static final GroupChatType INDIVIDUAL = new GroupChatType("INDIVIDUAL");
    public static final GroupChatType GROUP = new GroupChatType("GROUP");
    public static final GroupChatType BROADCAST = new GroupChatType("BROADCAST");
    public static final GroupChatType CHANNEL = new GroupChatType("CHANNEL");
    
    public GroupChatType {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Le type de chat ne peut pas être vide");
        }
    }
    
    public boolean isIndividual() {
        return "INDIVIDUAL".equals(type);
    }
    
    public boolean isGroup() {
        return "GROUP".equals(type);
    }
    
    public boolean isBroadcast() {
        return "BROADCAST".equals(type);
    }
    
    public boolean isChannel() {
        return "CHANNEL".equals(type);
    }
    
    public boolean supportsMultipleParticipants() {
        return isGroup() || isBroadcast() || isChannel();
    }
}
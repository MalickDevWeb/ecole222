package ecole_221.sn.ecole.modules.finance.domain.model;

import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de Expense.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class ExpenseFactory {
    
    /**
     * Crée une nouvelle dépense avec un ID généré et le statut "En Attente" par défaut.
     */
    public static Expense create(String label, ExpenseCategory category, Long amount, 
                                LocalDateTime date, String description) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new Expense(id, label, category, amount, date, description, 
                          ExpenseStatus.EN_ATTENTE, now, now);
    }
    
    /**
     * Crée une nouvelle dépense avec un ID généré et un statut personnalisé.
     */
    public static Expense create(String label, ExpenseCategory category, Long amount, 
                                LocalDateTime date, String description, ExpenseStatus status) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new Expense(id, label, category, amount, date, description, status, now, now);
    }
    
    /**
     * Reconstruit une dépense existante (pour la persistance).
     */
    public static Expense reconstruct(String id, String label, ExpenseCategory category, 
                                     Long amount, LocalDateTime date, String description, 
                                     ExpenseStatus status, LocalDateTime createdAt, 
                                     LocalDateTime updatedAt) {
        return new Expense(id, label, category, amount, date, description, status, 
                          createdAt, updatedAt);
    }
}
package ecole_221.sn.ecole.modules.admissions.domain.model;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;

import java.util.Objects;

/**
 * Agrégat racine représentant un candidat.
 * Constructeur privé pour garantir l'invariance - utiliser CandidateFactory.
 */
public class Candidate {
    
    private final String id;
    private String nom;
    private String prenom;
    private final String email;
    private String telephone;
    private CandidateStatus statut;
    private Double note;
    private final String filiereDemandee;
    
    // Constructeur package-private pour DDD Factory pattern
    Candidate(String id, String nom, String prenom, String email, String telephone, 
                    CandidateStatus statut, Double note, String filiereDemandee) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
        this.email = Objects.requireNonNull(email, "L'email ne peut pas être null");
        this.telephone = telephone;
        this.statut = Objects.requireNonNull(statut, "Le statut ne peut pas être null");
        this.note = note;
        this.filiereDemandee = Objects.requireNonNull(filiereDemandee, "La filière demandée ne peut pas être null");
        
        // Validation de l'email
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email invalide");
        }
        
        // Validation de la note
        if (note != null && (note < 0 || note > 20)) {
            throw new IllegalArgumentException("La note doit être entre 0 et 20");
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public CandidateStatus getStatut() { return statut; }
    public Double getNote() { return note; }
    public String getFiliereDemandee() { return filiereDemandee; }
    
    // Setters (pour comportements métier)
    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom ne peut pas être null");
    }

    public void setPrenom(String prenom) {
        this.prenom = Objects.requireNonNull(prenom, "Le prénom ne peut pas être null");
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public void setStatut(CandidateStatus statut) {
        this.statut = Objects.requireNonNull(statut, "Le statut ne peut pas être null");
    }

    public void setStatut(String statut) {
        this.statut = CandidateStatus.valueOf(statut);
    }

    public void setNote(Double note) {
        if (note != null && (note < 0 || note > 20)) {
            throw new IllegalArgumentException("La note doit être entre 0 et 20");
        }
        this.note = note;
    }
    
    // Comportements métier
    public void accept() {
        this.statut = CandidateStatus.ACCEPTED;
    }
    
    public void reject() {
        this.statut = CandidateStatus.REJECTED;
    }
    
    public void markAsInProgress() {
        this.statut = CandidateStatus.EN_COURS;
    }
    
    public void finalizeStatus() {
        this.statut = CandidateStatus.FINALISE;
    }
    
    public boolean isAccepted() {
        return this.statut.equals(CandidateStatus.ACCEPTED);
    }
    
    public boolean isRejected() {
        return this.statut.equals(CandidateStatus.REJECTED);
    }
    
    public boolean isPending() {
        return this.statut.equals(CandidateStatus.EN_ATTENTE);
    }
    
    public boolean hasPassed() {
        return note != null && note >= 10;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(id, candidate.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Candidate{" +
                "id='" + id + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", statut=" + statut +
                ", note=" + note +
                ", filiereDemandee='" + filiereDemandee + '\'' +
                '}';
    }
}
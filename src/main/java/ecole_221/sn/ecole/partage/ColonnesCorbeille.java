package ecole_221.sn.ecole.partage;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

/**
 * Colonnes de la corbeille (suppression douce, décision du Tech Lead du 15/11 ; migration V21).
 *
 * <p>Chaque entité qui en hérite porte aussi {@code @SQLRestriction("supprime_le is null")} : une ligne mise à la
 * corbeille n'est plus lue par aucune requête JPA (findById, findAll, requêtes dérivées, JPQL, comptages).</p>
 *
 * <p>Hibernate n'écrit jamais ces colonnes ({@code insertable = false, updatable = false}) : seule la corbeille
 * (CorbeilleService, en SQL et toujours filtrée par école) les pose, les efface ou purge la ligne. Un {@code save()}
 * ultérieur d'une entité chargée avant la suppression ne peut donc pas « restaurer » la ligne par accident.</p>
 */
@MappedSuperclass
public abstract class ColonnesCorbeille {

    @Column(name = "supprime_le", insertable = false, updatable = false)
    private LocalDateTime supprimeLe;

    @Column(name = "supprime_par", insertable = false, updatable = false)
    private String supprimePar;

    @Column(name = "supprime_libelle", insertable = false, updatable = false)
    private String supprimeLibelle;

    public LocalDateTime getSupprimeLe() { return supprimeLe; }
    public String getSupprimePar() { return supprimePar; }
    public String getSupprimeLibelle() { return supprimeLibelle; }
}

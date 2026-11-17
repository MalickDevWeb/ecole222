package ecole_221.sn.ecole.modules.archivage.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Transactions JDBC propres à l'archivage, sur la même base, sans passer par JPA (aucune entité @TenantId n'est lue :
 * l'école est toujours un paramètre explicite). Le gestionnaire n'est PAS un bean Spring : il ne remplace pas le
 * gestionnaire JPA de l'application.
 */
public class TransactionsArchivage {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate ecriture;
    private final TransactionTemplate instantane;

    public TransactionsArchivage(DataSource source) {
        this.jdbc = new JdbcTemplate(source);
        DataSourceTransactionManager gestionnaire = new DataSourceTransactionManager(source);
        this.ecriture = new TransactionTemplate(gestionnaire);
        this.instantane = new TransactionTemplate(gestionnaire);
        this.instantane.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.instantane.setReadOnly(true);
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    /** Transaction ordinaire (lecture validée). */
    public TransactionTemplate ecriture() {
        return ecriture;
    }

    /** Instantané cohérent en lecture seule : toutes les tables de l'école lues au même instant. */
    public TransactionTemplate instantane() {
        return instantane;
    }

    /**
     * Formes texte stables pour l'aller-retour des valeurs (dates ISO, nombres à virgule flottante exacts, octets en
     * hexadécimal), dans la transaction en cours seulement.
     */
    public void reglerFormatsTexte() {
        jdbc.execute("SET LOCAL DateStyle = 'ISO, YMD'");
        jdbc.execute("SET LOCAL IntervalStyle = 'postgres'");
        jdbc.execute("SET LOCAL extra_float_digits = 3");
        jdbc.execute("SET LOCAL bytea_output = 'hex'");
    }
}

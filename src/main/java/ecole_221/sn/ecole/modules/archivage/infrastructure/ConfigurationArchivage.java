package ecole_221.sn.ecole.modules.archivage.infrastructure;

import ecole_221.sn.ecole.modules.archivage.application.ArchivageEcolesService;
import ecole_221.sn.ecole.modules.archivage.application.FichiersArchive;
import ecole_221.sn.ecole.modules.archivage.application.JournalArchivage;
import ecole_221.sn.ecole.modules.archivage.application.MoteurArchivage;
import ecole_221.sn.ecole.modules.archivage.application.MoteurRestauration;
import ecole_221.sn.ecole.modules.archivage.application.ReglagesArchivageService;
import ecole_221.sn.ecole.modules.archivage.application.TransactionsArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Assemblage du module d'archivage. Les classes applicatives restent sans annotation Spring (testables à la main sur
 * une vraie base PostgreSQL) ; l'exécuteur est privé au module (un seul fil : une opération à la fois), il ne remplace
 * pas l'exécuteur @Async de l'application.
 */
@Configuration
public class ConfigurationArchivage {

    @Bean
    TransactionsArchivage transactionsArchivage(DataSource source) {
        return new TransactionsArchivage(source);
    }

    @Bean
    JournalArchivage journalArchivage(ArchivagesEcolesPort archivages, JournalAudit audit) {
        return new JournalArchivage(archivages, audit);
    }

    @Bean
    ReglagesArchivageService reglagesArchivageService(JdbcTemplate jdbc, JournalAudit audit) {
        return new ReglagesArchivageService(jdbc, audit);
    }

    @Bean
    FichiersArchive fichiersArchive(@Value("${ecole.archivage.dossier-temporaire:${java.io.tmpdir}}") String dossier) {
        return new FichiersArchive(Path.of(dossier));
    }

    @Bean
    MoteurArchivage moteurArchivage(TransactionsArchivage tx, ArchivagesEcolesPort archivages, EcolesArchivablesPort ecoles,
                                    StockageArchive stockage, CleDesArchives cles, JournalArchivage journal,
                                    ReglagesArchivageService reglages, FichiersArchive fichiers,
                                    @Value("${ecole.archivage.purge.lot:1000}") int lot) {
        return new MoteurArchivage(tx, archivages, ecoles, stockage, cles, journal, reglages, fichiers, lot);
    }

    @Bean
    MoteurRestauration moteurRestauration(DataSource source, TransactionsArchivage tx, ArchivagesEcolesPort archivages,
                                          EcolesArchivablesPort ecoles, StockageArchive stockage, CleDesArchives cles,
                                          JournalArchivage journal, FichiersArchive fichiers) {
        return new MoteurRestauration(source, tx, archivages, ecoles, stockage, cles, journal, fichiers);
    }

    /**
     * L'exécuteur (un seul fil) n'est PAS déclaré comme bean : un bean Executor ferait renoncer Spring Boot à son
     * exécuteur @Async par défaut. Il est fermé avec le service.
     */
    @Bean(destroyMethod = "fermer")
    ArchivageEcolesService archivageEcolesService(ArchivagesEcolesPort archivages, EcolesArchivablesPort ecoles,
                                                  StockageArchive stockage, CleDesArchives cles,
                                                  ReglagesArchivageService reglages, JournalArchivage journal,
                                                  MoteurArchivage moteur, MoteurRestauration restauration,
                                                  TransactionsArchivage tx) {
        ExecutorService executeur = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "archivage-ecoles");
            t.setDaemon(true);
            return t;
        });
        return new ArchivageEcolesService(archivages, ecoles, stockage, cles, reglages, journal, moteur, restauration, tx,
                executeur);
    }
}

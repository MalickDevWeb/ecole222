package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Numéros déjà enregistrés remis au format E.164 seulement quand c'est sans ambiguïté (2026-11-16). */
class NormalisationTelephonesTest {

    private JdbcTemplate jdbc;
    private NormalisationTelephones normalisation;

    @BeforeEach
    void base() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:tel-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE users (id VARCHAR(64) PRIMARY KEY, telephone VARCHAR(255) UNIQUE)");
        normalisation = new NormalisationTelephones(jdbc);
    }

    private void compte(String id, String telephone) {
        jdbc.update("INSERT INTO users (id, telephone) VALUES (?, ?)", id, telephone);
    }

    private String telephone(String id) {
        return jdbc.queryForObject("SELECT telephone FROM users WHERE id = ?", String.class, id);
    }

    @Test
    void convertitLesFormesSansAmbiguiteEtLaisseLesAutres() {
        compte("espaces", "+221 77 000 00 01");
        compte("local", "770000002");
        compte("double-zero", "00221 76 000 00 03");
        compte("diaspora", "+33 6 12 34 56 78");
        compte("deja-bon", "+221770000004");
        compte("fixe", "33 800 00 00");          // fixe sénégalais : non accepté par la connexion, laissé
        compte("texte", "voir le dossier");
        compte("sans-numero", null);

        NormalisationTelephones.Bilan b = normalisation.normaliser();

        assertThat(b.convertis()).isEqualTo(4);
        assertThat(b.nonReconnus()).isEqualTo(2);
        assertThat(b.conflits()).isZero();
        assertThat(telephone("espaces")).isEqualTo("+221770000001");
        assertThat(telephone("local")).isEqualTo("+221770000002");
        assertThat(telephone("double-zero")).isEqualTo("+221760000003");
        assertThat(telephone("diaspora")).isEqualTo("+33612345678");
        assertThat(telephone("deja-bon")).isEqualTo("+221770000004");
        assertThat(telephone("fixe")).isEqualTo("33 800 00 00");
        assertThat(telephone("texte")).isEqualTo("voir le dossier");
        assertThat(telephone("sans-numero")).isNull();
    }

    @Test
    void conflitsLaissesTelsQuels() {
        compte("e164", "+221770000005");
        compte("meme-numero", "77 000 00 05");     // déjà porté par un autre compte
        compte("a", "77 000 00 06");
        compte("b", "+221 77 000 00 06");          // deux comptes visent le même numéro : aucun choix arbitraire

        NormalisationTelephones.Bilan b = normalisation.normaliser();

        assertThat(b.convertis()).isZero();
        assertThat(b.conflits()).isEqualTo(3);
        assertThat(telephone("meme-numero")).isEqualTo("77 000 00 05");
        assertThat(telephone("a")).isEqualTo("77 000 00 06");
        assertThat(telephone("b")).isEqualTo("+221 77 000 00 06");
    }

    @Test
    void idempotente() {
        compte("espaces", "+221 77 000 00 07");
        assertThat(normalisation.normaliser().convertis()).isOne();
        NormalisationTelephones.Bilan second = normalisation.normaliser();
        assertThat(second.convertis()).isZero();
        assertThat(second.nonReconnus()).isZero();
        assertThat(telephone("espaces")).isEqualTo("+221770000007");
    }
}

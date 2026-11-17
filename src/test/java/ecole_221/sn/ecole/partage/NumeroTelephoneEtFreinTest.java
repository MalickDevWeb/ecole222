package ecole_221.sn.ecole.partage;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class NumeroTelephoneEtFreinTest {

    @Test
    void numerosSenegalaisEtDiasporaAuFormatE164() {
        assertThat(NumeroTelephone.e164("77 123 45 67")).contains("+221771234567");
        assertThat(NumeroTelephone.e164("00221 70-123-45-67")).contains("+221701234567");
        assertThat(NumeroTelephone.e164("+221 78 123 45 67")).contains("+221781234567");
        assertThat(NumeroTelephone.e164("+33 6 12 34 56 78")).contains("+33612345678");
        assertThat(NumeroTelephone.e164("0033612345678")).contains("+33612345678");
        // fixe sénégalais (33…), préfixe mobile inconnu, numéro local étranger sans indicatif, texte : refusés
        assertThat(NumeroTelephone.e164("+221 33 821 00 00")).isEmpty();
        assertThat(NumeroTelephone.e164("+221 79 123 45 67")).isEmpty();
        assertThat(NumeroTelephone.e164("0612345678")).isEmpty();
        assertThat(NumeroTelephone.e164("awa@exemple.sn")).isEmpty();
        assertThat(NumeroTelephone.e164(null)).isEmpty();
    }

    @Test
    void masqueEtChiffres() {
        assertThat(NumeroTelephone.masquer("+221771234567")).isEqualTo("+221 77 ••• •• 67");
        assertThat(NumeroTelephone.masquer("+33612345678")).doesNotContain("123456");
        assertThat(NumeroTelephone.chiffres("+221771234567")).isEqualTo("221771234567");
        assertThat(NumeroTelephone.ressembleAUnNumero("77 123 45 67")).isTrue();
        assertThat(NumeroTelephone.ressembleAUnNumero("awa@exemple.sn")).isFalse();
        assertThat(NumeroTelephone.ressembleAUnNumero("ETU-2026-0001")).isFalse();
    }

    @Test
    void freinTemporaireParCle() {
        Frein f = new Frein(3, Duration.ofMinutes(15));
        Instant[] t = {Instant.parse("2026-11-16T10:00:00Z")};
        f.horloge = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId z) { return this; }
            @Override public Instant instant() { return t[0]; }
        };
        for (int i = 0; i < 3; i++) f.noter("1.2.3.4");
        assertThat(f.bloque("1.2.3.4")).isTrue();
        assertThat(f.bloque("5.6.7.8")).isFalse();
        assertThat(f.autoriser("1.2.3.4")).isFalse();
        t[0] = t[0].plus(Duration.ofMinutes(15));
        assertThat(f.bloque("1.2.3.4")).isFalse();   // libéré de lui-même à la fin de la fenêtre
        assertThat(f.compte("1.2.3.4")).isZero();
    }
}

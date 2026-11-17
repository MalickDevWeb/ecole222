package ecole_221.sn.ecole.modules.archivage.application.format;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Format des archives : JSON par table avec noms de colonnes, GZIP, nombre de lignes et empreinte par table. */
class FormatArchiveTest {

    private static final TableArchivee TABLE = new TableArchivee("attendances", "DIRECTE", List.of(
            new TableArchivee.Colonne("id", "character varying(255)"),
            new TableArchivee.Colonne("date", "timestamp(6) without time zone"),
            new TableArchivee.Colonne("note", "text")), List.of("id"));

    private static byte[] ecrire(List<String[]> lignes) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (EcritureArchive e = new EcritureArchive(sortie)) {
            e.entete(Map.of("archivageId", "arc-test"));
            e.debutTable(TABLE);
            for (String[] l : lignes) e.ligne(l);
            EcritureArchive.ResumeTable r = e.finTable();
            assertThat(r.lignes()).isEqualTo(lignes.size());
            e.fin(List.of(Map.of("table", "documents", "colonne", "url", "url", "https://res.cloudinary.com/x/y.pdf")),
                    Map.of("lignes", lignes.size()));
        }
        return sortie.toByteArray();
    }

    @Test
    void allerRetourAvecNullsUnicodeEtGuillemets() throws IOException {
        List<String[]> lignes = List.of(
                new String[]{"a-1", "2026-11-16 10:00:00", null},
                new String[]{"a-2", null, "Élève « absent » \"justifié\" — 日本 \n ligne 2"},
                new String[]{"a-3", "2026-11-16 10:00:00.123456", ""});
        List<String[]> relues = new ArrayList<>();
        Map<String, Object> entete = new LinkedHashMap<>();
        List<Map<String, Object>> medias = new ArrayList<>();
        LectureArchive.lire(new ByteArrayInputStream(ecrire(lignes)), new LectureArchive.Visiteur() {
            @Override
            public void entete(Map<String, Object> e) {
                entete.putAll(e);
            }

            @Override
            public void ligne(TableArchivee t, String[] v) {
                assertThat(t.nomsColonnes()).containsExactly("id", "date", "note");
                relues.add(v);
            }

            @Override
            public void medias(List<Map<String, Object>> m) {
                medias.addAll(m);
            }
        });
        assertThat(entete).containsEntry("archivageId", "arc-test").containsEntry("format", EcritureArchive.FORMAT);
        assertThat(relues).hasSize(3);
        for (int i = 0; i < 3; i++) assertThat(relues.get(i)).containsExactly(lignes.get(i));
        assertThat(medias).hasSize(1);
    }

    @Test
    void uneLigneModifieeEstDetecteeParLEmpreinte() throws IOException {
        byte[] gz = ecrire(List.<String[]>of(new String[]{"a-1", "2026-11-16 10:00:00", "présent"}));
        String json = new String(new GZIPInputStream(new ByteArrayInputStream(gz)).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String altere = json.replace("présent", "absent");
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        try (GZIPOutputStream o = new GZIPOutputStream(z)) {
            o.write(altere.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertThatThrownBy(() -> LectureArchive.lire(new ByteArrayInputStream(z.toByteArray()), (t, v) -> { }))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("empreinte");
    }

    @Test
    void uneArchiveTronqueeEstRefusee() throws IOException {
        byte[] gz = ecrire(List.<String[]>of(new String[]{"a-1", null, "x"}));
        byte[] coupe = java.util.Arrays.copyOf(gz, gz.length / 2);
        assertThatThrownBy(() -> LectureArchive.lire(new ByteArrayInputStream(coupe), (t, v) -> { }))
                .isInstanceOf(ArchivageException.class);
    }

    @Test
    void empreinteSansAmbiguite() {
        EmpreinteLignes a = new EmpreinteLignes();
        a.ajouter(new String[]{"ab", "c"});
        EmpreinteLignes b = new EmpreinteLignes();
        b.ajouter(new String[]{"a", "bc"});
        EmpreinteLignes c = new EmpreinteLignes();
        c.ajouter(new String[]{null, "c"});
        EmpreinteLignes d = new EmpreinteLignes();
        d.ajouter(new String[]{"", "c"});
        assertThat(a.hex()).isNotEqualTo(b.hex());
        assertThat(c.hex()).isNotEqualTo(d.hex());
    }
}

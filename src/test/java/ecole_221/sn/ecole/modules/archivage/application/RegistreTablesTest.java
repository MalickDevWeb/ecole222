package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage.Categorie;
import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage.TablePlan;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Registre de l'archivage : ce qui part, ce qui reste, complétude, ordre des clés étrangères. */
class RegistreTablesTest {

    private static List<CatalogueBase.Colonne> cols(String... noms) {
        List<CatalogueBase.Colonne> l = new ArrayList<>();
        for (String n : noms) l.add(new CatalogueBase.Colonne(n, "character varying(255)", n.equals("id"), false, false));
        return l;
    }

    private static CatalogueBase catalogue(Map<String, List<CatalogueBase.Colonne>> tables, List<CatalogueBase.CleEtrangere> fks) {
        Map<String, List<String>> pk = new LinkedHashMap<>();
        tables.forEach((t, c) -> {
            if (c.stream().anyMatch(x -> x.nom().equals("id"))) pk.put(t, List.of("id"));
        });
        return new CatalogueBase(tables, pk, fks, Map.of());
    }

    private static Map<String, List<CatalogueBase.Colonne>> base() {
        Map<String, List<CatalogueBase.Colonne>> t = new LinkedHashMap<>();
        t.put("organizations", cols("id", "slug", "status"));
        t.put("users", cols("id", "email"));
        t.put("audit_logs", cols("id", "organization_id"));
        t.put("domaines_ecoles", cols("id", "organization_id", "hostname"));
        t.put("demandes_transfert", cols("id", "ecole_source_id"));
        t.put("organization_users", cols("id", "organization_id", "user_id"));
        t.put("group_chats", cols("id", "organization_id", "avatar_url"));
        t.put("group_chat_participants", cols("chat_id", "user_id"));
        t.put("roles", cols("id", "organization_id"));
        t.put("role_permissions", cols("id", "role_id", "permission_id"));
        t.put("attendances", cols("id", "organization_id", "student_id"));
        t.put("documents", cols("id", "organization_id", "url"));
        return t;
    }

    @Test
    void lesDonneesMetierPartentLesComptesAuditDomainesEtTransfertsRestent() {
        PlanArchivage plan = RegistreTables.plan(catalogue(base(), List.of()));
        Map<String, TablePlan> parNom = new LinkedHashMap<>();
        plan.tables().forEach(t -> parNom.put(t.nom(), t));
        assertThat(parNom.keySet()).containsExactlyInAnyOrder("organization_users", "group_chats", "group_chat_participants",
                "roles", "role_permissions", "attendances", "documents");
        assertThat(plan.gardees()).contains("organizations", "users", "audit_logs", "domaines_ecoles", "demandes_transfert");
        assertThat(parNom.get("attendances").categorie()).isEqualTo(Categorie.DIRECTE);
        assertThat(parNom.get("attendances").selection()).isEqualTo("t.organization_id = ?");
        // compte de plusieurs écoles : seul le rattachement part
        assertThat(parNom.get("organization_users").categorie()).isEqualTo(Categorie.APPARTENANCES);
        assertThat(parNom.get("organization_users").selection()).contains("o.organization_id <> t.organization_id");
        assertThat(parNom.get("group_chat_participants").categorie()).isEqualTo(Categorie.INDIRECTE);
        assertThat(parNom.get("documents").colonnesMedias()).containsExactly("url");
        assertThat(parNom.get("group_chats").colonnesMedias()).containsExactly("avatar_url");
    }

    @Test
    void enfantsAvantParentsDansLOrdreDePurge() {
        List<CatalogueBase.CleEtrangere> fks = List.of(
                new CatalogueBase.CleEtrangere("fk1", "group_chat_participants", "group_chats", "a"));
        List<String> ordre = RegistreTables.plan(catalogue(base(), fks)).tables().stream().map(TablePlan::nom).toList();
        assertThat(ordre.indexOf("group_chat_participants")).isLessThan(ordre.indexOf("group_chats"));
        assertThat(ordre.indexOf("role_permissions")).isLessThan(ordre.indexOf("roles"));   // lien connu sans clé étrangère
    }

    @Test
    void uneTableSansOrganizationIdNonClasseeBloqueLArchivage() {
        Map<String, List<CatalogueBase.Colonne>> t = base();
        t.put("eleves_inscrits_cours", cols("course_id", "student_id"));
        assertThatThrownBy(() -> RegistreTables.plan(catalogue(t, List.of()))).isInstanceOf(ArchivageException.class)
                .hasMessageContaining("non classée").hasMessageContaining("eleves_inscrits_cours");
    }

    @Test
    void uneTableNouvelleAvecOrganizationIdPartAutomatiquement() {
        Map<String, List<CatalogueBase.Colonne>> t = base();
        t.put("bulletins", cols("id", "organization_id"));
        assertThat(RegistreTables.plan(catalogue(t, List.of())).tables()).extracting(TablePlan::nom).contains("bulletins");
    }

    @Test
    void uneCleEtrangereDUneTableGardeeVersUneTableArchiveeBloque() {
        List<CatalogueBase.CleEtrangere> fks = List.of(new CatalogueBase.CleEtrangere("fk_users_role", "users", "roles", "a"));
        assertThatThrownBy(() -> RegistreTables.plan(catalogue(base(), fks))).isInstanceOf(ArchivageException.class)
                .hasMessageContaining("users → roles");
    }

    @Test
    void cycleDeClesEtrangeresRefuse() {
        Map<String, java.util.Set<String>> parents = Map.of("a", java.util.Set.of("b"), "b", java.util.Set.of("a"));
        Map<String, TablePlan> tables = new LinkedHashMap<>();
        for (String n : List.of("a", "b")) {
            tables.put(n, new TablePlan(n, Categorie.DIRECTE, List.of(), List.of(), Map.of(), "t.organization_id = ?", 1, List.of()));
        }
        assertThatThrownBy(() -> RegistreTables.ordreEnfantsDabord(tables, parents)).isInstanceOf(ArchivageException.class);
    }
}

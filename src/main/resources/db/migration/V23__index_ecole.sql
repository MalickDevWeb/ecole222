-- V23 — Index qui commencent par organization_id (étude docs/architecture/archivage-des-ecoles.md, § 0 et § 3.1).
--
-- Mesuré le 16/11 : 47 des 77 tables qui portent organization_id n'avaient AUCUN index qui commence par cette colonne.
-- Chaque requête d'une école (@TenantId ajoute « organization_id = ? ») lisait alors les lignes de TOUTES les écoles.
-- S'y ajoutent 13 tables dont le seul index sur organization_id est l'index partiel de la corbeille (V21,
-- « WHERE supprime_le IS NOT NULL ») : il ne sert jamais aux lectures ordinaires (supprime_le IS NULL).
-- Non repris : reference_values (lue par list_code, déjà couverte par uk_reference_value_scope) et les tables déjà
-- couvertes par une clé primaire ou un index complet commençant par organization_id.
--
-- Deuxième colonne : le filtre principal des requêtes du code (findByStudentId, findByRecipientId…), seulement quand
-- il est net ; sinon organization_id seul.
--
-- Compatible base neuve ET base ancienne (créée par Hibernate puis marquée V1 par Flyway) : une table ou une colonne
-- absente est ignorée (avis dans le journal de migration) ; si la deuxième colonne manque, l'index se limite à
-- organization_id ; un index existant qui commence déjà par les mêmes colonnes (hors index partiel) suffit.
-- CREATE INDEX simple (pas CONCURRENTLY) : impossible dans un bloc DO, et les tables sont encore petites.

DO $$
DECLARE
    r record;
    voulues text[];
    retenues text[];
    table_cible regclass;
    deja boolean;
BEGIN
    FOR r IN SELECT * FROM (VALUES
        -- 47 tables sans aucun index commençant par organization_id
        ('activity_logs',               'organization_id,activity_time'),
        ('admission_settings',          'organization_id'),
        ('admissions_portail_elements', 'organization_id'),
        ('affectations_portail',        'organization_id'),
        ('attendances',                 'organization_id,student_id'),
        ('audit_logs',                  'organization_id,action_timestamp'),
        ('cahier_de_textes',            'organization_id,course_id'),
        ('candidates',                  'organization_id'),
        ('course_details',              'organization_id'),
        ('courses',                     'organization_id'),
        ('dashboard_alerts',            'organization_id'),
        ('dashboard_kpis',              'organization_id'),
        ('demandes_report',             'organization_id'),
        ('documents',                   'organization_id,group_id'),
        ('documents_eleves',            'organization_id,user_id'),
        ('expenses',                    'organization_id'),
        ('grades',                      'organization_id,student_id'),
        ('group_chats',                 'organization_id'),
        ('inscriptions_classes',        'organization_id,classe_id'),
        ('journal_notes_evaluations',   'organization_id,evaluation_id'),
        ('justificatifs_absence',       'organization_id,eleve_id'),
        ('live_chat_messages',          'organization_id,session_id'),
        ('notification_templates',      'organization_id'),
        ('notifications',               'organization_id,recipient_id'),
        ('organization_users',          'organization_id,user_id'),
        ('parametres_candidatures',     'organization_id'),
        ('parent_appointments',         'organization_id,parent_id'),
        ('parent_enfant',               'organization_id,parent_id'),
        ('parents',                     'organization_id,user_id'),
        ('pointages_personnel',         'organization_id,user_id'),
        ('profils_eleves',              'organization_id,user_id'),
        ('quiz_tentatives',             'organization_id,student_id'),
        ('regional_stats',              'organization_id'),
        ('roles',                       'organization_id'),
        ('schedule_sessions',           'organization_id'),
        ('security_events',             'organization_id,event_timestamp'),
        ('statuts_frais_eleves',        'organization_id'),
        ('student_courses',             'organization_id,student_id'),
        ('student_data',                'organization_id'),
        ('student_finances',            'organization_id,student_id'),
        ('student_grades',              'organization_id,student_id'),
        ('student_homeworks',           'organization_id,student_id'),
        ('subscriptions',               'organization_id'),
        ('tasks',                       'organization_id,group_id'),
        ('transactions',                'organization_id,student_id'),
        ('vigil_profiles',              'organization_id'),
        ('workgroups',                  'organization_id'),
        -- 13 tables dont le seul index sur organization_id est l'index partiel de la corbeille (V21)
        ('classes',                     'organization_id'),
        ('creneaux_planning',           'organization_id'),
        ('evaluations_cours',           'organization_id'),
        ('filieres',                    'organization_id'),
        ('homeworks',                   'organization_id'),
        ('lessons',                     'organization_id'),
        ('meets',                       'organization_id'),
        ('messages',                    'organization_id'),
        ('modules',                     'organization_id'),
        ('quizzes',                     'organization_id'),
        ('reminders',                   'organization_id'),
        ('rooms',                       'organization_id'),
        ('slots',                       'organization_id')
    ) AS v(nom_table, colonnes)
    LOOP
        table_cible := to_regclass(format('public.%I', r.nom_table));
        IF table_cible IS NULL THEN
            RAISE NOTICE 'V23 : table % absente, index ignoré', r.nom_table;
            CONTINUE;
        END IF;

        -- colonnes présentes, dans l'ordre voulu ; on s'arrête à la première absente
        voulues := string_to_array(r.colonnes, ',');
        retenues := ARRAY[]::text[];
        FOR i IN 1 .. array_length(voulues, 1) LOOP
            EXIT WHEN NOT EXISTS (SELECT 1 FROM pg_attribute a
                                  WHERE a.attrelid = table_cible AND a.attname = voulues[i]
                                    AND a.attnum > 0 AND NOT a.attisdropped);
            retenues := retenues || voulues[i];
        END LOOP;
        IF array_length(retenues, 1) IS NULL THEN
            RAISE NOTICE 'V23 : colonne organization_id absente de %, index ignoré', r.nom_table;
            CONTINUE;
        END IF;

        -- un index complet (non partiel, sans expression) qui commence déjà par ces colonnes suffit
        SELECT EXISTS (
            SELECT 1 FROM pg_index ix
            WHERE ix.indrelid = table_cible AND ix.indpred IS NULL AND ix.indexprs IS NULL
              AND ix.indnkeyatts >= array_length(retenues, 1)
              AND (SELECT array_agg(a.attname::text ORDER BY k.rang)
                   FROM unnest(ix.indkey::int2[]) WITH ORDINALITY AS k(numero, rang)
                   JOIN pg_attribute a ON a.attrelid = ix.indrelid AND a.attnum = k.numero
                   WHERE k.rang <= array_length(retenues, 1)) = retenues
        ) INTO deja;
        IF deja THEN
            CONTINUE;
        END IF;

        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON public.%I (%s)',
                       'idx_' || r.nom_table || '_ecole', r.nom_table,
                       (SELECT string_agg(quote_ident(c), ', ') FROM unnest(retenues) AS c));
    END LOOP;
END $$;

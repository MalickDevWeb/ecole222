-- =============================================================================
-- V1 — Schéma complet consolidé (APP-ARC-03), généré le 2026-11-11.
-- Source : structure créée par Hibernate à partir des 73 entités du code (backend 99d0e53),
-- exportée par pg_dump --schema-only depuis une base de QA neuve. Aucune donnée.
-- Remplace les anciens scripts V1 à V19 (conservés dans db/migration-historique/), qui ne
-- correspondaient plus au code. Les bases existantes sont « baselinées » à cette version.
-- =============================================================================



CREATE TABLE public.activity_logs (
    id character varying(255) NOT NULL,
    activity_time timestamp(6) without time zone,
    activity_type character varying(255),
    additional_info character varying(255),
    created_at timestamp(6) without time zone,
    description character varying(255),
    entity_id character varying(255),
    entity_type character varying(255),
    organization_id character varying(255) NOT NULL,
    user_id character varying(255),
    user_name character varying(255),
    user_role character varying(255)
);

CREATE TABLE public.admission_settings (
    id character varying(255) NOT NULL,
    academic_year character varying(255),
    additional_settings character varying(255),
    announcement_message character varying(255),
    closing_date timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    current_candidates_count integer,
    interview_required boolean,
    is_open boolean,
    max_candidates integer,
    min_academic_score double precision,
    opening_date timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    passing_score double precision,
    registration_fee_amount bigint,
    required_documents character varying(255),
    show_register_button boolean,
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.admissions_portail_elements (
    id character varying(255) NOT NULL,
    contenu_json text,
    nature character varying(20) NOT NULL,
    organization_id character varying(255) NOT NULL,
    "position" integer NOT NULL,
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.affectations_portail (
    id character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    portail character varying(255),
    updated_at timestamp(6) without time zone,
    vigile character varying(255)
);

CREATE TABLE public.attendances (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    date character varying(255),
    note character varying(500),
    organization_id character varying(255) NOT NULL,
    recorded_at timestamp(6) without time zone,
    recorded_by character varying(255),
    session_id character varying(255),
    status character varying(255),
    student_id character varying(255),
    student_name character varying(255)
);

CREATE TABLE public.audit_logs (
    id character varying(255) NOT NULL,
    action_description character varying(255),
    action_timestamp timestamp(6) without time zone,
    action_type character varying(255),
    actor_id character varying(255),
    actor_name character varying(255),
    actor_role character varying(255),
    additional_context character varying(255),
    created_at timestamp(6) without time zone,
    entity_id character varying(255),
    entity_type character varying(255),
    failure_reason character varying(255),
    ip_address character varying(255),
    module character varying(255),
    new_value character varying(255),
    old_value character varying(255),
    organization_id character varying(255) NOT NULL,
    severity character varying(255),
    status character varying(255),
    user_agent character varying(255)
);

CREATE TABLE public.cahier_de_textes (
    id character varying(255) NOT NULL,
    auteur_id character varying(255),
    contenu character varying(4000) NOT NULL,
    course_id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    date_seance character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    travail_a_faire character varying(4000)
);

CREATE TABLE public.candidates (
    id character varying(255) NOT NULL,
    academic_score double precision,
    additional_info character varying(255),
    admission_status character varying(255),
    course_choice character varying(255),
    created_at timestamp(6) without time zone,
    current_step character varying(255),
    diploma_uploaded boolean,
    email character varying(255),
    first_name character varying(255),
    full_name character varying(255),
    id_card_uploaded boolean,
    interview_date timestamp(6) without time zone,
    interview_score double precision,
    last_name character varying(255),
    motivation_letter_uploaded boolean,
    organization_id character varying(255) NOT NULL,
    phone character varying(255),
    registration_fee_amount bigint,
    registration_fee_paid boolean,
    registration_fee_reference character varying(255),
    rejection_reason character varying(255),
    submitted_at timestamp(6) without time zone,
    total_score double precision,
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.classes (
    id character varying(255) NOT NULL,
    capacity integer NOT NULL,
    code character varying(255),
    current_size integer NOT NULL,
    filiere_id character varying(255),
    frais_annuels bigint,
    level character varying(255),
    name character varying(255),
    niveau character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL
);

CREATE TABLE public.course_details (
    course_id character varying(255) NOT NULL,
    data jsonb,
    organization_id character varying(255) NOT NULL
);

CREATE TABLE public.courses (
    id character varying(255) NOT NULL,
    classe character varying(255),
    code character varying(255),
    description character varying(255),
    name character varying(255),
    organization_id character varying(255) NOT NULL,
    prof_id character varying(255),
    total_hours integer NOT NULL
);

CREATE TABLE public.creneaux_planning (
    id character varying(255) NOT NULL,
    classe_id character varying(255),
    description character varying(1000),
    heure_debut character varying(255),
    heure_fin character varying(255),
    jour character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL,
    professeur character varying(255),
    salle character varying(255),
    type character varying(255)
);

CREATE TABLE public.dashboard_alerts (
    id character varying(255) NOT NULL,
    additional_data character varying(255),
    alert_type character varying(255),
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone,
    is_active boolean,
    message character varying(255),
    organization_id character varying(255) NOT NULL,
    severity character varying(255),
    title character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.dashboard_kpis (
    id character varying(255) NOT NULL,
    category character varying(255),
    created_at timestamp(6) without time zone,
    currency character varying(255),
    description character varying(255),
    kpi_key character varying(255),
    kpi_name character varying(255),
    last_updated timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    trend character varying(255),
    trend_value character varying(255),
    value character varying(255),
    value_type character varying(255)
);

CREATE TABLE public.demandes_report (
    id character varying(255) NOT NULL,
    classe character varying(255),
    course_id character varying(255),
    course_title character varying(255),
    created_at timestamp(6) without time zone,
    decide_le timestamp(6) without time zone,
    decide_par character varying(255),
    horaire character varying(255) NOT NULL,
    horaire_actuel character varying(255),
    jour character varying(255) NOT NULL,
    jour_actuel character varying(255),
    motif character varying(1000),
    motif_refus character varying(1000),
    organization_id character varying(255) NOT NULL,
    professor_id character varying(255) NOT NULL,
    salle character varying(255) NOT NULL,
    salle_actuelle character varying(255),
    session_id character varying(255) NOT NULL,
    statut character varying(255) NOT NULL
);

CREATE TABLE public.documents (
    id character varying(255) NOT NULL,
    added_by character varying(255),
    group_id character varying(255),
    name character varying(255),
    organization_id character varying(255) NOT NULL,
    size character varying(255),
    type character varying(255),
    url character varying(255)
);

CREATE TABLE public.documents_eleves (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    name character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    size character varying(255),
    url character varying(2000),
    user_id character varying(255) NOT NULL
);

CREATE TABLE public.entitlements (
    id character varying(255) NOT NULL,
    feature_code character varying(255) NOT NULL,
    is_enabled boolean,
    max_limit integer,
    plan_id character varying(255) NOT NULL
);

CREATE TABLE public.evaluations_cours (
    id character varying(255) NOT NULL,
    coefficient double precision NOT NULL,
    course_id character varying(255) NOT NULL,
    cree_le timestamp(6) without time zone,
    cree_par character varying(255),
    date_evaluation date NOT NULL,
    evaluation_rattrapee_id character varying(255),
    note_sur double precision NOT NULL,
    organization_id character varying(255) NOT NULL,
    session character varying(20) NOT NULL,
    titre character varying(150) NOT NULL,
    type character varying(30) NOT NULL,
    verrouillee boolean NOT NULL,
    verrouillee_le timestamp(6) without time zone,
    verrouillee_par character varying(255)
);

CREATE TABLE public.expenses (
    id character varying(255) NOT NULL,
    amount bigint,
    category character varying(255),
    created_at timestamp(6) without time zone,
    date timestamp(6) without time zone,
    description character varying(255),
    label character varying(255),
    organization_id character varying(255) NOT NULL,
    status character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.filieres (
    id character varying(255) NOT NULL,
    code character varying(255),
    description character varying(255),
    level character varying(255),
    name character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL
);

CREATE TABLE public.grades (
    id character varying(255) NOT NULL,
    average double precision,
    cc double precision,
    course_id character varying(255),
    examen double precision,
    organization_id character varying(255) NOT NULL,
    student_id character varying(255)
);

CREATE TABLE public.group_chat_admins (
    chat_id character varying(36) NOT NULL,
    user_id character varying(36)
);

CREATE TABLE public.group_chat_participants (
    chat_id character varying(36) NOT NULL,
    user_id character varying(36)
);

CREATE TABLE public.group_chats (
    id character varying(36) NOT NULL,
    archived boolean,
    avatar_url character varying(500),
    chat_type character varying(20) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    creator_id character varying(36) NOT NULL,
    description text,
    last_message_time timestamp(6) without time zone,
    max_participants integer,
    muted boolean,
    muted_until timestamp(6) without time zone,
    name character varying(255),
    organization_id character varying(255) NOT NULL,
    pinned_message_id character varying(36)
);

CREATE TABLE public.homeworks (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    deadline character varying(255),
    description character varying(255),
    organization_id character varying(255) NOT NULL,
    priority character varying(255),
    title character varying(255)
);

CREATE TABLE public.inscriptions_classes (
    id character varying(255) NOT NULL,
    classe_id character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id character varying(255) NOT NULL
);

CREATE TABLE public.journal_notes_evaluations (
    id character varying(255) NOT NULL,
    action character varying(20) NOT NULL,
    ancien_commentaire character varying(500),
    ancienne_valeur character varying(20),
    auteur_id character varying(255),
    auteur_nom character varying(255),
    course_id character varying(255) NOT NULL,
    evaluation_id character varying(255) NOT NULL,
    horodatage timestamp(6) without time zone NOT NULL,
    nouveau_commentaire character varying(500),
    nouvelle_valeur character varying(20),
    organization_id character varying(255) NOT NULL,
    student_id character varying(255)
);

CREATE TABLE public.justificatifs_absence (
    id character varying(255) NOT NULL,
    depose_le timestamp(6) without time zone NOT NULL,
    eleve_id character varying(255) NOT NULL,
    fichier_url character varying(1000),
    motif character varying(500) NOT NULL,
    motif_refus character varying(500),
    organization_id character varying(255) NOT NULL,
    parent_id character varying(255) NOT NULL,
    presence_id character varying(255) NOT NULL,
    statut character varying(20) NOT NULL,
    statut_initial character varying(20),
    traite_le timestamp(6) without time zone,
    traite_par character varying(255)
);

CREATE TABLE public.lessons (
    id character varying(255) NOT NULL,
    attachment_name character varying(255),
    attachment_url character varying(2000),
    course_id character varying(255),
    date character varying(255),
    description character varying(4000),
    duration integer NOT NULL,
    module_id character varying(255),
    organization_id character varying(255) NOT NULL,
    title character varying(255)
);

CREATE TABLE public.live_chat_messages (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    session_id character varying(255),
    text character varying(255),
    "timestamp" timestamp(6) without time zone,
    user_id character varying(255),
    user_name character varying(255),
    user_role character varying(255)
);

CREATE TABLE public.meets (
    id character varying(255) NOT NULL,
    active boolean,
    class_ids character varying(2000),
    class_names character varying(2000),
    link character varying(255),
    meet_date character varying(255),
    organization_id character varying(255) NOT NULL,
    published boolean,
    restrict_google boolean,
    subject_name character varying(255),
    teacher_name character varying(255),
    "time" character varying(255),
    title character varying(255)
);

CREATE TABLE public.messages (
    id character varying(255) NOT NULL,
    content character varying(255),
    group_id character varying(255),
    organization_id character varying(255) NOT NULL,
    sender_id character varying(255),
    "timestamp" timestamp(6) without time zone,
    deleted_for_everyone boolean,
    duration bigint,
    forwarded_from character varying(36),
    media_url character varying(500),
    reply_to_id character varying(36),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.modules (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    description character varying(2000),
    name character varying(255),
    organization_id character varying(255) NOT NULL
);

CREATE TABLE public.notes_evaluations (
    id character varying(255) NOT NULL,
    absence character varying(10),
    commentaire character varying(500),
    course_id character varying(255) NOT NULL,
    evaluation_id character varying(255) NOT NULL,
    modifie_le timestamp(6) without time zone,
    modifie_par character varying(255),
    organization_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    valeur double precision
);

CREATE TABLE public.notification_templates (
    id character varying(255) NOT NULL,
    action_required boolean,
    body_template character varying(255),
    category character varying(255),
    created_at timestamp(6) without time zone,
    default_action_link character varying(255),
    default_action_type character varying(255),
    default_priority character varying(255),
    description character varying(255),
    is_active boolean,
    organization_id character varying(255) NOT NULL,
    subject character varying(255),
    supported_variables character varying(255),
    template_key character varying(255),
    template_name character varying(255),
    template_type character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.notifications (
    id character varying(255) NOT NULL,
    action_link character varying(255),
    action_required boolean,
    action_type character varying(255),
    additional_data character varying(255),
    category character varying(255),
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone,
    is_read boolean,
    message character varying(255),
    notification_type character varying(255),
    organization_id character varying(255) NOT NULL,
    priority character varying(255),
    read_at timestamp(6) without time zone,
    recipient_id character varying(255),
    recipient_role character varying(255),
    recipient_type character varying(255),
    sent_via_email boolean,
    sent_via_push boolean,
    sent_via_sms boolean,
    title character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.organization_branding (
    organization_id character varying(255) NOT NULL,
    accent_color character varying(255),
    custom_css text,
    favicon_url character varying(255),
    font_family character varying(255),
    logo_url character varying(255),
    primary_color character varying(255),
    secondary_color character varying(255)
);

CREATE TABLE public.organization_features (
    organization_id character varying(255) NOT NULL,
    enable_live_classes boolean,
    enable_online_payment boolean,
    enable_parent_portal boolean,
    enable_sms_notifications boolean
);

CREATE TABLE public.organization_settings (
    organization_id character varying(255) NOT NULL,
    academic_year_end date,
    academic_year_start date,
    date_format character varying(255),
    language character varying(255),
    time_format character varying(255),
    timezone character varying(255)
);

CREATE TABLE public.organization_users (
    id character varying(255) NOT NULL,
    delegation_id character varying(255),
    expires_at timestamp(6) without time zone,
    is_delegated boolean,
    joined_at timestamp(6) without time zone NOT NULL,
    organization_id character varying(255) NOT NULL,
    permissions character varying(255)[],
    role character varying(255) NOT NULL,
    role_id character varying(255),
    scope_id character varying(255),
    scope_type character varying(255),
    status character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL
);

CREATE TABLE public.organizations (
    id character varying(255) NOT NULL,
    address text,
    city character varying(255),
    country character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255),
    logo character varying(255),
    name character varying(255) NOT NULL,
    phone character varying(255),
    primary_domain_id character varying(255),
    settings jsonb,
    slug character varying(255),
    status character varying(255) NOT NULL,
    subscription_id character varying(255),
    suspended_at timestamp(6) without time zone,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    validated_at timestamp(6) without time zone,
    website character varying(255),
    CONSTRAINT organizations_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT organizations_type_check CHECK (((type)::text = ANY ((ARRAY['ECOLE'::character varying, 'UNIVERSITE'::character varying, 'INSTITUT'::character varying, 'CENTRE_FORMATION'::character varying, 'LYCEE'::character varying, 'COLLEGE'::character varying])::text[])))
);

CREATE TABLE public.parametres_candidatures (
    id character varying(255) NOT NULL,
    date_fermeture character varying(255),
    date_ouverture character varying(255),
    docs_requis character varying(2000),
    message_avis character varying(2000),
    organization_id character varying(255) NOT NULL,
    ouvert boolean NOT NULL,
    show_register_button boolean,
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.parent_appointments (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    date_souhaitee character varying(255),
    meet_url character varying(255),
    motif character varying(1000),
    organization_id character varying(255) NOT NULL,
    parent_id character varying(255) NOT NULL,
    professeur_id character varying(255),
    professeur_nom character varying(255),
    status character varying(50)
);

CREATE TABLE public.parent_enfant (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    enfant_id character varying(255),
    organization_id character varying(255) NOT NULL,
    parent_id character varying(255),
    relation character varying(255)
);

CREATE TABLE public.parents (
    id character varying(255) NOT NULL,
    adresse character varying(255),
    created_at timestamp(6) without time zone,
    email character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL,
    prenom character varying(255),
    profession character varying(255),
    telephone character varying(255),
    updated_at timestamp(6) without time zone,
    user_id character varying(255)
);

CREATE TABLE public.permissions (
    id character varying(255) NOT NULL,
    action character varying(255) NOT NULL,
    category character varying(255) NOT NULL,
    description text,
    name character varying(255) NOT NULL,
    resource character varying(255) NOT NULL,
    supports_scope boolean
);

CREATE TABLE public.plans (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    is_active boolean,
    name character varying(255) NOT NULL,
    price_monthly numeric(38,2) NOT NULL,
    price_yearly numeric(38,2) NOT NULL
);

CREATE TABLE public.pointages_personnel (
    id character varying(255) NOT NULL,
    check_in character varying(255),
    check_out character varying(255),
    created_at timestamp(6) without time zone,
    hours double precision,
    name character varying(255),
    note character varying(1000),
    organization_id character varying(255) NOT NULL,
    role character varying(255),
    status character varying(255),
    user_id character varying(255)
);

CREATE TABLE public.profils_eleves (
    id character varying(255) NOT NULL,
    allergies character varying(1000),
    groupe_sanguin character varying(255),
    organization_id character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id character varying(255) NOT NULL
);

CREATE TABLE public.quiz_tentatives (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    date_tentative timestamp(6) without time zone NOT NULL,
    organization_id character varying(255) NOT NULL,
    quiz_id character varying(255) NOT NULL,
    reponses_json text NOT NULL,
    score integer NOT NULL,
    student_id character varying(255) NOT NULL,
    total integer NOT NULL
);

CREATE TABLE public.quizzes (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    description character varying(2000),
    duration integer NOT NULL,
    max_tentatives integer,
    module_id character varying(255),
    organization_id character varying(255) NOT NULL,
    questions_json text,
    title character varying(255)
);

CREATE TABLE public.reference_values (
    id character varying(255) NOT NULL,
    active boolean NOT NULL,
    code character varying(40) NOT NULL,
    color character varying(20),
    label character varying(80) NOT NULL,
    list_code character varying(40) NOT NULL,
    organization_id character varying(255),
    sort_order integer NOT NULL,
    system_value boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE TABLE public.regional_stats (
    id character varying(255) NOT NULL,
    academic_year character varying(255),
    color_code character varying(255),
    created_at timestamp(6) without time zone,
    last_updated timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    percentage double precision,
    region_name character varying(255),
    student_count integer
);

CREATE TABLE public.reminders (
    id character varying(255) NOT NULL,
    content character varying(255),
    course_id character varying(255),
    created_at timestamp(6) without time zone,
    due_date timestamp(6) without time zone,
    is_completed boolean,
    is_urgent boolean,
    organization_id character varying(255) NOT NULL,
    professor_id character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.rendus_devoirs (
    id character varying(255) NOT NULL,
    commentaire character varying(2000),
    corrige_le timestamp(6) without time zone,
    corrige_par character varying(255),
    course_id character varying(255) NOT NULL,
    depose_le timestamp(6) without time zone,
    fichier_nom character varying(255),
    fichier_taille bigint,
    fichier_type character varying(150),
    fichier_url character varying(1000),
    homework_id character varying(255) NOT NULL,
    note double precision,
    organization_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL
);

CREATE TABLE public.role_permissions (
    id character varying(255) NOT NULL,
    conditions jsonb,
    permission_id character varying(255) NOT NULL,
    role_id character varying(255) NOT NULL
);

CREATE TABLE public.roles (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    is_system_role boolean,
    name character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    permissions character varying(255)[]
);

CREATE TABLE public.rooms (
    id character varying(255) NOT NULL,
    capacite integer,
    capacity integer NOT NULL,
    equipment character varying(255),
    location character varying(255),
    name character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL
);

CREATE TABLE public.schedule_sessions (
    id character varying(255) NOT NULL,
    cancellation_reason character varying(255),
    classe character varying(255),
    course_id character varying(255),
    course_title character varying(255),
    created_at timestamp(6) without time zone,
    date_str character varying(255),
    day character varying(255),
    organization_id character varying(255) NOT NULL,
    original_date timestamp(6) without time zone,
    professor_id character varying(255),
    rescheduled_to timestamp(6) without time zone,
    room character varying(255),
    status character varying(255),
    "time" character varying(255),
    type character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.security_events (
    id character varying(255) NOT NULL,
    access_status character varying(255),
    additional_data character varying(255),
    badge_id character varying(255),
    camera_recording boolean,
    camera_reference character varying(255),
    checkpoint_id character varying(255),
    checkpoint_name character varying(255),
    created_at timestamp(6) without time zone,
    denial_reason character varying(255),
    event_category character varying(255),
    event_description character varying(255),
    event_timestamp timestamp(6) without time zone,
    event_type character varying(255),
    investigation_status character varying(255),
    location character varying(255),
    organization_id character varying(255) NOT NULL,
    person_id character varying(255),
    person_name character varying(255),
    person_type character varying(255),
    requires_investigation boolean,
    resolution_notes character varying(255),
    resolved_by character varying(255),
    severity character varying(255)
);

CREATE TABLE public.slots (
    id character varying(255) NOT NULL,
    day_of_week character varying(255),
    end_time character varying(255),
    organization_id character varying(255) NOT NULL,
    start_time character varying(255),
    type character varying(255)
);

CREATE TABLE public.statuts_frais_eleves (
    user_id character varying(255) NOT NULL,
    organization_id character varying(255) NOT NULL,
    statut character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE TABLE public.student_courses (
    id character varying(255) NOT NULL,
    coefficient integer,
    course_id character varying(255),
    created_at timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    progress integer,
    semester character varying(255),
    statut character varying(255),
    student_id character varying(255),
    titre character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.student_data (
    student_id character varying(255) NOT NULL,
    courses jsonb,
    dashboard jsonb,
    grades jsonb,
    organization_id character varying(255) NOT NULL,
    profile jsonb
);

CREATE TABLE public.student_finances (
    id character varying(255) NOT NULL,
    classe character varying(255),
    created_at timestamp(6) without time zone,
    debt bigint,
    last_payment_date timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    paid bigint,
    status character varying(255),
    student_id character varying(255),
    student_name character varying(255),
    total_required bigint,
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.student_grades (
    id character varying(255) NOT NULL,
    cc_grade double precision,
    created_at timestamp(6) without time zone,
    ects integer,
    exam_grade double precision,
    module character varying(255),
    organization_id character varying(255) NOT NULL,
    professor character varying(255),
    promo_average double precision,
    semester character varying(255),
    student_id character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.student_homeworks (
    id character varying(255) NOT NULL,
    course_id character varying(255),
    created_at timestamp(6) without time zone,
    deadline timestamp(6) without time zone,
    file_name character varying(255),
    organization_id character varying(255) NOT NULL,
    priority character varying(255),
    progress integer,
    statut character varying(255),
    student_id character varying(255),
    submitted_at timestamp(6) without time zone,
    titre character varying(255),
    updated_at timestamp(6) without time zone
);

CREATE TABLE public.subscriptions (
    id character varying(255) NOT NULL,
    end_date timestamp(6) without time zone,
    next_billing_date timestamp(6) without time zone,
    organization_id character varying(255) NOT NULL,
    plan_id character varying(255) NOT NULL,
    start_date timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL
);

CREATE TABLE public.tasks (
    id character varying(255) NOT NULL,
    assigned_to character varying(255),
    group_id character varying(255),
    organization_id character varying(255) NOT NULL,
    priority character varying(255),
    status character varying(255),
    title character varying(255)
);

CREATE TABLE public.transactions (
    id character varying(255) NOT NULL,
    amount bigint,
    created_at timestamp(6) without time zone,
    description character varying(255),
    organization_id character varying(255) NOT NULL,
    payment_method character varying(255),
    reference character varying(255),
    student_finance_id character varying(255),
    student_id character varying(255),
    student_name character varying(255),
    transaction_date timestamp(6) without time zone,
    transaction_type character varying(255)
);

CREATE TABLE public.users (
    id character varying(255) NOT NULL,
    doit_changer_mot_de_passe boolean,
    email character varying(255),
    email_verified boolean,
    language character varying(255),
    last_login_at timestamp(6) without time zone,
    matricule character varying(255),
    nom character varying(255),
    password character varying(255),
    phone_verified boolean,
    photo_url character varying(255),
    prenom character varying(255),
    role character varying(255),
    telephone character varying(255),
    timezone character varying(255)
);

CREATE TABLE public.vigil_profiles (
    id character varying(255) NOT NULL,
    badge_id character varying(255),
    created_at timestamp(6) without time zone,
    derniere_connexion timestamp(6) without time zone,
    equipe character varying(255),
    nom character varying(255),
    organization_id character varying(255) NOT NULL,
    prenom character varying(255),
    role character varying(255),
    statut character varying(255),
    updated_at timestamp(6) without time zone,
    zone character varying(255)
);

CREATE TABLE public.workgroups (
    id character varying(255) NOT NULL,
    creator_id character varying(255),
    description character varying(255),
    members_count integer NOT NULL,
    name character varying(255),
    organization_id character varying(255) NOT NULL
);

ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.admission_settings
    ADD CONSTRAINT admission_settings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.admissions_portail_elements
    ADD CONSTRAINT admissions_portail_elements_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.affectations_portail
    ADD CONSTRAINT affectations_portail_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.attendances
    ADD CONSTRAINT attendances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cahier_de_textes
    ADD CONSTRAINT cahier_de_textes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.candidates
    ADD CONSTRAINT candidates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.course_details
    ADD CONSTRAINT course_details_pkey PRIMARY KEY (course_id);

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.creneaux_planning
    ADD CONSTRAINT creneaux_planning_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dashboard_alerts
    ADD CONSTRAINT dashboard_alerts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dashboard_kpis
    ADD CONSTRAINT dashboard_kpis_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.demandes_report
    ADD CONSTRAINT demandes_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.documents_eleves
    ADD CONSTRAINT documents_eleves_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entitlements
    ADD CONSTRAINT entitlements_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.evaluations_cours
    ADD CONSTRAINT evaluations_cours_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.filieres
    ADD CONSTRAINT filieres_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.grades
    ADD CONSTRAINT grades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.group_chats
    ADD CONSTRAINT group_chats_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.homeworks
    ADD CONSTRAINT homeworks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inscriptions_classes
    ADD CONSTRAINT inscriptions_classes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.journal_notes_evaluations
    ADD CONSTRAINT journal_notes_evaluations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.justificatifs_absence
    ADD CONSTRAINT justificatifs_absence_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.live_chat_messages
    ADD CONSTRAINT live_chat_messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.meets
    ADD CONSTRAINT meets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notes_evaluations
    ADD CONSTRAINT notes_evaluations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organization_branding
    ADD CONSTRAINT organization_branding_pkey PRIMARY KEY (organization_id);

ALTER TABLE ONLY public.organization_features
    ADD CONSTRAINT organization_features_pkey PRIMARY KEY (organization_id);

ALTER TABLE ONLY public.organization_settings
    ADD CONSTRAINT organization_settings_pkey PRIMARY KEY (organization_id);

ALTER TABLE ONLY public.organization_users
    ADD CONSTRAINT organization_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parametres_candidatures
    ADD CONSTRAINT parametres_candidatures_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parent_appointments
    ADD CONSTRAINT parent_appointments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parent_enfant
    ADD CONSTRAINT parent_enfant_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT parents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT plans_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pointages_personnel
    ADD CONSTRAINT pointages_personnel_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.profils_eleves
    ADD CONSTRAINT profils_eleves_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quiz_tentatives
    ADD CONSTRAINT quiz_tentatives_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT quizzes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_values
    ADD CONSTRAINT reference_values_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.regional_stats
    ADD CONSTRAINT regional_stats_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reminders
    ADD CONSTRAINT reminders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.rendus_devoirs
    ADD CONSTRAINT rendus_devoirs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.schedule_sessions
    ADD CONSTRAINT schedule_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.slots
    ADD CONSTRAINT slots_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.statuts_frais_eleves
    ADD CONSTRAINT statuts_frais_eleves_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.student_courses
    ADD CONSTRAINT student_courses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.student_data
    ADD CONSTRAINT student_data_pkey PRIMARY KEY (student_id);

ALTER TABLE ONLY public.student_finances
    ADD CONSTRAINT student_finances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.student_grades
    ADD CONSTRAINT student_grades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.student_homeworks
    ADD CONSTRAINT student_homeworks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk2p58gbqhxvue2igoderm0gh2c UNIQUE (telephone);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.notes_evaluations
    ADD CONSTRAINT uk_note_evaluation_eleve UNIQUE (organization_id, evaluation_id, student_id);

ALTER TABLE ONLY public.reference_values
    ADD CONSTRAINT uk_reference_value_scope UNIQUE (list_code, code, organization_id);

ALTER TABLE ONLY public.rendus_devoirs
    ADD CONSTRAINT uk_rendu_devoir_eleve UNIQUE (organization_id, homework_id, student_id);

ALTER TABLE ONLY public.group_chat_admins
    ADD CONSTRAINT ukcjyl2e1uud5vubb0tco94i3l6 UNIQUE (chat_id, user_id);

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT ukisg8o11v7k7i1vko77g2bv4mu UNIQUE (email);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT ukpnvtwliis6p05pn6i3ndjrqt2 UNIQUE (name);

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT uksfr9257mbjkowos3ci3e22ay2 UNIQUE (slug);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.vigil_profiles
    ADD CONSTRAINT vigil_profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.workgroups
    ADD CONSTRAINT workgroups_pkey PRIMARY KEY (id);

CREATE INDEX idx_admins_user_id ON public.group_chat_admins USING btree (user_id);

CREATE INDEX idx_evaluations_cours_cours ON public.evaluations_cours USING btree (course_id);

CREATE INDEX idx_group_chats_archived ON public.group_chats USING btree (archived);

CREATE INDEX idx_group_chats_created ON public.group_chats USING btree (created_at);

CREATE INDEX idx_group_chats_creator ON public.group_chats USING btree (creator_id);

CREATE INDEX idx_group_chats_last_message ON public.group_chats USING btree (last_message_time);

CREATE INDEX idx_group_chats_type ON public.group_chats USING btree (chat_type);

CREATE INDEX idx_journal_notes_eval ON public.journal_notes_evaluations USING btree (evaluation_id);

CREATE INDEX idx_justif_eleve ON public.justificatifs_absence USING btree (eleve_id);

CREATE INDEX idx_justif_presence ON public.justificatifs_absence USING btree (presence_id);

CREATE INDEX idx_messages_group_id ON public.messages USING btree (group_id);

CREATE INDEX idx_messages_group_timestamp ON public.messages USING btree (group_id, "timestamp");

CREATE INDEX idx_messages_sender_id ON public.messages USING btree (sender_id);

CREATE INDEX idx_messages_sender_timestamp ON public.messages USING btree (sender_id, "timestamp");

CREATE INDEX idx_messages_timestamp ON public.messages USING btree ("timestamp");

CREATE INDEX idx_notes_evaluations_cours ON public.notes_evaluations USING btree (course_id);

CREATE INDEX idx_notes_evaluations_eval ON public.notes_evaluations USING btree (evaluation_id);

CREATE INDEX idx_participants_user_id ON public.group_chat_participants USING btree (user_id);

CREATE INDEX idx_quiz_tentatives_quiz ON public.quiz_tentatives USING btree (quiz_id);

CREATE INDEX idx_quiz_tentatives_quiz_eleve ON public.quiz_tentatives USING btree (quiz_id, student_id);

CREATE INDEX idx_reference_values_list_org ON public.reference_values USING btree (list_code, organization_id);

CREATE INDEX idx_rendu_devoir ON public.rendus_devoirs USING btree (homework_id);

CREATE INDEX idx_rendu_eleve ON public.rendus_devoirs USING btree (student_id);

ALTER TABLE ONLY public.group_chat_admins
    ADD CONSTRAINT fk81x404eq2hfoo87j5uuqoxqo9 FOREIGN KEY (chat_id) REFERENCES public.group_chats(id);

ALTER TABLE ONLY public.group_chat_participants
    ADD CONSTRAINT fkk7xylrxim0sirs5nn1e30njqd FOREIGN KEY (chat_id) REFERENCES public.group_chats(id);

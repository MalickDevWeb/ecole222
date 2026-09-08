package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.*;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DashboardKpiJpaRepository kpiRepository;
    private final DashboardAlertJpaRepository alertRepository;
    private final RegionalStatsJpaRepository regionalStatsRepository;
    private final StudentFinanceJpaRepository studentFinanceRepository;
    private final ExpenseJpaRepository expenseRepository;
    private final CandidateJpaRepository candidateRepository;
    private final AdmissionSettingsJpaRepository admissionSettingsRepository;
    private final NotificationTemplateJpaRepository notificationTemplateRepository;
    private final ActivityLogJpaRepository activityLogRepository;
    private final ReminderJpaRepository reminderRepository;
    private final ScheduleSessionJpaRepository scheduleRepository;
    private final StudentGradeJpaRepository studentGradeRepository;
    private final StudentCourseJpaRepository studentCourseRepository;
    private final StudentHomeworkJpaRepository studentHomeworkRepository;
    private final LiveChatMessageJpaRepository liveChatMessageRepository;
    private final VigilProfileJpaRepository vigilProfileRepository;
    private final UserJpaRepository userRepository;
    private final ParentJpaRepository parentJpaRepository;
    private final ParentEnfantJpaRepository parentEnfantJpaRepository;

    public DataInitializer(
            DashboardKpiJpaRepository kpiRepository,
            DashboardAlertJpaRepository alertRepository,
            RegionalStatsJpaRepository regionalStatsRepository,
            StudentFinanceJpaRepository studentFinanceRepository,
            ExpenseJpaRepository expenseRepository,
            CandidateJpaRepository candidateRepository,
            AdmissionSettingsJpaRepository admissionSettingsRepository,
            NotificationTemplateJpaRepository notificationTemplateRepository,
            ActivityLogJpaRepository activityLogRepository,
            ReminderJpaRepository reminderRepository,
            ScheduleSessionJpaRepository scheduleRepository,
            StudentGradeJpaRepository studentGradeRepository,
            StudentCourseJpaRepository studentCourseRepository,
            StudentHomeworkJpaRepository studentHomeworkRepository,
            LiveChatMessageJpaRepository liveChatMessageRepository,
            VigilProfileJpaRepository vigilProfileRepository,
            UserJpaRepository userRepository,
            ParentJpaRepository parentJpaRepository,
            ParentEnfantJpaRepository parentEnfantJpaRepository) {
        this.kpiRepository = kpiRepository;
        this.alertRepository = alertRepository;
        this.regionalStatsRepository = regionalStatsRepository;
        this.studentFinanceRepository = studentFinanceRepository;
        this.expenseRepository = expenseRepository;
        this.candidateRepository = candidateRepository;
        this.admissionSettingsRepository = admissionSettingsRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.activityLogRepository = activityLogRepository;
        this.reminderRepository = reminderRepository;
        this.scheduleRepository = scheduleRepository;
        this.studentGradeRepository = studentGradeRepository;
        this.studentCourseRepository = studentCourseRepository;
        this.studentHomeworkRepository = studentHomeworkRepository;
        this.liveChatMessageRepository = liveChatMessageRepository;
        this.vigilProfileRepository = vigilProfileRepository;
        this.userRepository = userRepository;
        this.parentJpaRepository = parentJpaRepository;
        this.parentEnfantJpaRepository = parentEnfantJpaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Only populate if tables are empty
        if (kpiRepository.count() == 0) {
            populateKPIs();
            populateAlerts();
            populateRegionalStats();
            populateStudentFinances();
            populateExpenses();
            populateCandidates();
            populateAdmissionSettings();
            populateNotificationTemplates();
            populateActivityLogs();
        }
        
        // Always populate reminders and schedule if empty (new tables)
        if (reminderRepository.count() == 0) {
            populateReminders();
        }
        if (scheduleRepository.count() == 0) {
            populateScheduleSessions();
        }
        if (studentGradeRepository.count() == 0) {
            populateStudentGrades();
        }
        if (studentCourseRepository.count() == 0) {
            populateStudentCourses();
        }
        if (studentHomeworkRepository.count() == 0) {
            populateStudentHomeworks();
        }
        if (liveChatMessageRepository.count() == 0) {
            populateLiveChatMessages();
        }
        if (vigilProfileRepository.count() == 0) {
            populateVigilProfiles();
        }
        if (userRepository.count() == 0) {
            populateTestUsers();
        }
    }

    private void populateKPIs() {
        kpiRepository.save(new DashboardKpiJpaEntity(
            "kpi-1", "recovery_rate", "Taux de recouvrement", "94.2", "percentage", 
            "up", "+2.4%", "FCFA", "finance", "Pourcentage des frais de scolarité collectés"
        ));
        kpiRepository.save(new DashboardKpiJpaEntity(
            "kpi-2", "cash_flow", "Cash Flow", "14850000", "currency", 
            "stable", "0%", "FCFA", "finance", "Solde disponible en temps réel"
        ));
        kpiRepository.save(new DashboardKpiJpaEntity(
            "kpi-3", "ebitda", "EBITDA Opérationnel", "64.8", "percentage", 
            "up", "+1.2%", "FCFA", "finance", "Marge opérationnelle"
        ));
        kpiRepository.save(new DashboardKpiJpaEntity(
            "kpi-4", "qr_suspended", "QR Suspendus", "12", "count", 
            "down", "-2", "", "security", "Nombre de QR codes suspendus"
        ));
        kpiRepository.save(new DashboardKpiJpaEntity(
            "kpi-5", "open_tickets", "Tickets ouverts", "3", "count", 
            "stable", "0", "", "operations", "Tickets support urgents"
        ));
    }

    private void populateAlerts() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime in6Hours = LocalDateTime.now().plusHours(6);
        
        alertRepository.save(new DashboardAlertJpaEntity(
            "alert-1", "weather", "Alerte Météo & Logistique", 
            "Forte pluie prévue. Bus scolaires pré-déployés. Retards estimés à +15%.", 
            "warning", true, "{\"temperature\": \"28°C\", \"condition\": \"orageux\"}", 
            tomorrow
        ));
        alertRepository.save(new DashboardAlertJpaEntity(
            "alert-2", "security", "Scan avec badge SUSPENDU", 
            "Tentative d'accès avec un badge suspendu détectée au portail principal.", 
            "urgent", true, "{\"checkpoint\": \"Portail Principal\"}", 
            in6Hours
        ));
    }

    private void populateRegionalStats() {
        regionalStatsRepository.save(new RegionalStatsJpaEntity(
            "reg-1", "Dakar Centre", 65.0, 98, "#B3181C", "2025-2026"
        ));
        regionalStatsRepository.save(new RegionalStatsJpaEntity(
            "reg-2", "Thiès & Mbour", 20.0, 30, "#F59E0B", "2025-2026"
        ));
        regionalStatsRepository.save(new RegionalStatsJpaEntity(
            "reg-3", "Saint-Louis / Nord", 15.0, 22, "#EF4444", "2025-2026"
        ));
    }

    private void populateStudentFinances() {
        studentFinanceRepository.save(new StudentFinanceJpaEntity(
            "fin-1", "usr-001", "Assane Diop", "L1 Génie Civil", 
            "En Règle", 0L, 450000L, 450000L
        ));
        studentFinanceRepository.save(new StudentFinanceJpaEntity(
            "fin-2", "usr-002", "Fatou Sow", "L1 Génie Civil", 
            "En Avance", 0L, 600000L, 450000L
        ));
        studentFinanceRepository.save(new StudentFinanceJpaEntity(
            "fin-3", "usr-003", "Amadou Diallo", "L2 Génie Civil", 
            "En Retard", 150000L, 300000L, 450000L
        ));
        studentFinanceRepository.save(new StudentFinanceJpaEntity(
            "fin-4", "usr-004", "Khadidiatou Ba", "Master 1 Spécialité IA", 
            "En Retard", 300000L, 900000L, 1200000L
        ));
        studentFinanceRepository.save(new StudentFinanceJpaEntity(
            "fin-5", "usr-005", "Cheikh Ndiaye", "L3 CM (Construction Métallique)", 
            "En Règle", 0L, 500000L, 500000L
        ));
    }

    private void populateExpenses() {
        expenseRepository.save(new ExpenseJpaEntity(
            "exp-1", "Salaires des professeurs vacataires", "RH", 
            4800000L, LocalDateTime.of(2026, 7, 1, 0, 0), 
            "Paie mensuelle professeurs vacataires", "Payé"
        ));
        expenseRepository.save(new ExpenseJpaEntity(
            "exp-2", "Facture d'électricité Senelec", "Loyer/Charges", 
            350000L, LocalDateTime.of(2026, 7, 4, 0, 0), 
            "Facture mensuelle électricité", "Payé"
        ));
        expenseRepository.save(new ExpenseJpaEntity(
            "exp-3", "Loyer des locaux (Annexe B)", "Loyer/Charges", 
            800000L, LocalDateTime.of(2026, 7, 5, 0, 0), 
            "Loyer mensuel annexe B", "Payé"
        ));
        expenseRepository.save(new ExpenseJpaEntity(
            "exp-4", "Achat de serveurs de calcul pour le Labo IA", "Matériel", 
            1500000L, LocalDateTime.of(2026, 7, 8, 0, 0), 
            "Serveurs pour laboratoire IA", "Payé"
        ));
    }

    private void populateCandidates() {
        CandidateJpaEntity cand1 = new CandidateJpaEntity(
            "cand-1", "Nafissatou", "Diallo", "nafissatou@example.com", 
            "221771234567", "L2 Génie Civil"
        );
        cand1.setFullName("Nafissatou Diallo");
        cand1.setCurrentStep("new");
        cand1.setAdmissionStatus("pending");
        cand1.setDiplomaUploaded(true);
        cand1.setIdCardUploaded(false);
        cand1.setRegistrationFeePaid(false);
        candidateRepository.save(cand1);
        
        CandidateJpaEntity cand2 = new CandidateJpaEntity(
            "cand-2", "Ibrahima", "Ba", "ibrahima@example.com", 
            "221772345678", "Master 1 Spécialité IA"
        );
        cand2.setFullName("Ibrahima Ba");
        cand2.setCurrentStep("docs");
        cand2.setAdmissionStatus("pending");
        cand2.setDiplomaUploaded(true);
        cand2.setIdCardUploaded(true);
        cand2.setRegistrationFeePaid(false);
        candidateRepository.save(cand2);
        
        CandidateJpaEntity cand3 = new CandidateJpaEntity(
            "cand-3", "Modou", "Diagne", "modou@example.com", 
            "221773456789", "L3 CM (Construction Métallique)"
        );
        cand3.setFullName("Modou Diagne");
        cand3.setCurrentStep("admitted");
        cand3.setAdmissionStatus("accepted");
        cand3.setDiplomaUploaded(true);
        cand3.setIdCardUploaded(true);
        cand3.setRegistrationFeePaid(true);
        candidateRepository.save(cand3);
        
        CandidateJpaEntity cand4 = new CandidateJpaEntity(
            "cand-4", "Coumba", "Sow", "coumba@example.com", 
            "221774567890", "Master 1 Spécialité IA"
        );
        cand4.setFullName("Coumba Sow");
        cand4.setCurrentStep("admitted");
        cand4.setAdmissionStatus("accepted");
        cand4.setDiplomaUploaded(true);
        cand4.setIdCardUploaded(true);
        cand4.setRegistrationFeePaid(false);
        candidateRepository.save(cand4);
        
        CandidateJpaEntity cand5 = new CandidateJpaEntity(
            "cand-5", "Ousmane", "Diop", "ousmane@example.com", 
            "221775678901", "L2 Génie Civil"
        );
        cand5.setFullName("Ousmane Diop");
        cand5.setCurrentStep("rejected");
        cand5.setAdmissionStatus("rejected");
        cand5.setDiplomaUploaded(false);
        cand5.setIdCardUploaded(true);
        cand5.setRegistrationFeePaid(false);
        candidateRepository.save(cand5);
    }

    private void populateAdmissionSettings() {
        AdmissionSettingsJpaEntity settings = new AdmissionSettingsJpaEntity(
            "settings-1", "2025-2026", true, 
            LocalDateTime.of(2026, 6, 1, 0, 0),
            LocalDateTime.of(2026, 8, 31, 0, 0)
        );
        settings.setAnnouncementMessage("Les candidatures sont ouvertes pour l'année académique 2025-2026");
        settings.setShowRegisterButton(true);
        settings.setRegistrationFeeAmount(25000L);
        settings.setMaxCandidates(500);
        settings.setRequiredDocuments("{\"cni\": true, \"bulletin\": true, \"diplome\": true, \"motivation\": true}");
        settings.setInterviewRequired(true);
        settings.setMinAcademicScore(10.0);
        settings.setPassingScore(12.0);
        admissionSettingsRepository.save(settings);
    }

    private void populateNotificationTemplates() {
        NotificationTemplateJpaEntity tpl1 = new NotificationTemplateJpaEntity(
            "tpl-1", "payment_reminder", "Rappel de paiement", 
            "Rappel de paiement - École 221", 
            "Cher/Chère {{student_name}}, ce message vous rappelle que votre paiement de {{amount}} FCFA est en retard. Merci de régulariser votre situation.", 
            "email", "financial"
        );
        tpl1.setDefaultPriority("high");
        tpl1.setActionRequired(true);
        notificationTemplateRepository.save(tpl1);
        
        NotificationTemplateJpaEntity tpl2 = new NotificationTemplateJpaEntity(
            "tpl-2", "exam_alert", "Alerte examen", 
            "Examen imminent - {{course_name}}", 
            "Cher/Chère {{student_name}}, cet email vous rappelle que l'examen de {{course_name}} aura lieu le {{exam_date}}.", 
            "email", "academic"
        );
        tpl2.setDefaultPriority("normal");
        tpl2.setActionRequired(false);
        notificationTemplateRepository.save(tpl2);
        
        NotificationTemplateJpaEntity tpl3 = new NotificationTemplateJpaEntity(
            "tpl-3", "maintenance", "Maintenance système", 
            "Maintenance système programmée", 
            "Une maintenance du système est programmée le {{maintenance_date}} de {{start_time}} à {{end_time}}.", 
            "in_app", "administrative"
        );
        tpl3.setDefaultPriority("low");
        tpl3.setActionRequired(false);
        notificationTemplateRepository.save(tpl3);
    }

    private void populateActivityLogs() {
        activityLogRepository.save(new ActivityLogJpaEntity(
            "act-1", "inscription", "Nouveau inscrit : Amadou Diallo (Master IA)", 
            "usr-006", "Amadou Diallo", "ETUDIANT", "student", "usr-006", null
        ));
        activityLogRepository.save(new ActivityLogJpaEntity(
            "act-2", "paiement", "Paiement validé : Fatou Sow (221-MOBI)", 
            "usr-002", "Fatou Sow", "ETUDIANT", "payment", "pay-001", null
        ));
        activityLogRepository.save(new ActivityLogJpaEntity(
            "act-3", "securite", "Alerte : scan avec badge SUSPENDU", 
            "vigil-001", "Mamadou Vigil", "VIGIL", "security_event", "sec-001", null
        ));
        activityLogRepository.save(new ActivityLogJpaEntity(
            "act-4", "planning", "Cours planifié en Salle 101 (Algo)", 
            "usr-002", "Moussa Diallo", "PROFESSEUR", "course", "course-001", null
        ));
    }

    private void populateReminders() {
        reminderRepository.save(new ReminderJpaEntity(
            "rem-1", "Rappel devoir TP", true, "course-1", "usr-002", LocalDateTime.now().plusDays(3)
        ));
        reminderRepository.save(new ReminderJpaEntity(
            "rem-2", "Examen prévu", false, "course-1", "usr-002", LocalDateTime.now().plusDays(7)
        ));
        reminderRepository.save(new ReminderJpaEntity(
            "rem-3", "Livraison projet final", true, "course-2", "usr-002", LocalDateTime.now().plusDays(14)
        ));
    }

    private void populateScheduleSessions() {
        scheduleRepository.save(new ScheduleSessionJpaEntity(
            "sched-001", "Architecture Logicielle", "Lundi", "08:00 - 11:00", "Amphi A", 
            "course-1", "CM", "2026-07-27", "M1 GL", "usr-002"
        ));
        scheduleRepository.save(new ScheduleSessionJpaEntity(
            "sched-002", "Base de données NoSQL", "Mardi", "14:00 - 17:00", "Salle B2", 
            "course-2", "TD", "2026-07-28", "M1 GL", "usr-002"
        ));
        scheduleRepository.save(new ScheduleSessionJpaEntity(
            "sched-003", "Architecture Hexagonale", "Mercredi", "09:00 - 12:00", "Salle 102", 
            "course-1", "TP", "2026-07-29", "M1 GL", "usr-002"
        ));
    }

    private void populateStudentGrades() {
        studentGradeRepository.save(new StudentGradeJpaEntity(
            "g-1", "usr-001", "Architecture Hexagonale", "Dr. Aly Diatta", 6, 16.5, 18.0, 13.5, "Semestre 1"
        ));
        studentGradeRepository.save(new StudentGradeJpaEntity(
            "g-2", "usr-001", "DevOps & CI/CD", "M. Badiane", 4, 14.0, 15.5, 14.2, "Semestre 1"
        ));
    }

    private void populateStudentCourses() {
        studentCourseRepository.save(new StudentCourseJpaEntity(
            "sc-1", "usr-001", "course-1", "Architecture des Microservices", 3, 42, "en_cours", "Semestre 1"
        ));
    }

    private void populateStudentHomeworks() {
        studentHomeworkRepository.save(new StudentHomeworkJpaEntity(
            "hw-001", "usr-001", "course-1", "a_rendre", "TP Docker", LocalDateTime.now().plusDays(2), "haute"
        ));
    }

    private void populateLiveChatMessages() {
        liveChatMessageRepository.save(new LiveChatMessageJpaEntity(
            "msg-001", "session-001", "usr-002", "Professeur", "PROFESSEUR", "Bienvenue dans le cours"
        ));
        liveChatMessageRepository.save(new LiveChatMessageJpaEntity(
            "msg-002", "session-001", "usr-001", "Étudiant", "ETUDIANT", "Question sur le chapitre 1"
        ));
    }

    private void populateVigilProfiles() {
        vigilProfileRepository.save(new VigilProfileJpaEntity(
            "VIG-221", "Diallo", "Mamadou", "BADGE-VIG-001", "Jour", "Portail Principal"
        ));
    }

    private void populateTestUsers() {
        // Password en clair pour le test (à remplacer par BCrypt en production)
        String plainPassword = "admin123";

        userRepository.save(new UserJpaEntity(
            "user-001", "Admin", "Principal", "admin@ecole221.sn", plainPassword,
            RoleUtilisateur.ADMIN.value(), "2026-EC-001", null
        ));

        userRepository.save(new UserJpaEntity(
            "user-002", "Professeur", "Test", "prof@ecole221.sn", plainPassword,
            RoleUtilisateur.PROFESSEUR.value(), "2026-EC-002", null
        ));

        userRepository.save(new UserJpaEntity(
            "user-003", "Étudiant", "Test", "etudiant@ecole221.sn", plainPassword,
            RoleUtilisateur.ETUDIANT.value(), "2026-EC-003", null
        ));

        userRepository.save(new UserJpaEntity(
            "user-004", "Vigile", "Test", "vigil@ecole221.sn", plainPassword,
            RoleUtilisateur.VIGIL.value(), "2026-EC-004", null
        ));

        // Utilisateur Parent pour le module Parent
        userRepository.save(new UserJpaEntity(
            "usr-parent-01", "Diallo", "Mamadou", "parent221@gmail.com", "ecole221",
            RoleUtilisateur.PARENT.value(), "PARENT-001", null
        ));

        // Créer le parent dans la table parents
        populateParentData();
    }

    private void populateParentData() {
        // Vérifier si le parent existe déjà
        if (parentJpaRepository.findByUserId("usr-parent-01").isEmpty()) {
            ParentJpaEntity parent = new ParentJpaEntity();
            parent.setId("parent-001");
            parent.setUserId("usr-parent-01");
            parent.setNom("Diallo");
            parent.setPrenom("Mamadou");
            parent.setEmail("parent221@gmail.com");
            parent.setTelephone("771234567");
            parentJpaRepository.save(parent);

            // Créer le lien parent-enfant avec l'étudiant user-003
            ParentEnfantJpaEntity parentEnfant = new ParentEnfantJpaEntity();
            parentEnfant.setId("parent-enfant-001");
            parentEnfant.setParentId("parent-001");
            parentEnfant.setEnfantId("user-003");
            parentEnfantJpaRepository.save(parentEnfant);
        }
    }
}
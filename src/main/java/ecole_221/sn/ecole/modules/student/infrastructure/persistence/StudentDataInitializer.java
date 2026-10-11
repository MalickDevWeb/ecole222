package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import ecole_221.sn.ecole.modules.student.application.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//@Component
public class StudentDataInitializer implements CommandLineRunner {

    private final StudentDataRepository studentDataRepository;
    private final StudentService studentService;

    public StudentDataInitializer(StudentDataRepository studentDataRepository, StudentService studentService) {
        this.studentDataRepository = studentDataRepository;
        this.studentService = studentService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (studentDataRepository.count() == 0) {
            String studentId = "usr-001";
            
            // Extract from existing StudentService (which acts as the legacy mock provider)
            Map<String, Object> profile = studentService.getStudentProfile(studentId);
            Map<String, Object> dashboard = studentService.getStudentDashboard(studentId);
            
            // Recreate grades and courses from StudentController's legacy static mock
            List<Map<String, Object>> grades = List.of(
                Map.of("id", "g1", "module", "Architecture Hexagonale", "prof", "Dr. Aly Diatta", "ects", 6, "cc", 16.5, "examen", 18.0, "moyPromo", 13.5),
                Map.of("id", "g2", "module", "DevOps & CI/CD", "prof", "M. Badiane", "ects", 4, "cc", 14.0, "examen", 15.5, "moyPromo", 14.2)
            );
            
            List<Map<String, Object>> courses = List.of(
                Map.of("id", "course-1", "nom", "Architecture Microservices", "categorie", "Majeure", "professeur", "Dr. Aly Diatta", "salle", "En Ligne", "volumeHoraire", "45 Heures", "prochainCours", "Demain à 08:30", "description", "Concevoir des architectures distribuées...", "image", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=2070"),
                Map.of("id", "course-2", "nom", "Management Stratégique", "categorie", "Mineure", "professeur", "Mme. Fall", "salle", "Amphi B", "volumeHoraire", "20 Heures", "prochainCours", "Lundi à 14:00", "description", "Stratégies d'entreprise...", "image", "https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=2070")
            );

            StudentData data = new StudentData(studentId, profile, dashboard, grades, courses);
            studentDataRepository.save(data);
            
            System.out.println("Global Student Mock Data migrated to PostgreSQL via JSONB!");
        }
    }
}

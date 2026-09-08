package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Component
public class CourseDetailInitializer implements CommandLineRunner {

    private final CourseDetailRepository courseDetailRepository;
    private final ObjectMapper objectMapper;

    public CourseDetailInitializer(
            CourseDetailRepository courseDetailRepository,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.courseDetailRepository = courseDetailRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void run(String... args) throws Exception {
        if (courseDetailRepository.count() == 0) {
            // imageUrl = URL de l'image stockée (Unsplash pour demo, Cloudinary après upload via POST /media/upload)
            // La DB ne stocke QUE l'URL — Cloudinary/Unsplash héberge les bytes réels
            String json = """
{
  "c-1": {
    "imageUrl": "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&q=80&auto=format&fit=crop",
    "chapitres": [
      {
        "titre": "Chapitre 1 : Introduction aux Microservices",
        "lecons": [
          { "titre": "1.1 Architecture Monolithique vs Microservices", "duree": "45 min", "complete": true },
          { "titre": "1.2 D\\u00e9coupage des domaines m\\u00e9tier (DDD)", "duree": "1h 15m", "complete": true }
        ]
      },
      {
        "titre": "Chapitre 2 : Communication & Scalabilit\\u00e9",
        "lecons": [
          { "titre": "2.1 Communication synchrone (REST, gRPC)", "duree": "1h 00m", "complete": true },
          { "titre": "2.2 Messages asynchrones (RabbitMQ, Kafka)", "estDerniereEnseignee": true, "duree": "1h 30m", "complete": false },
          { "titre": "2.3 API Gateways & Service Discovery", "duree": "1h 10m", "complete": false }
        ]
      }
    ],
    "ressources": [
      { "titre": "Supports de cours complet - Microservices.pdf", "type": "pdf", "taille": "8.4 Mo" },
      { "titre": "Vid\\u00e9o - TP d'impl\\u00e9mentation.mp4", "type": "video", "taille": "124 Mo" }
    ],
    "quizzes": [
      {
        "question": "Quel protocole est g\\u00e9n\\u00e9ralement recommand\\u00e9 ?",
        "options": ["SOAP", "gRPC", "FTP", "WebSockets"],
        "reponseCorrecte": 1
      },
      {
        "question": "Quel composant agit comme point d'entr\\u00e9e ?",
        "options": ["Broker", "Registry", "Gateway", "DB"],
        "reponseCorrecte": 2
      }
    ],
    "devoirs": [
      { "id": "dev-1", "titre": "TP 1 : Configuration API Gateway", "dateLimite": "Dans 3 jours", "points": "20 pts", "statut": "A rendre" }
    ]
  },
  "c-2": {
    "imageUrl": "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&q=80&auto=format&fit=crop",
    "chapitres": [
      {
        "titre": "Chapitre 1 : Fondements de la Cybers\\u00e9curit\\u00e9",
        "lecons": [
          { "titre": "1.1 Concepts de base", "duree": "50 min", "complete": false },
          { "titre": "1.2 Cryptographie", "estDerniereEnseignee": true, "duree": "1h 30m", "complete": false }
        ]
      }
    ],
    "ressources": [
      { "titre": "Intro Crypto.pdf", "type": "pdf", "taille": "6.2 Mo" }
    ],
    "quizzes": [
      {
        "question": "Que signifie CIA ?",
        "options": ["Central", "Confidentiality, Integrity, Availability", "Cloud", "Computer"],
        "reponseCorrecte": 1
      }
    ],
    "devoirs": [
      { "id": "dev-5", "titre": "Exercice : D\\u00e9chiffrement", "dateLimite": "Dans 10 jours", "points": "20 pts", "statut": "A rendre" }
    ]
  }
}
""";

            Map<String, Map<String, Object>> map = objectMapper.readValue(json, new TypeReference<>() {});
            for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                courseDetailRepository.save(new CourseDetail(entry.getKey(), entry.getValue()));
            }
            System.out.println("Course details (with imageUrl) migrated to PostgreSQL JSONB!");
        }
    }
}

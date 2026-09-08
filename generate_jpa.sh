#!/bin/bash
# Script pour générer les entités JPA et les Repositories Spring Data restants

BASE_DIR="src/main/java/ecole_221/sn/ecole/modules"

# Liste des entités à générer sous la forme : Module:Entité:Table
# Format: MODULE_NAME:ENTITY_NAME:TABLE_NAME:FIELDS...
# Fields format: type:name

ENTITIES=(
    "student:Attendance:attendances:String:date:String:studentId:String:studentName:String:status:String:courseId"
    "professor:Homework:homeworks:String:courseId:String:title:String:description:String:priority:String:deadline"
    "professor:Lesson:lessons:String:courseId:String:title:int:duration:String:date"
    "professor:Module:modules:String:courseId:String:name"
    "professor:Quiz:quizzes:String:courseId:String:title:int:duration"
    "professor:Grade:grades:String:studentId:String:courseId:Double:cc:Double:examen:Double:average"
    "collaboration:Workgroup:workgroups:String:name:String:description:int:membersCount"
    "collaboration:Document:documents:String:groupId:String:name:String:type:String:size:String:url:String:addedBy"
    "collaboration:Task:tasks:String:groupId:String:title:String:status:String:assignedTo"
    "collaboration:Message:messages:String:groupId:String:sender:String:content"
    "collaboration:Meet:meets:String:title:String:link:String:time"
    "planning:Room:rooms:String:name:int:capacity"
    "planning:Filiere:filieres:String:code:String:name"
    "planning:Classe:classes:String:name:String:filiereId"
    "planning:Slot:slots:String:day:String:timeRange"
)

for entry in "${ENTITIES[@]}"; do
    IFS=':' read -r module entity table fields <<< "$entry"
    
    # Extract fields (everything after table)
    rest=${entry#*:*:*:}
    
    PACKAGE_DIR="$BASE_DIR/$module/infrastructure/persistence"
    PACKAGE_NAME="ecole_221.sn.ecole.modules.$module.infrastructure.persistence"
    
    mkdir -p "$PACKAGE_DIR"
    
    ENTITY_FILE="$PACKAGE_DIR/${entity}JpaEntity.java"
    REPO_FILE="$PACKAGE_DIR/${entity}JpaRepository.java"
    
    # 1. Générer l'entité
    echo "Génération de ${entity}JpaEntity..."
    
    cat <<EOF > "$ENTITY_FILE"
package $PACKAGE_NAME;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "$table")
public class ${entity}JpaEntity {

    @Id
    private String id;
EOF

    IFS=':' read -ra FIELD_ARRAY <<< "$rest"
    for (( i=0; i<${#FIELD_ARRAY[@]}; i+=2 )); do
        type="${FIELD_ARRAY[$i]}"
        name="${FIELD_ARRAY[$i+1]}"
        echo "    private $type $name;" >> "$ENTITY_FILE"
    done
    
    # Constructor and Getters/Setters
    cat <<EOF >> "$ENTITY_FILE"

    public ${entity}JpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
EOF

    for (( i=0; i<${#FIELD_ARRAY[@]}; i+=2 )); do
        type="${FIELD_ARRAY[$i]}"
        name="${FIELD_ARRAY[$i+1]}"
        CapitalizedName="$(tr '[:lower:]' '[:upper:]' <<< ${name:0:1})${name:1}"
        echo "    public $type get${CapitalizedName}() { return $name; }" >> "$ENTITY_FILE"
        echo "    public void set${CapitalizedName}($type $name) { this.$name = $name; }" >> "$ENTITY_FILE"
    done
    
    echo "}" >> "$ENTITY_FILE"
    
    # 2. Générer le Repository
    echo "Génération de ${entity}JpaRepository..."
    cat <<EOF > "$REPO_FILE"
package $PACKAGE_NAME;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${entity}JpaRepository extends JpaRepository<${entity}JpaEntity, String> {
    // Les requêtes par défaut peuvent être ajoutées ici (ex: findByCourseId)
}
EOF

done

echo "Toutes les entités et repositories ont été générés avec succès !"

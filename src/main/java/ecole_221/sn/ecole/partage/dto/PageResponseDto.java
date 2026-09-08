package ecole_221.sn.ecole.partage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO générique pour les réponses paginées.
 * Ce DTO est partagé entre tous les modules pour assurer la cohérence.
 * 
 * @param <T> Type des éléments dans la page
 */
@Schema(description = "Réponse paginée générique")
public record PageResponseDto<T>(
    @Schema(description = "Contenu de la page")
    List<T> content,
    
    @Schema(description = "Numéro de la page")
    int page,
    
    @Schema(description = "Taille de la page")
    int size,
    
    @Schema(description = "Nombre total d'éléments")
    long totalElements,
    
    @Schema(description = "Nombre total de pages")
    int totalPages,
    
    @Schema(description = "Dernière page ou non")
    boolean last
) {
    public PageResponseDto {
        // Validation can be added here
    }
    
    /**
     * Vérifie si la page est vide.
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
    
    /**
     * Convertit depuis un PageResult legacy.
     */
    public static <T> PageResponseDto<T> fromPageResult(ecole_221.sn.ecole.partage.PageResult<T> pageResult) {
        return new PageResponseDto<>(
            pageResult.getContent(),
            pageResult.getPageNumber(),
            pageResult.getPageSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getPageNumber() >= pageResult.getTotalPages() - 1
        );
    }
}
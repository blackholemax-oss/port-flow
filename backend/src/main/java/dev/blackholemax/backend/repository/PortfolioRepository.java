package dev.blackholemax.backend.repository;

import dev.blackholemax.backend.entity.Portfolio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @EntityGraph(attributePaths = "projects")
    Optional<Portfolio> findBySlug(String slug);

    List<Portfolio> findByUserIdOrderByIdDesc(Long userId);

    List<Portfolio> findAllByOrderByIdDesc();

    @Query("select p.id from Portfolio p where p.slug = :slug and p.isPublished = true")
    Optional<Long> findPublishedIdBySlug(@Param("slug") String slug);
}

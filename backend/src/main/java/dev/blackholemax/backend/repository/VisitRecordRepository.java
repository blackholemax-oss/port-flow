package dev.blackholemax.backend.repository;

import dev.blackholemax.backend.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    long countByPortfolioId(Long portfolioId);

    @Query("select count(distinct v.visitorId) from VisitRecord v where v.portfolioId = :portfolioId")
    long countDistinctVisitorByPortfolioId(@Param("portfolioId") Long portfolioId);
}
package org.example.QuanLyMuaVu.Repository;

import java.util.List;
import java.util.Optional;

import org.example.QuanLyMuaVu.Entity.Document;
import org.example.QuanLyMuaVu.Enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

        List<Document> findByTitleContainingIgnoreCase(String title);

        Page<Document> findByTitleContainingIgnoreCase(String title, Pageable pageable);

        /**
         * Find all visible documents with optional filters including type
         */
        @Query("SELECT d FROM Document d WHERE d.isActive = true AND d.isPublic = true " +
                        "AND (:q IS NULL OR :q = '' OR LENGTH(:q) < 2 OR LOWER(d.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :q, '%'))) "
                        +
                        "AND (:crop IS NULL OR :crop = '' OR d.crop = :crop) " +
                        "AND (:stage IS NULL OR :stage = '' OR d.stage = :stage) " +
                        "AND (:topic IS NULL OR :topic = '' OR d.topic = :topic) " +
                        "AND (:type IS NULL OR d.documentType = :type)")
        Page<Document> findAllVisibleWithType(
                        @Param("q") String q,
                        @Param("crop") String crop,
                        @Param("stage") String stage,
                        @Param("topic") String topic,
                        @Param("type") DocumentType type,
                        Pageable pageable);

        /**
         * Find all visible documents (legacy method without type filter)
         */
        @Query("SELECT d FROM Document d WHERE d.isActive = true AND d.isPublic = true " +
                        "AND (:q IS NULL OR :q = '' OR LENGTH(:q) < 2 OR LOWER(d.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :q, '%'))) "
                        +
                        "AND (:crop IS NULL OR :crop = '' OR d.crop = :crop) " +
                        "AND (:stage IS NULL OR :stage = '' OR d.stage = :stage) " +
                        "AND (:topic IS NULL OR :topic = '' OR d.topic = :topic)")
        Page<Document> findAllVisible(
                        @Param("q") String q,
                        @Param("crop") String crop,
                        @Param("stage") String stage,
                        @Param("topic") String topic,
                        Pageable pageable);

        /**
         * Find visible document by id
         */
        @Query("SELECT d FROM Document d WHERE d.id = :id AND d.isActive = true AND d.isPublic = true")
        Optional<Document> findVisibleById(@Param("id") Integer id);

        /**
         * Find documents by IDs (for favorites/recent) that are visible
         */
        @Query("SELECT d FROM Document d WHERE d.id IN :ids AND d.isActive = true AND d.isPublic = true")
        List<Document> findVisibleByIds(@Param("ids") List<Integer> ids);

        /**
         * Get distinct topics for meta endpoint
         */
        @Query("SELECT DISTINCT d.topic FROM Document d WHERE d.isActive = true AND d.isPublic = true AND d.topic IS NOT NULL ORDER BY d.topic")
        List<String> findDistinctTopics();

        /**
         * Get distinct stages for meta endpoint
         */
        @Query("SELECT DISTINCT d.stage FROM Document d WHERE d.isActive = true AND d.isPublic = true AND d.stage IS NOT NULL ORDER BY d.stage")
        List<String> findDistinctStages();
}

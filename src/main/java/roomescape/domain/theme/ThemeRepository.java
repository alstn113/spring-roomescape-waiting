package roomescape.domain.theme;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThemeRepository extends JpaRepository<Theme, Long> {

    default Theme getByIdentifier(Long id) {
        return findById(id)
                .orElseThrow(() -> new NoSuchElementException("해당 id의 테마가 존재하지 않습니다."));
    }

    @Query(value = """
                SELECT
                th.id,
                th.name,
                th.description,
                th.thumbnail
                FROM Theme AS th
                JOIN Reservation AS r
                ON th.id = r.theme_id
                WHERE r.date BETWEEN :startDate AND :endDate
                GROUP BY th.id
                ORDER BY COUNT(th.id) DESC
                LIMIT :limit
            """, nativeQuery = true)
    List<Theme> findPopularThemes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );

    boolean existsByName(String name);
}

package roomescape.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import roomescape.domain.member.Member;
import roomescape.domain.member.Role;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationRepository;
import roomescape.domain.reservation.ReservationStatus;
import roomescape.domain.reservation.dto.ReservationWithRankDto;
import roomescape.domain.reservation.ReservationTime;
import roomescape.domain.reservation.Theme;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("예약을 추가한다.")
    void save() {
        Member member = createMember();
        ReservationTime time = createReservationTime();
        Theme theme = createTheme();

        Reservation reservation = new Reservation(null, LocalDate.of(2024, 5, 4), member, time, theme,
                ReservationStatus.WAITING);

        Reservation saved = reservationRepository.save(reservation);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(saved.getId()).isNotNull();
            softly.assertThat(saved.getDate()).isEqualTo("2024-05-04");
            softly.assertThat(saved.getTime().getStartAt()).isEqualTo("10:00");
            softly.assertThat(saved.getMember().getEmail()).isEqualTo("example@gmail.com");
            softly.assertThat(saved.getStatus()).isEqualTo(ReservationStatus.WAITING);
        });
    }

    @Test
    @DisplayName("모든 예약들을 조회한다.")
    void findAll() {
        createReservation();

        List<Reservation> reservations = reservationRepository.findAllByConditions(1L, 1L, LocalDate.of(2024, 5, 4),
                LocalDate.of(2024, 5, 4));
        Reservation reservation = reservations.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reservations).hasSize(1);
            softly.assertThat(reservation.getDate()).isEqualTo("2024-05-04");
            softly.assertThat(reservation.getTime().getStartAt()).isEqualTo("10:00");
            softly.assertThat(reservation.getMember().getEmail()).isEqualTo("example@gmail.com");
            softly.assertThat(reservation.getMember().getPassword()).isEqualTo("password");
            softly.assertThat(reservation.getMember().getRole()).isEqualTo(Role.USER);
            softly.assertThat(reservation.getMember().getName()).isEqualTo("구름");
            softly.assertThat(reservation.getTheme().getName()).isEqualTo("테마1");
            softly.assertThat(reservation.getTheme().getDescription()).isEqualTo("테마1 설명");
            softly.assertThat(reservation.getTheme().getThumbnail()).isEqualTo("https://example1.com");
        });
    }

    @Test
    @DisplayName("회원 id로 대기 순서를 포함한 예약들을 조회한다.")
    @Sql("/waitings.sql")
    void findReservationWithRanksByMemberId() {
        List<ReservationWithRankDto> reservationWithRanks = reservationRepository.findReservationWithRanksByMemberId(
                4L);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reservationWithRanks).hasSize(3);
            softly.assertThat(reservationWithRanks.get(0).rank()).isEqualTo(3);
            softly.assertThat(reservationWithRanks.get(1).rank()).isEqualTo(0);
            softly.assertThat(reservationWithRanks.get(2).rank()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("예약을 삭제한다.")
    void deleteById() {
        createReservation();

        reservationRepository.deleteById(1L);

        assertThat(reservationRepository.existsById(1L)).isFalse();
    }

    @Test
    @DisplayName("id에 해당하는 예약이 존재하는지 확인한다.")
    void existsById() {
        createReservation();

        assertThat(reservationRepository.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("예약 시간 id에 해당하는 예약이 존재하는지 확인한다.")
    void existsByTimeId() {
        createReservation();

        assertThat(reservationRepository.existsByTimeId(1L)).isTrue();
    }

    @Test
    @DisplayName("테마 id에 해당하는 예약이 존재하는지 확인한다.")
    void existsByThemeId() {
        createReservation();

        assertThat(reservationRepository.existsByThemeId(1L)).isTrue();
    }

    @Test
    @DisplayName("날짜와 시간 id, 테마 id에 해당하는 예약이 존재하는지 확인한다.")
    void existsByReservation() {
        createReservation();

        LocalDate date = LocalDate.of(2024, 5, 4);
        assertThat(reservationRepository.existsByReservation(date, 1L, 1L)).isTrue();
        assertThat(reservationRepository.existsByReservation(date, 1L, 2L)).isFalse();
    }

    private void createReservation() {
        Member member = createMember();
        ReservationTime reservationTime = createReservationTime();
        Theme theme = createTheme();

        jdbcTemplate.update("INSERT INTO reservation (id, date, member_id, time_id, theme_id, status) "
                + "VALUES (1, '2024-05-04', 1, 1, 1, 'RESERVED')");

        new Reservation(1L, LocalDate.of(2024, 5, 4), member, reservationTime, theme, ReservationStatus.RESERVED);
    }

    private Member createMember() {
        jdbcTemplate.update("INSERT INTO member (id, email, password, name, role) "
                + "VALUES (1, 'example@gmail.com', 'password', '구름', 'USER')");

        return new Member(1L, "example@gmail.com", "password", "구름", Role.USER);
    }

    private Theme createTheme() {
        jdbcTemplate.update("INSERT INTO theme (id, name, description, thumbnail) "
                + "VALUES (1, '테마1', '테마1 설명', 'https://example1.com')");

        return new Theme(1L, "테마1", "테마1 설명", "https://example1.com");
    }

    private ReservationTime createReservationTime() {
        jdbcTemplate.update("INSERT INTO reservation_time (id, start_at) " + "VALUES (1, '10:00')");

        return new ReservationTime(1L, LocalTime.of(10, 0));
    }
}

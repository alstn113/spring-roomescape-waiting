package roomescape.presentation.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import roomescape.domain.member.Role;
import roomescape.domain.reservation.ReservationStatus;
import roomescape.domain.reservation.ReservationTime;
import roomescape.domain.reservation.ReservationTimeRepository;
import roomescape.domain.reservation.Theme;
import roomescape.domain.reservation.ThemeRepository;
import roomescape.presentation.dto.request.ReservationWebRequest;
import roomescape.application.dto.response.MemberResponse;
import roomescape.application.dto.response.MyReservationResponse;
import roomescape.application.dto.response.ReservationResponse;
import roomescape.application.dto.response.ReservationTimeResponse;
import roomescape.application.dto.response.ThemeResponse;
import roomescape.support.BaseControllerTest;

@Sql("/member.sql")
class ReservationControllerTest extends BaseControllerTest {

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @BeforeEach
    void setUp() {
        reservationTimeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        themeRepository.save(new Theme("테마 이름", "테마 설명", "https://example.com"));

        userLogin();
    }

    @TestFactory
    @DisplayName("예약을 생성, 조회, 삭제한다.")
    Stream<DynamicTest> reservationControllerTests() {
        return Stream.of(
                DynamicTest.dynamicTest("예약을 생성한다.", this::addReservation),
                DynamicTest.dynamicTest("예약을 모두 조회한다.", this::getReservationsByConditions),
                DynamicTest.dynamicTest("예약을 삭제한다.", this::deleteReservationById)
        );
    }

    @TestFactory
    @DisplayName("중복된 예약을 생성하면 실패한다.")
    Stream<DynamicTest> failWhenDuplicatedReservation() {
        return Stream.of(
                DynamicTest.dynamicTest("예약을 생성한다.", this::addReservation),
                DynamicTest.dynamicTest("이미 존재하는 예약을 생성한다.", this::addReservationFailWhenDuplicatedReservation)
        );
    }

    @Test
    @DisplayName("지나간 날짜/시간에 대한 예약은 실패한다.")
    void failWhenDateTimePassed() {
        ReservationWebRequest request = new ReservationWebRequest(LocalDate.of(2024, 4, 7), 1L, 1L);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .cookie("token", token)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            softly.assertThat(response.body().asString()).contains("지나간 날짜/시간에 대한 예약은 불가능합니다.");
        });
    }

    @Test
    @DisplayName("존재하지 않는 예약을 삭제하면 실패한다.")
    void failWhenNotFoundReservation() {
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/reservations/1")
                .then().log().all()
                .extract();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
            softly.assertThat(response.body().asString()).contains("해당 id의 예약이 존재하지 않습니다.");
        });
    }

    @Test
    @DisplayName("나의 예약들을 조회한다")
    void getMyReservations() {
        addReservation();

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .cookie("token", token)
                .when().get("/reservations/mine")
                .then().log().all()
                .extract();

        List<MyReservationResponse> reservationResponses = response.jsonPath()
                .getList(".", MyReservationResponse.class);

        MyReservationResponse myReservationResponse = reservationResponses.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(reservationResponses).hasSize(1);

            softly.assertThat(myReservationResponse.id()).isEqualTo(1L);
            softly.assertThat(myReservationResponse.date()).isEqualTo(LocalDate.of(2024, 4, 9));
            softly.assertThat(myReservationResponse.time()).isEqualTo(LocalTime.of(11, 0));
            softly.assertThat(myReservationResponse.theme()).isEqualTo("테마 이름");
            softly.assertThat(myReservationResponse.status()).isEqualTo(ReservationStatus.RESERVED);
            softly.assertThat(myReservationResponse.rank()).isEqualTo(0L);
        });
    }

    private void addReservation() {
        ReservationWebRequest request = new ReservationWebRequest(LocalDate.of(2024, 4, 9), 1L, 1L);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .cookie("token", token)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();

        ReservationResponse reservationResponse = response.as(ReservationResponse.class);
        MemberResponse memberResponse = reservationResponse.member();
        ReservationTimeResponse reservationTimeResponse = reservationResponse.time();
        ThemeResponse themeResponse = reservationResponse.theme();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            softly.assertThat(response.header("Location")).isEqualTo("/reservations/1");

            softly.assertThat(reservationResponse.date()).isEqualTo(LocalDate.of(2024, 4, 9));
            softly.assertThat(memberResponse).isEqualTo(new MemberResponse(2L, "user@gmail.com", "유저", Role.USER));
            softly.assertThat(reservationTimeResponse).isEqualTo(new ReservationTimeResponse(1L, LocalTime.of(11, 0)));
            softly.assertThat(themeResponse).isEqualTo(new ThemeResponse(1L, "테마 이름", "테마 설명", "https://example.com"));
        });
    }

    private void getReservationsByConditions() {
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/reservations")
                .then().log().all()
                .extract();

        List<ReservationResponse> reservationResponses = response.jsonPath()
                .getList(".", ReservationResponse.class);

        ReservationResponse reservationResponse = reservationResponses.get(0);

        MemberResponse memberResponse = reservationResponse.member();
        ReservationTimeResponse reservationTimeResponse = reservationResponse.time();
        ThemeResponse themeResponse = reservationResponse.theme();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            softly.assertThat(reservationResponses).hasSize(1);

            softly.assertThat(reservationResponse.date()).isEqualTo(LocalDate.of(2024, 4, 9));
            softly.assertThat(memberResponse).isEqualTo(new MemberResponse(2L, "user@gmail.com", "유저", Role.USER));
            softly.assertThat(reservationTimeResponse).isEqualTo(new ReservationTimeResponse(1L, LocalTime.of(11, 0)));
            softly.assertThat(themeResponse).isEqualTo(new ThemeResponse(1L, "테마 이름", "테마 설명", "https://example.com"));
        });
    }

    private void deleteReservationById() {
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/reservations/1")
                .then().log().all()
                .extract();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        });
    }

    private void addReservationFailWhenDuplicatedReservation() {
        ReservationWebRequest request = new ReservationWebRequest(LocalDate.of(2024, 4, 9), 1L, 1L);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .cookie("token", token)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            softly.assertThat(response.body().asString()).contains("해당 날짜/시간에 이미 예약이 존재합니다.");
        });
    }
}

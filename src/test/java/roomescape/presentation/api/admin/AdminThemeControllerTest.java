package roomescape.presentation.api.admin;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import roomescape.application.dto.request.ThemeRequest;
import roomescape.application.dto.response.ThemeResponse;
import roomescape.domain.member.Member;
import roomescape.domain.member.MemberRepository;
import roomescape.domain.member.Role;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationRepository;
import roomescape.domain.reservation.ReservationStatus;
import roomescape.domain.reservation.ReservationTime;
import roomescape.domain.reservation.ReservationTimeRepository;
import roomescape.domain.reservation.Theme;
import roomescape.domain.reservation.ThemeRepository;
import roomescape.presentation.BaseControllerTest;

@Sql("/member.sql")
class AdminThemeControllerTest extends BaseControllerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Nested
    @DisplayName("테마를 추가하는 경우")
    class AddTheme extends BaseControllerTest {

        @Test
        @DisplayName("성공하면 201을 반환한다.")
        void success() {
            adminLogin();

            ThemeRequest request = new ThemeRequest("테마 이름", "테마 설명", "https://example.com/image.jpg");

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("token", token)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/admin/themes")
                    .then().log().all()
                    .extract();

            ThemeResponse themeResponse = response.as(ThemeResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
                softly.assertThat(response.header("Location")).isEqualTo("/themes/1");
                softly.assertThat(themeResponse)
                        .isEqualTo(new ThemeResponse(1L, "테마 이름", "테마 설명", "https://example.com/image.jpg"));
            });
        }

        @Test
        @DisplayName("이미 존재하는 테마 이름이면 400을 반환한다.")
        void addThemeFailWhenNameAlreadyExists() {
            adminLogin();

            themeRepository.save(new Theme("테마 이름", "테마 설명", "https://example.com/image.jpg"));

            ThemeRequest request = new ThemeRequest("테마 이름", "테마 설명-2", "https://example.com/image-2.jpg");

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("token", token)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/admin/themes")
                    .then().log().all()
                    .extract();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.body().asString()).contains("해당 이름의 테마는 이미 존재합니다.");
            });
        }
    }

    @Nested
    @DisplayName("테마를 삭제하는 경우")
    class DeleteTheme extends BaseControllerTest {
        @Test
        @DisplayName("성공하면 204를 반환한다.")
        void success() {
            adminLogin();

            Theme theme = themeRepository.save(new Theme("테마 이름", "테마 설명", "https://example.com"));

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("token", token)
                    .when().delete("/admin/themes/" + theme.getId())
                    .then().log().all()
                    .extract();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
            });
        }


        @Test
        @DisplayName("존재하지 않는 테마를 삭제하면 404를 반환한다.")
        void deleteThemeByIdFailWhenNotFoundId() {
            adminLogin();

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("token", token)
                    .when().delete("/admin/themes/1")
                    .then().log().all()
                    .extract();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
                softly.assertThat(response.body().asString()).contains("해당 id의 테마가 존재하지 않습니다.");
            });
        }

        @Test
        @DisplayName("이미 사용 중인 테마을 삭제하면 400을 반환한다.")
        void deleteThemeByIdFailWhenUsedTheme() {
            adminLogin();

            Member member = memberRepository.save(new Member("member@gmail.com", "password", "member", Role.USER));
            ReservationTime time = reservationTimeRepository.save(new ReservationTime(LocalTime.of(10, 30)));
            Theme theme = themeRepository.save(new Theme("테마 이름", "테마 설명", "https://example.com"));

            reservationRepository.save(
                    new Reservation(LocalDate.of(2024, 6, 22), member, time, theme, ReservationStatus.RESERVED));

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("token", token)
                    .when().delete("/admin/themes/" + theme.getId())
                    .then().log().all()
                    .extract();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.body().asString()).contains("해당 테마를 사용하는 예약이 존재합니다.");
            });
        }
    }
}
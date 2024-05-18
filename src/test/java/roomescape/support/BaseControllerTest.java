package roomescape.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import roomescape.support.config.TestConfig;
import roomescape.application.dto.request.LoginRequest;
import roomescape.support.extension.DatabaseClearExtension;

@SpringBootTest(
        classes = TestConfig.class,
        webEnvironment = WebEnvironment.RANDOM_PORT
)
@ExtendWith(DatabaseClearExtension.class)
public abstract class BaseControllerTest {

    protected static final Long ADMIN_ID = 1L;
    protected static final String ADMIN_EMAIL = "admin@gmail.com";
    protected static final String ADMIN_PASSWORD = "abc123";
    protected static final String USER_EMAIL = "user@gmail.com";
    protected static final String USER_PASSWORD = "abc123";

    @LocalServerPort
    private int port;

    protected String token;

    @BeforeEach
    void environmentSetUp() {
        RestAssured.port = port;
    }

    protected void adminLogin() {
        token = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))
                .when().post("/login")
                .then().log().cookies().extract().cookie("token");
    }

    protected void userLogin() {
        token = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(USER_EMAIL, USER_PASSWORD))
                .when().post("/login")
                .then().log().cookies().extract().cookie("token");
    }
}

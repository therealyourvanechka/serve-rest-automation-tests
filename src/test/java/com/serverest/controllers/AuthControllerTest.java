package com.serverest.controllers;

import com.serverest.BaseTest;
import com.serverest.model.request.LoginRequest;
import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.LoginResponse;
import com.serverest.model.response.MessageResponse;
import com.serverest.util.ServeRestDataFactory;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Login")
class AuthControllerTest extends BaseTest {

    private static String userEmail;
    private static String userPassword;
    private static String userId;

    @BeforeAll
    static void setUp() {
        UsuarioRequest request = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse response = usuariosClient.create(request);
        userId = response.getId();
        userEmail = request.getEmail();
        userPassword = request.getPassword();
    }

    @AfterAll
    static void tearDown() {
        usuariosClient.delete(userId);
    }

    @Test
    @Tag("LG-01")
    @DisplayName("LG-01: Успешный вход")
    @Severity(SeverityLevel.CRITICAL)
    void shouldLoginSuccessfully() {
        LoginResponse response = authClient.login(
                LoginRequest.builder()
                        .email(userEmail)
                        .password(userPassword)
                        .build()
        );

        assertThat(response.getAuthorization())
                .as("Токен авторизации не должен быть null или пустым")
                .isNotBlank();
    }

    @Test
    @Tag("LG-02")
    @DisplayName("LG-02: Логин с неверным паролем")
    @Severity(SeverityLevel.CRITICAL)
    void shouldReturn401ForInvalidPassword() {
        Response response = authClient.loginRaw(
                LoginRequest.builder()
                        .email(userEmail)
                        .password(ServeRestDataFactory.randomPassword())
                        .build()
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("LG-03")
    @DisplayName("LG-03: Логин с несуществующим email")
    @Severity(SeverityLevel.CRITICAL)
    void shouldReturn401ForNonExistentEmail() {
        Response response = authClient.loginRaw(
                LoginRequest.builder()
                        .email(ServeRestDataFactory.randomEmail())
                        .password(ServeRestDataFactory.randomPassword())
                        .build()
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("LG-04")
    @DisplayName("LG-04: Логин без пароля (обязательное поле)")
    @Severity(SeverityLevel.CRITICAL)
    void shouldReturn400ForMissingPassword() {
        Response response = authClient.loginRaw(
                LoginRequest.builder()
                        .email(userEmail)
                        .build()
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }
}

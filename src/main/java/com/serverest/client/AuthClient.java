package com.serverest.client;

import com.serverest.exception.AuthenticationException;
import com.serverest.model.request.LoginRequest;
import com.serverest.model.response.LoginResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class AuthClient extends BaseClient {

    @Step("Логин пользователя POST /login")
    public LoginResponse login(LoginRequest body) {
        return post("/login", body)
                .then()
                .extract()
                .as(LoginResponse.class);
    }

    @Step("Отправка сырого POST /login")
    public Response loginRaw(Object body) {
        return post("/login", body);
    }

    @Step("Логин и получение токена")
    public String loginAndGetToken(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        LoginResponse response = login(request);
        if (response.getAuthorization() == null || response.getAuthorization().isBlank()) {
            throw new AuthenticationException(
                    "Не удалось получить токен: " + response.getMessage());
        }
        return response.getAuthorization();
    }
}

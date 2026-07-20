package com.serverest.client;

import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.MessageResponse;
import com.serverest.model.response.UsuarioResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.Map;

public class UsuariosClient extends BaseClient {

    @Step("Создание пользователя POST /usuarios")
    public MessageResponse create(UsuarioRequest body) {
        return createRaw(body)
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().as(MessageResponse.class);
    }

    @Step("Создание пользователя POST /usuarios (raw)")
    public Response createRaw(Object body) {
        return post("/usuarios", body);
    }

    @Step("Список пользователей GET /usuarios")
    public Response getAll() {
        return get("/usuarios");
    }

    @Step("Список пользователей с фильтрами GET /usuarios")
    public Response getAll(Map<String, Object> queryParams) {
        return get("/usuarios", queryParams);
    }

    @Step("Поиск пользователя GET /usuarios/{id}")
    public UsuarioResponse getById(String id) {
        return getByIdRaw(id)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(UsuarioResponse.class);
    }

    @Step("Поиск пользователя GET /usuarios/{id} (raw)")
    public Response getByIdRaw(String id) {
        return super.getById("/usuarios", id);
    }

    @Step("Обновление пользователя PUT /usuarios/{id}")
    public MessageResponse update(String id, UsuarioRequest body) {
        return updateRaw(id, body)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(MessageResponse.class);
    }

    @Step("Обновление пользователя PUT /usuarios/{id} (raw)")
    public Response updateRaw(String id, Object body) {
        return super.put("/usuarios", id, body);
    }

    @Step("Удаление пользователя DELETE /usuarios/{id}")
    public MessageResponse delete(String id) {
        return deleteRaw(id)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(MessageResponse.class);
    }

    @Step("Удаление пользователя DELETE /usuarios/{id} (raw)")
    public Response deleteRaw(String id) {
        return super.delete("/usuarios", id);
    }
}

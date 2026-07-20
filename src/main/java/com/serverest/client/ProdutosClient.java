package com.serverest.client;

import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.response.MessageResponse;
import com.serverest.model.response.ProdutoResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.Map;

public class ProdutosClient extends BaseClient {

    public ProdutosClient() {
    }

    public ProdutosClient(String token) {
        super(token);
    }

    @Step("Создание товара POST /produtos")
    public MessageResponse create(ProdutoRequest body) {
        return createRaw(body)
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().as(MessageResponse.class);
    }

    @Step("Создание товара POST /produtos (raw)")
    public Response createRaw(Object body) {
        return post("/produtos", body);
    }

    @Step("Список товаров GET /produtos")
    public Response getAll() {
        return get("/produtos");
    }

    @Step("Список товаров с фильтрами GET /produtos")
    public Response getAll(Map<String, Object> queryParams) {
        return get("/produtos", queryParams);
    }

    @Step("Поиск товара GET /produtos/{id}")
    public ProdutoResponse getById(String id) {
        return getByIdRaw(id)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(ProdutoResponse.class);
    }

    @Step("Поиск товара GET /produtos/{id} (raw)")
    public Response getByIdRaw(String id) {
        return super.getById("/produtos", id);
    }

    @Step("Обновление товара PUT /produtos/{id}")
    public MessageResponse update(String id, ProdutoRequest body) {
        return updateRaw(id, body)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(MessageResponse.class);
    }

    @Step("Обновление товара PUT /produtos/{id} (raw)")
    public Response updateRaw(String id, Object body) {
        return super.put("/produtos", id, body);
    }

    @Step("Удаление товара DELETE /produtos/{id}")
    public MessageResponse delete(String id) {
        return deleteRaw(id)
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(MessageResponse.class);
    }

    @Step("Удаление товара DELETE /produtos/{id} (raw)")
    public Response deleteRaw(String id) {
        return super.delete("/produtos", id);
    }
}

package com.serverest.client;

import com.serverest.model.request.CarrinhoRequest;
import com.serverest.model.response.CarrinhoResponse;
import com.serverest.model.response.MessageResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.Map;

public class CarrinhosClient extends BaseClient {

    public CarrinhosClient() {
    }

    public CarrinhosClient(String token) {
        super(token);
    }

    @Step("Создание корзины POST /carrinhos")
    public MessageResponse create(CarrinhoRequest body) {
        return createRaw(body)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .as(MessageResponse.class);
    }

    @Step("Создание корзины POST /carrinhos (raw)")
    public Response createRaw(Object body) {
        return post("/carrinhos", body);
    }

    @Step("Список корзин GET /carrinhos")
    public Response getAll() {
        return get("/carrinhos");
    }

    @Step("Список корзин с фильтрами GET /carrinhos")
    public Response getAll(Map<String, Object> queryParams) {
        return get("/carrinhos", queryParams);
    }

    @Step("Поиск корзины GET /carrinhos/{id}")
    public CarrinhoResponse getById(String id) {
        return getByIdRaw(id)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(CarrinhoResponse.class);
    }

    @Step("Поиск корзины GET /carrinhos/{id} (raw)")
    public Response getByIdRaw(String id) {
        return super.getById("/carrinhos", id);
    }

    @Step("Завершение покупки DELETE /carrinhos/concluir-compra")
    public MessageResponse checkout() {
        return checkoutRaw()
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(MessageResponse.class);
    }

    @Step("Завершение покупки DELETE /carrinhos/concluir-compra (raw)")
    public Response checkoutRaw() {
        return super.deleteEndpoint("/carrinhos/concluir-compra");
    }

    @Step("Отмена покупки DELETE /carrinhos/cancelar-compra")
    public MessageResponse cancelPurchase() {
        return cancelPurchaseRaw()
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(MessageResponse.class);
    }

    @Step("Отмена покупки DELETE /carrinhos/cancelar-compra (raw)")
    public Response cancelPurchaseRaw() {
        return super.deleteEndpoint("/carrinhos/cancelar-compra");
    }
}

package com.serverest.controllers;

import com.serverest.BaseTest;
import com.serverest.client.CarrinhosClient;
import com.serverest.client.ProdutosClient;
import com.serverest.model.request.CarrinhoRequest;
import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.MessageResponse;
import com.serverest.util.ServeRestDataFactory;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("E2E")
class E2eControllerTest extends BaseTest {

    private static ProdutosClient adminProdutosClient;
    private static String adminId;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdProductIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        UsuarioRequest adminRequest = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse adminCreated = usuariosClient.create(adminRequest);
        adminId = adminCreated.getId();
        String token = authClient.loginAndGetToken(adminRequest.getEmail(), adminRequest.getPassword());

        adminProdutosClient = new ProdutosClient(token);
    }

    @AfterEach
    void cleanUp() {
        for (String id : createdProductIds) {
            adminProdutosClient.deleteRaw(id);
        }

        for (String id : createdUserIds) {
            usuariosClient.deleteRaw(id);
        }
    }

    @AfterAll
    static void tearDown() {
        usuariosClient.deleteRaw(adminId);
    }


    /*Первый запуск: JVM холодная — запросы медленные, оба треда реально
    одновременно долетают до сервера, тест находит баг (201+201)
    Повторные: JVM горячая, код летит — один тред успевает раньше,
    сервер обрабатывает последовательно, тест маскирует баг (201+400)*/
    @Test
    @Tag("E2E-03")
    @DisplayName("E2E-03: Overselling — два пользователя одновременно покупают последний товар")
    @Severity(SeverityLevel.CRITICAL)
    void shouldPreventParallelOverselling() throws Exception {
        ProdutoRequest product = ServeRestDataFactory.defaultProduto()
                .quantidade(1)
                .build();

        String productId = adminProdutosClient.create(product).getId();
        createdProductIds.add(productId);

        UsuarioRequest userA = ServeRestDataFactory.defaultUsuario().build();
        createdUserIds.add(usuariosClient.create(userA).getId());

        UsuarioRequest userB = ServeRestDataFactory.defaultUsuario().build();
        createdUserIds.add(usuariosClient.create(userB).getId());

        CarrinhosClient clientA = new CarrinhosClient(
                authClient.loginAndGetToken(userA.getEmail(), userA.getPassword()));

        CarrinhosClient clientB = new CarrinhosClient(
                authClient.loginAndGetToken(userB.getEmail(), userB.getPassword()));

        CarrinhoRequest cart = ServeRestDataFactory.buildCarrinho(productId, 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(3);

        try {
            Future<Response> futureA = executor.submit(() -> {
                barrier.await();
                return clientA.createRaw(cart);
            });

            Future<Response> futureB = executor.submit(() -> {
                barrier.await();
                return clientB.createRaw(cart);
            });

            barrier.await();

            Response responseA = futureA.get();
            Response responseB = futureB.get();

            List<Response> responses = List.of(responseA, responseB);

            assertThat(responses)
                    .as("Ровно один пользователь должен купить товар, второй — получить отказ")
                    .extracting(Response::statusCode)
                    .containsExactlyInAnyOrder(201, 400);
        } finally {
            executor.shutdown();
            clientA.cancelPurchaseRaw();
            clientB.cancelPurchaseRaw();
        }
    }


    @Test
    @Tag("E2E-04")
    @DisplayName("E2E-04: Жизненный цикл: создание корзины -> покупка -> удаление пользователя")
    @Severity(SeverityLevel.NORMAL)
    void shouldDeleteUserAfterCompletePurchase() {
        UsuarioRequest userRequest = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse userCreated = usuariosClient.create(userRequest);
        String userId = userCreated.getId();
        createdUserIds.add(userId);
        String userToken = authClient.loginAndGetToken(userRequest.getEmail(), userRequest.getPassword());

        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(10)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        CarrinhosClient userCarrinhosClient = new CarrinhosClient(userToken);
        CarrinhoRequest cartRequest = ServeRestDataFactory.buildCarrinho(productId, 1);
        userCarrinhosClient.create(cartRequest);

        userCarrinhosClient.completePurchase();

        Response deleteResponse = usuariosClient.deleteRaw(userId);
        assertThat(deleteResponse.getStatusCode())
                .as("После покупки пользователь должен успешно удаляться")
                .isEqualTo(HttpStatus.SC_OK);
        createdUserIds.remove(userId);
    }
}

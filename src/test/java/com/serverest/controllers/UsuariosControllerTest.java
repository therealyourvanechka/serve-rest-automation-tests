package com.serverest.controllers;

import com.serverest.BaseTest;
import com.serverest.client.CarrinhosClient;
import com.serverest.client.ProdutosClient;
import com.serverest.model.request.CarrinhoRequest;
import com.serverest.model.request.ProdutoCarrinhoRequest;
import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.MessageResponse;
import com.serverest.model.response.UsuarioResponse;
import com.serverest.util.ServeRestDataFactory;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Feature("Usuarios")
class UsuariosControllerTest extends BaseTest {

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdIds.forEach(id -> usuariosClient.deleteRaw(id));
    }

    @Test
    @Tag("US-01")
    @DisplayName("US-01: Создание пользователя с уникальным email")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCreateUserSuccessfully() {
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse createResponse = usuariosClient.create(request);
        String newUserId = createResponse.getId();
        createdIds.add(newUserId);

        UsuarioResponse getResponse = usuariosClient.getById(newUserId);
        assertSoftly(softly -> {
            softly.assertThat(getResponse.getNome()).isEqualTo(request.getNome());
            softly.assertThat(getResponse.getEmail()).isEqualTo(request.getEmail());
            softly.assertThat(getResponse.getAdministrador()).isEqualTo(request.getAdministrador());
        });
    }

    @Test
    @Tag("US-02")
    @DisplayName("US-02: Дубликат email запрещён")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDuplicateEmail() {
        UsuarioRequest firstUser = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse firstUserResponse = usuariosClient.create(firstUser);
        createdIds.add(firstUserResponse.getId());

        UsuarioRequest duplicateRequest = ServeRestDataFactory.defaultUsuario()
                .email(firstUser.getEmail())
                .build();

        Response duplicateResponse = usuariosClient.createRaw(duplicateRequest);
        assertThat(duplicateResponse.getStatusCode())
                .as("Дубликат email должен возвращать 400")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(duplicateResponse.jsonPath().getString("message"))
                .as("Сообщение об ошибке должно содержать 'já está sendo usado'")
                .contains("já está sendo usado");
    }

    @Test
    @Tag("US-03")
    @DisplayName("US-03: Создание без обязательного поля email")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForMissingEmail() {
        UsuarioRequest request = UsuarioRequest.builder()
                .nome(ServeRestDataFactory.randomNome())
                .password(ServeRestDataFactory.randomPassword())
                .administrador("false")
                .build();

        Response response = usuariosClient.createRaw(request);
        assertThat(response.getStatusCode())
                .as("Создание пользователя без email должно возвращать 400")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("US-04")
    @DisplayName("US-04: Создание с administrador=yes (невалидное значение)")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForInvalidAdministradorValue() {
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario()
                .administrador("yes")
                .build();

        Response response = usuariosClient.createRaw(request);
        assertThat(response.getStatusCode())
                .as("Недопустимое значение administrador должно возвращать 400")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("US-05")
    @DisplayName("US-05: Удаление пользователя с активной корзиной запрещено")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDeleteUserWithActiveCart() {
        UsuarioRequest adminRequest = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse adminResponse = usuariosClient.create(adminRequest);
        String adminId = adminResponse.getId();
        createdIds.add(adminId);

        String adminToken = authClient.loginAndGetToken(
                adminRequest.getEmail(), adminRequest.getPassword());
        ProdutosClient adminProdutosClient = new ProdutosClient(adminToken);

        ProdutoRequest produtoRequest = ServeRestDataFactory.defaultProduto().build();
        MessageResponse produtoResponse = adminProdutosClient.create(produtoRequest);
        String produtoId = produtoResponse.getId();

        CarrinhosClient adminCarrinhosClient = new CarrinhosClient(adminToken);

        CarrinhoRequest carrinhoRequest = ServeRestDataFactory.buildCarrinho(produtoId, 1);
        adminCarrinhosClient.create(carrinhoRequest);

        try {
            Response deleteResponse = usuariosClient.deleteRaw(adminId);
            assertThat(deleteResponse.getStatusCode())
                    .as("Удаление пользователя с активной корзиной должно возвращать 400")
                    .isEqualTo(HttpStatus.SC_BAD_REQUEST);
            assertThat(deleteResponse.jsonPath().getString("message"))
                    .as("Сообщение об ошибке должно содержать 'carrinho'")
                    .contains("carrinho");

            UsuarioResponse getResponse = usuariosClient.getById(adminId);
            assertThat(getResponse.getEmail())
                    .as("Пользователь не должен быть удалён")
                    .isEqualTo(adminRequest.getEmail());
        } finally {
            adminCarrinhosClient.cancelPurchase();
            adminProdutosClient.delete(produtoId);
        }
    }

    @Test
    @Tag("US-06")
    @DisplayName("US-06: Удаление пользователя без корзины")
    @Severity(SeverityLevel.CRITICAL)
    void shouldDeleteUserWithoutCartSuccessfully() {
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse createResponse = usuariosClient.create(request);
        String newUserId = createResponse.getId();
        createdIds.add(newUserId);

        usuariosClient.delete(newUserId);
        createdIds.remove(newUserId);

        Response getResponse = usuariosClient.getByIdRaw(newUserId);
        assertThat(getResponse.getStatusCode())
                .as("Удалённый пользователь должен быть недоступен")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("US-07")
    @DisplayName("US-07: PUT на несуществующий ID создаёт нового пользователя")
    @Severity(SeverityLevel.NORMAL)
    void shouldCreateNewUserOnPutWithNonExistentId() {
        String nonExistentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario().build();

        Response updateResponse = usuariosClient.updateRaw(nonExistentId, request);
        assertThat(updateResponse.getStatusCode())
                .as("PUT на несуществующий ID должен создавать пользователя (201)")
                .isEqualTo(HttpStatus.SC_CREATED);

        String createdId = updateResponse.jsonPath().getString("_id");
        createdIds.add(createdId);

        UsuarioResponse getResponse = usuariosClient.getById(createdId);
        assertThat(getResponse.getEmail())
                .as("Email созданного пользователя должен совпадать с переданным")
                .isEqualTo(request.getEmail());
    }

    @Test
    @Tag("US-08")
    @DisplayName("US-08: PUT с email, занятым другим пользователем")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForPutWithTakenEmail() {
        UsuarioRequest userARequest = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse userAResponse = usuariosClient.create(userARequest);
        createdIds.add(userAResponse.getId());

        UsuarioRequest userBRequest = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse userBResponse = usuariosClient.create(userBRequest);
        String userBId = userBResponse.getId();

        createdIds.add(userBId);

        UsuarioRequest updateBRequest = ServeRestDataFactory.defaultUsuario()
                .email(userARequest.getEmail())
                .build();

        Response updateBResponse = usuariosClient.updateRaw(userBId, updateBRequest);
        assertThat(updateBResponse.getStatusCode())
                .as("PUT с чужим email должен возвращать 400")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(updateBResponse.jsonPath().getString("message"))
                .as("Сообщение об ошибке должно содержать 'já está sendo usado'")
                .contains("já está sendo usado");
    }

    @Test
    @Tag("US-09")
    @DisplayName("US-09: PUT на существующий ID обновляет пользователя")
    @Severity(SeverityLevel.CRITICAL)
    void shouldUpdateUserOnPutWithExistingId() {
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse response = usuariosClient.create(request);
        String userId = response.getId();
        createdIds.add(userId);

        UsuarioRequest updateRequest = ServeRestDataFactory.defaultAdmin().build();

        usuariosClient.update(userId, updateRequest);

        UsuarioResponse getResponse = usuariosClient.getById(userId);
        assertSoftly(softly -> {
            softly.assertThat(getResponse.getNome()).isEqualTo(updateRequest.getNome());
            softly.assertThat(getResponse.getEmail()).isEqualTo(updateRequest.getEmail());
            softly.assertThat(getResponse.getAdministrador()).isEqualTo(updateRequest.getAdministrador());
        });
    }

    @Test
    @Tag("US-10")
    @DisplayName("US-10: Фильтр по administrador=true")
    @Severity(SeverityLevel.MINOR)
    void shouldFilterByAdministradorTrue() {
        UsuarioRequest adminRequest = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse adminResponse = usuariosClient.create(adminRequest);
        createdIds.add(adminResponse.getId());

        Response response = usuariosClient.getAll(Map.of("administrador", "true"));
        assertThat(response.getStatusCode())
                .as("Запрос списка с фильтром должен возвращать 200")
                .isEqualTo(HttpStatus.SC_OK);

        List<String> adminValues = response.jsonPath().getList("usuarios.administrador");
        assertThat(adminValues)
                .as("Список администраторов не должен быть пустым")
                .isNotEmpty();
        assertThat(adminValues)
                .as("Все элементы должны быть администраторами (true)")
                .allMatch("true"::equals);
    }

    @Test
    @Tag("US-11")
    @DisplayName("US-11: Фильтр по nome (case-insensitive)")
    @Severity(SeverityLevel.MINOR)
    void shouldFilterByNomeCaseInsensitive() {
        UsuarioRequest request = ServeRestDataFactory.defaultUsuario()
                .nome("Ivan")
                .build();
        MessageResponse userResponse = usuariosClient.create(request);
        String userId = userResponse.getId();
        createdIds.add(userId);

        Response response = usuariosClient.getAll(Map.of("nome", "ivan"));
        assertThat(response.getStatusCode())
                .as("Запрос с фильтром по nome должен возвращать 200")
                .isEqualTo(HttpStatus.SC_OK);

        List<String> ids = response.jsonPath().getList("usuarios._id");
        assertThat(ids)
                .as("Созданный пользователь должен быть в результатах фильтра")
                .contains(userId);
    }
}
package com.serverest.controllers;

import com.serverest.BaseTest;
import com.serverest.client.CarrinhosClient;
import com.serverest.client.ProdutosClient;
import com.serverest.model.request.CarrinhoRequest;
import com.serverest.model.request.ProdutoCarrinhoRequest;
import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.MessageResponse;
import com.serverest.model.response.ProdutoResponse;
import com.serverest.util.JwtHelper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Feature("Produtos")
class ProdutosControllerTest extends BaseTest {

    private static ProdutosClient adminProdutosClient;
    private static ProdutosClient userProdutosClient;
    private static ProdutosClient noAuthProdutosClient;
    private static String adminEmail;
    private static String adminPassword;
    private static String adminToken;
    private static String adminId;
    private static String userId;

    private final List<String> createdProductIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        UsuarioRequest adminRequest = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse adminCreated = usuariosClient.create(adminRequest);
        adminId = adminCreated.getId();
        adminEmail = adminRequest.getEmail();
        adminPassword = adminRequest.getPassword();
        adminToken = authClient.loginAndGetToken(adminEmail, adminPassword);
        adminProdutosClient = new ProdutosClient(adminToken);

        UsuarioRequest userRequest = ServeRestDataFactory.defaultUsuario().build();
        MessageResponse userCreated = usuariosClient.create(userRequest);
        userId = userCreated.getId();
        String userToken = authClient.loginAndGetToken(userRequest.getEmail(), userRequest.getPassword());
        userProdutosClient = new ProdutosClient(userToken);

        noAuthProdutosClient = new ProdutosClient();
    }

    @AfterEach
    void cleanUpProducts() {
        for (String id : createdProductIds) {
            adminProdutosClient.deleteRaw(id);
        }
    }

    @AfterAll
    static void tearDown() {
        usuariosClient.deleteRaw(adminId);
        usuariosClient.deleteRaw(userId);
    }

    @Test
    @Tag("PR-01")
    @DisplayName("PR-01: Админ создаёт товар")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCreateProductByAdmin() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        ProdutoResponse product = adminProdutosClient.getById(createResponse.getId());
        assertSoftly(softly -> {
            softly.assertThat(product.getNome()).isEqualTo(request.getNome());
            softly.assertThat(product.getPreco()).isEqualTo(request.getPreco());
            softly.assertThat(product.getDescricao()).isEqualTo(request.getDescricao());
            softly.assertThat(product.getQuantidade()).isEqualTo(request.getQuantidade());
        });
    }

    @ParameterizedTest(name = "PR-02..03")
    @CsvSource({"-1", "0"})
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForInvalidPrecoOnCreate(int preco) {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .preco(preco)
                .build();
        Response response = adminProdutosClient.createRaw(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("PR-04")
    @DisplayName("PR-04: Создание с quantidade = -1")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForNegativeQuantidadeOnCreate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .quantidade(-1)
                .build();
        Response response = adminProdutosClient.createRaw(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("PR-05")
    @DisplayName("PR-05: Пользователь без прав админа не может создать товар")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn403ForNonAdminCreate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        Response response = userProdutosClient.createRaw(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @Tag("PR-06")
    @DisplayName("PR-06: Создание товара без токена")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForCreateWithoutToken() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        Response response = noAuthProdutosClient.createRaw(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-07")
    @DisplayName("PR-07: Создание товара с просроченным токеном")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForExpiredTokenCreate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        String expiredToken = JwtHelper.generateExpiredToken(adminEmail, adminPassword);
        ProdutosClient expiredClient = new ProdutosClient(expiredToken);
        Response response = expiredClient.createRaw(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-08")
    @DisplayName("PR-08: Дубликат названия товара запрещён")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDuplicateProductName() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .nome("UniqueName")
                .build();
        MessageResponse firstCreate = adminProdutosClient.create(request);
        createdProductIds.add(firstCreate.getId());

        Response secondCreate = adminProdutosClient.createRaw(request);
        assertThat(secondCreate.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(secondCreate.jsonPath().getString("message"))
                .contains("Já existe produto");
    }

    @Test
    @Tag("PR-09")
    @DisplayName("PR-09: Админ обновляет товар")
    @Severity(SeverityLevel.CRITICAL)
    void shouldUpdateProductByAdmin() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        String productId = createResponse.getId();
        createdProductIds.add(productId);

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto().build();
        adminProdutosClient.update(productId, updateRequest);

        ProdutoResponse product = adminProdutosClient.getById(productId);
        assertSoftly(softly -> {
            softly.assertThat(product.getNome()).isEqualTo(updateRequest.getNome());
            softly.assertThat(product.getPreco()).isEqualTo(updateRequest.getPreco());
            softly.assertThat(product.getDescricao()).isEqualTo(updateRequest.getDescricao());
            softly.assertThat(product.getQuantidade()).isEqualTo(updateRequest.getQuantidade());
        });
    }

    @ParameterizedTest(name = "PR-10..11")
    @CsvSource({"-1", "0"})
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForInvalidPrecoOnUpdate(int preco) {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse created = adminProdutosClient.create(request);
        createdProductIds.add(created.getId());

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto()
                .preco(preco)
                .build();
        Response response = adminProdutosClient.updateRaw(created.getId(), updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("PR-12")
    @DisplayName("PR-12: Обновление с quantidade = -1")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForNegativeQuantidadeOnUpdate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse created = adminProdutosClient.create(request);
        createdProductIds.add(created.getId());

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(-1)
                .build();
        Response response = adminProdutosClient.updateRaw(created.getId(), updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("PR-13")
    @DisplayName("PR-13: Обновление quantidade = 0 (товар закончился)")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn200ForQuantidadeZeroOnUpdate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        String productId = createResponse.getId();
        createdProductIds.add(productId);

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(0)
                .build();
        adminProdutosClient.update(productId, updateRequest);

        ProdutoResponse product = adminProdutosClient.getById(productId);
        assertThat(product.getQuantidade()).isZero();
    }

    @Test
    @Tag("PR-14")
    @DisplayName("PR-14: Обновление товара без токена")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForUpdateWithoutToken() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto().build();
        Response response = noAuthProdutosClient.updateRaw(createResponse.getId(), updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-15")
    @DisplayName("PR-15: Обновление товара с просроченным токеном")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForExpiredTokenUpdate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        String expiredToken = JwtHelper.generateExpiredToken(adminEmail, adminPassword);
        ProdutosClient expiredClient = new ProdutosClient(expiredToken);
        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto().build();
        Response response = expiredClient.updateRaw(createResponse.getId(), updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-16")
    @DisplayName("PR-16: Обновление товара без прав админа")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn403ForNonAdminUpdate() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        ProdutoRequest updateRequest = ServeRestDataFactory.defaultProduto().build();
        Response response = userProdutosClient.updateRaw(createResponse.getId(), updateRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @Tag("PR-17")
    @DisplayName("PR-17: PUT на несуществующий ID создаёт новый товар")
    @Severity(SeverityLevel.NORMAL)
    void shouldCreateNewProductOnPutWithNonExistentId() {
        String nonExistentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();

        Response updateResponse = adminProdutosClient.updateRaw(nonExistentId, request);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

        String createdId = updateResponse.jsonPath().getString("_id");
        createdProductIds.add(createdId);

        ProdutoResponse product = adminProdutosClient.getById(createdId);
        assertThat(product.getNome()).isEqualTo(request.getNome());
    }

    @Test
    @Tag("PR-18")
    @DisplayName("PR-18: Нельзя удалить товар в активной корзине")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDeleteProductInActiveCart() {
        CarrinhosClient adminCarrinhosClient = new CarrinhosClient(adminToken);

        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto().build();
        MessageResponse productResponse = adminProdutosClient.create(productRequest);
        String productId = productResponse.getId();

        CarrinhoRequest cartRequest = ServeRestDataFactory.buildCarrinho(productId, 1);
        adminCarrinhosClient.create(cartRequest);

        try {
            Response deleteResponse = adminProdutosClient.deleteRaw(productId);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            assertThat(deleteResponse.jsonPath().getString("message"))
                    .contains("carrinho");

            ProdutoResponse product = adminProdutosClient.getById(productId);
            assertThat(product.getNome()).isEqualTo(productRequest.getNome());
        } finally {
            adminCarrinhosClient.cancelPurchase();
            adminProdutosClient.deleteRaw(productId);
        }
    }

    @Test
    @Tag("PR-19")
    @DisplayName("PR-19: Удаление товара без корзины")
    @Severity(SeverityLevel.CRITICAL)
    void shouldDeleteProductNotInCart() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        String productId = createResponse.getId();

        adminProdutosClient.delete(productId);

        Response getResponse = adminProdutosClient.getByIdRaw(productId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("PR-20")
    @DisplayName("PR-20: Удаление товара без прав админа")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn403ForNonAdminDelete() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        Response response = userProdutosClient.deleteRaw(createResponse.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @Tag("PR-21")
    @DisplayName("PR-21: Удаление товара без токена")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForDeleteWithoutToken() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        Response response = noAuthProdutosClient.deleteRaw(createResponse.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-22")
    @DisplayName("PR-22: Удаление товара с просроченным токеном")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn401ForExpiredTokenDelete() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto().build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        String expiredToken = JwtHelper.generateExpiredToken(adminEmail, adminPassword);
        ProdutosClient expiredClient = new ProdutosClient(expiredToken);
        Response response = expiredClient.deleteRaw(createResponse.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Tag("PR-23")
    @DisplayName("PR-23: Фильтрация товаров по preco")
    @Severity(SeverityLevel.MINOR)
    void shouldFilterByPreco() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .preco(300)
                .build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        Response response = adminProdutosClient.getAll(Map.of("preco", 300));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        List<String> ids = response.jsonPath().getList("produtos._id");
        assertThat(ids).contains(createResponse.getId());
    }

    @Test
    @Tag("PR-24")
    @DisplayName("PR-24: Фильтрация товаров по quantidade")
    @Severity(SeverityLevel.MINOR)
    void shouldFilterByQuantidade() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .quantidade(0)
                .build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        Response response = adminProdutosClient.getAll(Map.of("quantidade", 0));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        List<String> ids = response.jsonPath().getList("produtos._id");
        assertThat(ids).contains(createResponse.getId());
    }

    @Test
    @Tag("PR-25")
    @DisplayName("PR-25: Фильтр по nome (case-insensitive)")
    @Severity(SeverityLevel.MINOR)
    void shouldFilterByNomeCaseInsensitive() {
        ProdutoRequest request = ServeRestDataFactory.defaultProduto()
                .nome("MacBook")
                .build();
        MessageResponse createResponse = adminProdutosClient.create(request);
        createdProductIds.add(createResponse.getId());

        Response response = adminProdutosClient.getAll(Map.of("nome", "macbook"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        List<String> ids = response.jsonPath().getList("produtos._id");
        assertThat(ids).contains(createResponse.getId());
    }

}

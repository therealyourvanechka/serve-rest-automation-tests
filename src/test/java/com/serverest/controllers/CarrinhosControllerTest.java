package com.serverest.controllers;

import com.serverest.BaseTest;
import com.serverest.client.CarrinhosClient;
import com.serverest.client.ProdutosClient;
import com.serverest.model.request.CarrinhoRequest;
import com.serverest.model.request.ProdutoCarrinhoRequest;
import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.request.UsuarioRequest;
import com.serverest.model.response.CarrinhoResponse;
import com.serverest.model.response.MessageResponse;
import com.serverest.model.response.ProdutoResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Carrinhos")
class CarrinhosControllerTest extends BaseTest {

    private static CarrinhosClient adminCarrinhosClient;
    private static ProdutosClient adminProdutosClient;
    private static String adminId;

    private final List<String> createdProductIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        UsuarioRequest adminRequest = ServeRestDataFactory.defaultAdmin().build();
        MessageResponse adminCreated = usuariosClient.create(adminRequest);
        adminId = adminCreated.getId();
        String email = adminRequest.getEmail();
        String password = adminRequest.getPassword();
        String token = authClient.loginAndGetToken(email, password);

        adminCarrinhosClient = new CarrinhosClient(token);
        adminProdutosClient = new ProdutosClient(token);
    }

    @AfterEach
    void cleanUp() {
        adminCarrinhosClient.cancelPurchaseRaw();
        for (String id : createdProductIds) {
            adminProdutosClient.deleteRaw(id);
        }
    }

    @AfterAll
    static void tearDown() {
        usuariosClient.deleteRaw(adminId);
    }

    @ParameterizedTest(name = "CR-BVA-01,02,06")
    @CsvSource({"-1", "0", "11"})
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForInvalidQuantidade(int quantidade) {
        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(10)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(quantidade)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        Response response = adminCarrinhosClient.createRaw(cartRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest(name = "CR-BVA-03,04,05")
    @CsvSource({"1", "9", "10"})
    @Severity(SeverityLevel.CRITICAL)
    void shouldCreateCartWithValidQuantidade(int quantidade) {
        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(10)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(quantidade)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        Response response = adminCarrinhosClient.createRaw(cartRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
    }

    @Test
    @Tag("CR-01")
    @DisplayName("CR-01: Создание корзины — остаток уменьшается")
    @Severity(SeverityLevel.CRITICAL)
    void shouldDecreaseStockAfterCartCreation() {
        int initialStock = 10;
        int cartQuantidade = 3;

        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(initialStock)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(cartQuantidade)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();
        adminCarrinhosClient.create(cartRequest);

        ProdutoResponse product = adminProdutosClient.getById(productId);
        assertThat(product.getQuantidade()).isEqualTo(initialStock - cartQuantidade);
    }

    @Test
    @Tag("CR-02")
    @DisplayName("CR-02: Нельзя создать вторую корзину, пока первая активна")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDuplicateCart() {
        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(10)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(1)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        adminCarrinhosClient.create(cartRequest);

        Response secondResponse = adminCarrinhosClient.createRaw(cartRequest);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("CR-03")
    @DisplayName("CR-03: Создание корзины с несуществующим idProduto")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForNonExistentProduct() {
        String fakeId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(fakeId)
                .quantidade(1)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        Response response = adminCarrinhosClient.createRaw(cartRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("CR-04")
    @DisplayName("CR-04: Дубликат idProduto внутри одного запроса")
    @Severity(SeverityLevel.NORMAL)
    void shouldReturn400ForDuplicateProductInCart() {
        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto().build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest p1 = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(1)
                .build();

        ProdutoCarrinhoRequest p2 = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(2)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(p1, p2))
                .build();

        Response response = adminCarrinhosClient.createRaw(cartRequest);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Tag("CR-05")
    @DisplayName("CR-05: concluir-compra — товар остаётся списанным")
    @Severity(SeverityLevel.CRITICAL)
    void shouldKeepStockDecreasedAfterConcludePurchase() {
        int initialStock = 10;
        int cartQuantidade = 3;

        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(initialStock)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(cartQuantidade)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        adminCarrinhosClient.create(cartRequest);

        adminCarrinhosClient.completePurchase();

        ProdutoResponse afterPurchase = adminProdutosClient.getById(productId);
        assertThat(afterPurchase.getQuantidade()).isEqualTo(initialStock - cartQuantidade);
    }

    @Test
    @Tag("CR-06")
    @DisplayName("CR-06: cancelar-compra — товар возвращается на склад")
    @Severity(SeverityLevel.CRITICAL)
    void shouldRestoreStockAfterCancelPurchase() {
        int initialStock = 10;
        int cartQuantidade = 3;

        ProdutoRequest productRequest = ServeRestDataFactory.defaultProduto()
                .quantidade(initialStock)
                .build();
        MessageResponse productCreated = adminProdutosClient.create(productRequest);
        String productId = productCreated.getId();
        createdProductIds.add(productId);

        ProdutoCarrinhoRequest produtoCarrinho = ProdutoCarrinhoRequest.builder()
                .idProduto(productId)
                .quantidade(cartQuantidade)
                .build();

        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(produtoCarrinho))
                .build();

        adminCarrinhosClient.create(cartRequest);

        adminCarrinhosClient.cancelPurchase();

        ProdutoResponse afterCancel = adminProdutosClient.getById(productId);
        assertThat(afterCancel.getQuantidade()).isEqualTo(initialStock);
    }

    @Test
    @Tag("CR-07")
    @DisplayName("CR-07: Проверка precoTotal")
    @Severity(SeverityLevel.NORMAL)
    void shouldCalculatePrecoTotalCorrectly() {
        int precoA = 100;
        int precoB = 200;
        int quantidadeA = 2;
        int quantidadeB = 3;

        ProdutoRequest productA = ServeRestDataFactory.defaultProduto()
                .preco(precoA)
                .build();
        MessageResponse productACreated = adminProdutosClient.create(productA);
        createdProductIds.add(productACreated.getId());

        ProdutoRequest productB = ServeRestDataFactory.defaultProduto()
                .preco(precoB)
                .build();
        MessageResponse productBCreated = adminProdutosClient.create(productB);
        createdProductIds.add(productBCreated.getId());

        ProdutoCarrinhoRequest pA = ProdutoCarrinhoRequest.builder()
                .idProduto(productACreated.getId())
                .quantidade(quantidadeA)
                .build();
        ProdutoCarrinhoRequest pB = ProdutoCarrinhoRequest.builder()
                .idProduto(productBCreated.getId())
                .quantidade(quantidadeB)
                .build();
        CarrinhoRequest cartRequest = CarrinhoRequest.builder()
                .produtos(List.of(pA, pB))
                .build();
        MessageResponse cartCreated = adminCarrinhosClient.create(cartRequest);

        CarrinhoResponse cart = adminCarrinhosClient.getById(cartCreated.getId());
        assertThat(cart.getPrecoTotal()).isEqualTo(precoA * quantidadeA + precoB * quantidadeB);
    }

    @Test
    @Tag("CR-08")
    @DisplayName("CR-08: concluir-compra без корзины")
    @Severity(SeverityLevel.MINOR)
    void shouldCompletePurchaseWithoutCart() {
        MessageResponse response = adminCarrinhosClient.completePurchase();
        assertThat(response.getMessage()).contains("Não foi encontrado");
    }

    @Test
    @Tag("CR-09")
    @DisplayName("CR-09: cancelar-compra без корзины")
    @Severity(SeverityLevel.MINOR)
    void shouldCancelPurchaseWithoutCart() {
        MessageResponse response = adminCarrinhosClient.cancelPurchase();
        assertThat(response.getMessage()).contains("Não foi encontrado");
    }
}

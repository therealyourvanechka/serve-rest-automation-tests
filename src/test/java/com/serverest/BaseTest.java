package com.serverest;

import com.serverest.client.AuthClient;
import com.serverest.client.CarrinhosClient;
import com.serverest.client.ProdutosClient;
import com.serverest.client.UsuariosClient;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {
    private static boolean frameworkReady = false;

    protected static AuthClient authClient;
    protected static UsuariosClient usuariosClient;
    protected static ProdutosClient produtosClient;
    protected static CarrinhosClient carrinhosClient;

    @BeforeAll
    static void setUpFramework() {
        if (frameworkReady) return;
        frameworkReady = true;

        RestAssured.filters(new AllureRestAssured());

        authClient = new AuthClient();
        usuariosClient = new UsuariosClient();
    }
}

package com.serverest.client;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class BaseClient {
    private static final String BASE_URI = "https://serverest.dev";

    protected final RequestSpecification requestSpec;

    public BaseClient() {
        requestSpec = buildBaseSpec();
    }

    public BaseClient(String token) {
        requestSpec = buildBaseSpec()
                .header("Authorization", "Bearer " + token);
    }

    private static RequestSpecification buildBaseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setContentType(ContentType.JSON)
                .addHeader("Accept", "application/json")
                .build();
    }

    protected Response post(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response get(String endpoint) {
        return given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response get(String endpoint, Map<String, Object> queryParams) {
        return given()
                .spec(requestSpec)
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response getById(String endpoint, Object id) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .when()
                .get(endpoint + "/{id}")
                .then()
                .extract()
                .response();
    }

    protected Response put(String endpoint, Object id, Object body) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .body(body)
                .when()
                .put(endpoint + "/{id}")
                .then()
                .extract()
                .response();
    }

    protected Response deleteEndpoint(String endpoint) {
        return given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }

    protected Response delete(String endpoint, Object id) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .when()
                .delete(endpoint + "/{id}")
                .then()
                .extract()
                .response();
    }
}

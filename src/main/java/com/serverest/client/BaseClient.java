package com.serverest.client;

import com.serverest.util.Specifications;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class BaseClient {
    protected final RequestSpecification requestSpec;

    public BaseClient() {
        requestSpec = Specifications.getRequestSpec();
    }

    public BaseClient(String token) {
        requestSpec = Specifications.getRequestSpec()
                .header("Authorization", token);
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

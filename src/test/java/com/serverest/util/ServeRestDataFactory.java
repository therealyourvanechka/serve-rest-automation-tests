package com.serverest.util;

import com.serverest.model.request.ProdutoRequest;
import com.serverest.model.request.UsuarioRequest;
import net.datafaker.Faker;

import java.util.Locale;

public class ServeRestDataFactory {
    private static final Faker faker = new Faker(Locale.ENGLISH);

    public static UsuarioRequest.UsuarioRequestBuilder defaultUsuario() {
        return UsuarioRequest.builder()
                .nome(randomNome())
                .email(randomEmail())
                .password(randomPassword())
                .administrador("false");
    }

    public static UsuarioRequest.UsuarioRequestBuilder defaultAdmin() {
        return defaultUsuario().administrador("true");
    }

    public static ProdutoRequest.ProdutoRequestBuilder defaultProduto() {
        return ProdutoRequest.builder()
                .nome(faker.commerce().productName())
                .preco(randomPreco())
                .descricao(faker.lorem().sentence(3))
                .quantidade(randomQuantidade());
    }

    public static String randomNome() {
        return faker.name().fullName();
    }

    public static String randomEmail() {
        return faker.internet().emailAddress();
    }

    public static String randomPassword() {
        return faker.internet().password(6, 12);
    }

    public static Integer randomPreco() {
        return faker.number().numberBetween(50, 5001);
    }

    public static Integer randomQuantidade() {
        return faker.number().numberBetween(1, 101);
    }
}

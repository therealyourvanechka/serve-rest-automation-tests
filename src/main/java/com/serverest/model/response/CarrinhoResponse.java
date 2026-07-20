package com.serverest.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrinhoResponse {
    @JsonProperty("_id")
    private String id;
    private String idUsuario;
    private Integer quantidadeTotal;
    private Double precoTotal;
    private List<ProdutoCarrinhoResponse> produtos;
}

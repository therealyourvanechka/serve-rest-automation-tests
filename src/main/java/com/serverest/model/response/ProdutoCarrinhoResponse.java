package com.serverest.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoCarrinhoResponse {
    private String idProduto;
    private Integer quantidade;
    private Double precoUnitario;
}

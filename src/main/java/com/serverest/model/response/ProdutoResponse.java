package com.serverest.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponse {
    @JsonProperty("_id")
    private String id;
    private String nome;
    private Integer preco;
    private String descricao;
    private Integer quantidade;
}

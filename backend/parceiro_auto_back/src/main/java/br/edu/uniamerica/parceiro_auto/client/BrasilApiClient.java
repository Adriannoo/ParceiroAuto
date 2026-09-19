package br.edu.uniamerica.parceiro_auto.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// O Feign cria a chamada HTTP a partir desta interface e converte o JSON em DTO.
@FeignClient(name = "brasilApi", url = "${integrations.brasil-api.url}")
public interface BrasilApiClient {

    @GetMapping("/api/cnpj/v1/{cnpj}")
    BrasilApiCnpjResponse findByCnpj(@PathVariable("cnpj") String cnpj);
}

package br.edu.uniamerica.parceiro_auto.client;

// Os nomes acompanham o JSON da BrasilAPI, sem alterar o contrato da nossa API.
public record BrasilApiCnpjResponse(
        String cnpj,
        String razao_social,
        String nome_fantasia,
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String ddd_telefone_1,
        String email
) {
}

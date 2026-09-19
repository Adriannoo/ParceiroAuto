package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;

// Usa um servidor local para testar o Feign sem depender da disponibilidade da BrasilAPI.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:brasil-api",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none", "spring.flyway.enabled=false",
        "spring.cloud.openfeign.client.config.brasilApi.readTimeout=200"
})
class BrasilApiIntegrationTest {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final HttpServer server = startServer();
    private static volatile int status = 200;
    private static volatile String body = "{}";
    private static volatile boolean slow;
    private static volatile String requestedPath;
    @Autowired CompanyService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("integrations.brasil-api.url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
    }

    private static HttpServer startServer() {
        try {
            HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            http.setExecutor(executor);
            http.createContext("/api/cnpj/v1/", exchange -> {
                requestedPath = exchange.getRequestURI().getPath();
                int responseStatus = status;
                byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
                try {
                    if (slow) Thread.sleep(800);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(responseStatus, responseBody.length);
                    exchange.getResponseBody().write(responseBody);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            http.start();
            return http;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void mapsExternalResponseAndNormalizesCnpj() {
        status = 200;
        slow = false;
        body = """
                {"cnpj":"11222333000181","razao_social":"Empresa Teste","nome_fantasia":"Teste",
                 "cep":"85800000","logradouro":"Rua A","numero":"10","bairro":"Centro",
                 "municipio":"Foz","uf":"PR","ddd_telefone_1":"45999999999","email":"teste@example.com"}
                """;
        var response = service.lookupByCnpj("11.222.333/0001-81");
        assertThat(response.cnpj()).isEqualTo("11222333000181");
        assertThat(requestedPath).isEqualTo("/api/cnpj/v1/11222333000181");
        assertThat(response.legalName()).isEqualTo("Empresa Teste");
    }

    @Test
    void translatesExternalNotFound() {
        expectFailure(404, "{}", false, 404);
    }

    @Test
    void translatesExternalServerFailure() {
        expectFailure(500, "{}", false, 502);
    }

    @Test
    void translatesTimeout() {
        expectFailure(200, "{}", true, 503);
    }

    @Test
    void rejectsEmptyResponse() {
        expectFailure(200, "null", false, 502);
    }

    private void expectFailure(int responseStatus, String responseBody, boolean delay, int expectedStatus) {
        status = responseStatus;
        body = responseBody;
        slow = delay;
        assertThatThrownBy(() -> service.lookupByCnpj("11222333000181"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(expectedStatus));
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
        executor.close();
    }
}

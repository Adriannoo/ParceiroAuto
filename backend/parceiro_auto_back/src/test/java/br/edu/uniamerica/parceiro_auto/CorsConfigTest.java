package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.config.CorsConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsConfigTest {
    // Verifica as origens e os metodos sem iniciar servidor HTTP.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "OPTIONS,https://outro.example,200,false",
            "GET,https://outro.example,200,true", "HEAD,https://outro.example,200,true",
            "POST,NULL,200,true", "POST,http://localhost:4200,200,true",
            "POST,http://localhost:8080,200,true", "POST,https://outro.example,403,false"
    })
    void allowsExpectedRequestsAndBlocksWritesFromOtherOrigins(String method, String origin, int status, boolean continues) throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/users");
        request.addHeader("Host", "localhost:8080");
        if (origin != null) request.addHeader("Origin", origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        new CorsConfig().corsFilter().doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:4200");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        verify(chain, times(continues ? 1 : 0)).doFilter(request, response);
    }
}

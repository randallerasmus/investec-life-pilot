package za.co.byteservices.moneycoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsLocalLifePilotFrontendOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/api/lifepilot/scenarios"
        );
        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://localhost:8081"))
                .isEqualTo("http://localhost:8081");
        assertThat(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST)).isNotNull();
        assertThat(configuration.checkHeaders(java.util.List.of("content-type")))
                .containsExactly("content-type");
    }
}

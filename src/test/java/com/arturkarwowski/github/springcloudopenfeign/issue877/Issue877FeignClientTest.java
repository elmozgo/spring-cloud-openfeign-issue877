package com.arturkarwowski.github.springcloudopenfeign.issue877;

import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                HttpMessageConvertersAutoConfiguration.class,
                FeignAutoConfiguration.class,
                FeignClientsConfiguration.class
        },
        properties = {
                "issue877-feign.url=http://localhost:${wiremock.server.port}/",
                "logging.level.WireMock=WARN"
        })
@AutoConfigureWireMock(port = 0)
@Import(Issue877FeignClientTest.TestConfig.class)
class Issue877FeignClientTest {

    @BeforeEach
    void resetWiremock() {
        reset();
    }

    @Autowired
    Issue877FeignClient issue877FeignClient;

    private static final String RESPONSE = """
            {
             "value": "hello test"
            }
            """;

    @Test
    void shouldDecode() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .willReturn(okJson(RESPONSE)));

        var response = issue877FeignClient.get();
        assertThat(response).isPresent();
        assertThat(response.get().value()).isEqualTo("hello test");
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    @Test
    void shouldDecode404() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .willReturn(aResponse().withStatus(404)));

        var response = issue877FeignClient.get();
        assertThat(response).isEmpty();
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    @Test
    void shouldRetryConnectionResetByPeer() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("failed once"));
        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .whenScenarioStateIs("failed once")
                .willReturn(okJson(RESPONSE)));

        var response = issue877FeignClient.get();
        assertThat(response).isPresent();
        assertThat(response.get().value()).isEqualTo("hello test");
        verify(exactly(2), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    // this fails
    @Test
    void shouldRetryRandomDataThenClose() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
                .willSetStateTo("failed once"));
        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .whenScenarioStateIs("failed once")
                .willReturn(okJson(RESPONSE)));

        var response = issue877FeignClient.get();
        assertThat(response).isPresent();
        assertThat(response.get().value()).isEqualTo("hello test");
        verify(exactly(2), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    @Test
    void shouldRetryEmptyResponse() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo("failed once"));
        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .whenScenarioStateIs("failed once")
                .willReturn(okJson(RESPONSE)));

        var response = issue877FeignClient.get();
        assertThat(response).isPresent();
        assertThat(response.get().value()).isEqualTo("hello test");
        verify(exactly(2), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    // this fails
    @Test
    void shouldRetryMalformedResponseChunk() {

        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
                .willSetStateTo("failed once"));
        stubFor(get(urlPathEqualTo("/test-url"))
                .inScenario("retry test")
                .whenScenarioStateIs("failed once")
                .willReturn(okJson(RESPONSE)));

        var response = issue877FeignClient.get();
        assertThat(response).isPresent();
        assertThat(response.get().value()).isEqualTo("hello test");
        verify(exactly(2), getRequestedFor(urlPathEqualTo("/test-url")));
    }

    @EnableFeignClients
    static class TestConfig {
    }
}
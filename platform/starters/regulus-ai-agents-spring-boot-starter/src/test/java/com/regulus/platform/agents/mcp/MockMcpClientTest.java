package com.regulus.platform.agents.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Mock MCP Client")
class MockMcpClientTest {

    private MockMcpClient client;

    @BeforeEach
    void setUp() {
        client = new MockMcpClient("http://localhost:8080/mcp");
    }

    @Nested
    @DisplayName("connection management")
    class ConnectionManagement {

        @Test
        @DisplayName("should start disconnected")
        void shouldStartDisconnected() {
            assertThat(client.isConnected()).isFalse();
        }

        @Test
        @DisplayName("should connect successfully")
        void shouldConnect() throws Exception {
            client.connect().get();

            assertThat(client.isConnected()).isTrue();
        }

        @Test
        @DisplayName("should disconnect successfully")
        void shouldDisconnect() throws Exception {
            client.connect().get();
            client.disconnect().get();

            assertThat(client.isConnected()).isFalse();
        }

        @Test
        @DisplayName("should return server URL")
        void shouldReturnServerUrl() {
            assertThat(client.getServerUrl()).isEqualTo("http://localhost:8080/mcp");
        }
    }

    @Nested
    @DisplayName("tool discovery")
    class ToolDiscovery {

        @Test
        @DisplayName("should fail when not connected")
        void shouldFailWhenNotConnected() {
            assertThatThrownBy(() -> client.discoverTools().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should discover default tools when connected")
        void shouldDiscoverDefaultTools() throws Exception {
            client.connect().get();

            List<McpTool> tools = client.discoverTools().get();

            assertThat(tools).isNotEmpty();
            assertThat(tools).anyMatch(t -> t.name().equals("iso20022_validate"));
            assertThat(tools).anyMatch(t -> t.name().equals("risk_score"));
            assertThat(tools).anyMatch(t -> t.name().equals("customer_lookup"));
        }

        @Test
        @DisplayName("should return tool with schema")
        void shouldReturnToolSchema() throws Exception {
            client.connect().get();

            List<McpTool> tools = client.discoverTools().get();
            McpTool isoTool = tools.stream()
                .filter(t -> t.name().equals("iso20022_validate"))
                .findFirst()
                .orElseThrow();

            assertThat(isoTool.description()).contains("ISO 20022");
            assertThat(isoTool.inputSchema()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("tool invocation")
    class ToolInvocation {

        @BeforeEach
        void connect() throws Exception {
            client.connect().get();
        }

        @Test
        @DisplayName("should invoke ISO 20022 validator")
        void shouldInvokeIso20022Validator() throws Exception {
            McpToolResponse response = client.invoke("iso20022_validate", Map.of(
                "message", "<pain.001>...</pain.001>",
                "messageType", "pain.001"
            )).get();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.content()).isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.content();
            assertThat(result).containsKey("valid");
            assertThat(result).containsKey("messageType");
        }

        @Test
        @DisplayName("should invoke risk score tool")
        void shouldInvokeRiskScore() throws Exception {
            McpToolResponse response = client.invoke("risk_score", Map.of(
                "transactionId", "TX123",
                "amount", 15000,
                "currency", "GBP"
            )).get();

            assertThat(response.isSuccess()).isTrue();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.content();
            assertThat(result).containsKey("score");
            assertThat(result).containsKey("riskLevel");
            assertThat(result.get("riskLevel")).isEqualTo("HIGH"); // amount > 10000
        }

        @Test
        @DisplayName("should invoke customer lookup")
        void shouldInvokeCustomerLookup() throws Exception {
            McpToolResponse response = client.invoke("customer_lookup", Map.of(
                "customerId", "CUST123"
            )).get();

            assertThat(response.isSuccess()).isTrue();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.content();
            assertThat(result.get("customerId")).isEqualTo("CUST123");
            assertThat(result.get("name")).isEqualTo("[REDACTED]");
        }

        @Test
        @DisplayName("should return error for unknown tool")
        void shouldReturnErrorForUnknownTool() throws Exception {
            McpToolResponse response = client.invoke("unknown_tool", Map.of()).get();

            assertThat(response.isError()).isTrue();
            assertThat(response.errorMessage()).contains("not found");
        }

        @Test
        @DisplayName("should fail when not connected")
        void shouldFailInvokeWhenNotConnected() throws Exception {
            client.disconnect().get();

            assertThatThrownBy(() -> client.invoke("any_tool", Map.of()).get())
                .isInstanceOf(ExecutionException.class);
        }
    }

    @Nested
    @DisplayName("custom tool registration")
    class CustomToolRegistration {

        @Test
        @DisplayName("should register and invoke custom tool")
        void shouldRegisterCustomTool() throws Exception {
            client.registerTool(
                McpTool.builder()
                    .name("custom_tool")
                    .description("Custom test tool")
                    .serverUrl(client.getServerUrl())
                    .build(),
                args -> Map.of("result", "custom response", "input", args)
            );

            client.connect().get();

            McpToolResponse response = client.invoke("custom_tool", Map.of("key", "value")).get();

            assertThat(response.isSuccess()).isTrue();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.content();
            assertThat(result.get("result")).isEqualTo("custom response");
        }

        @Test
        @DisplayName("should discover custom tools")
        void shouldDiscoverCustomTool() throws Exception {
            client.registerTool(
                McpTool.builder()
                    .name("my_custom_tool")
                    .description("My custom tool")
                    .serverUrl(client.getServerUrl())
                    .build(),
                null
            );

            client.connect().get();
            List<McpTool> tools = client.discoverTools().get();

            assertThat(tools).anyMatch(t -> t.name().equals("my_custom_tool"));
        }
    }

    @Nested
    @DisplayName("response metadata")
    class ResponseMetadata {

        @BeforeEach
        void connect() throws Exception {
            client.connect().get();
        }

        @Test
        @DisplayName("should include mock flag in metadata")
        void shouldIncludeMockFlag() throws Exception {
            McpToolResponse response = client.invoke("iso20022_validate", Map.of(
                "message", "test",
                "messageType", "pain.001"
            )).get();

            assertThat(response.metadata()).containsEntry("mock", true);
        }
    }
}

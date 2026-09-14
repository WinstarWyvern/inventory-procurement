package com.procurement.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchaseRequestTests extends AbstractIntegrationTest {

    private MvcResult createDraft(String itemsJson) throws Exception {
        String body = """
                { "warehouseId": %d, "items": %s }
                """.formatted(warehouse.getId(), itemsJson);

        return mockMvc.perform(post("/purchase-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void createsDraftPurchaseRequestWithItems() throws Exception {
        MvcResult result = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode data = json(result).get("data");
        assertThat(data.get("status").asText()).isEqualTo("DRAFT");
        assertThat(data.get("items")).hasSize(1);
        assertThat(data.get("requestNumber").asText()).matches("PR-\\d{4}-\\d{6}");
    }

    @Test
    void rejectsDuplicateProductsInSameRequest() throws Exception {
        MvcResult result = createDraft("""
                [ { "productId": %d, "quantity": 10 }, { "productId": %d, "quantity": 5 } ]
                """.formatted(product.getId(), product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("DUPLICATE_PRODUCT_IN_REQUEST");
    }

    @Test
    void rejectsInactiveProduct() throws Exception {
        MvcResult result = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(inactiveProduct.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PRODUCT_INACTIVE");
    }

    @Test
    void cannotSubmitPurchaseRequestWithoutItems() throws Exception {
        MvcResult created = createDraft("[]");
        long prId = json(created).at("/data/id").asLong();

        MvcResult result = mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                        .header("Authorization", "Bearer " + userToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_REQUEST_EMPTY");
    }

    @Test
    void submitsPurchaseRequestThatHasItems() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void cannotEditPurchaseRequestOnceSubmitted() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));

        String body = """
                { "productId": %d, "quantity": 5 }
                """.formatted(secondProduct.getId());

        MvcResult result = mockMvc.perform(post("/purchase-requests/{id}/items", prId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_REQUEST_NOT_DRAFT");
    }

    @Test
    void cannotApprovePurchaseRequestThatIsNotSubmitted() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        MvcResult result = mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_REQUEST_NOT_SUBMITTED");
    }

    @Test
    void onlyApproverCanApproveNotUser() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));

        mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                        .header("Authorization", "Bearer " + userToken) // wrong role
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void approvesSubmittedPurchaseRequest() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));

        mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"remarks\": \"ok\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approver.id").value(approver.getId()));
    }

    @Test
    void rejectsSubmittedPurchaseRequest() throws Exception {
        MvcResult created = createDraft("""
                [ { "productId": %d, "quantity": 10 } ]
                """.formatted(product.getId()));
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));

        mockMvc.perform(post("/purchase-requests/{id}/reject", prId)
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"remarks\": \"not needed\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void rejectsQuantityLessThanOrEqualToZero() throws Exception {
        MvcResult result = createDraft("""
                [ { "productId": %d, "quantity": 0 } ]
                """.formatted(product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void loginReturnsTokenForSeededCredentials() throws Exception {
        String body = """
                { "username": "test.user", "password": "password123" }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("test.user"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String body = """
                { "username": "test.user", "password": "wrong-password" }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}

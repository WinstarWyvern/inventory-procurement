package com.procurement.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseOrderTests extends AbstractIntegrationTest {

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Creates and fully approves a Purchase Request for {@code quantity} units of the fixture product. */
    private long createApprovedPr(int quantity) throws Exception {
        String createBody = """
                { "warehouseId": %d, "items": [ { "productId": %d, "quantity": %d } ] }
                """.formatted(warehouse.getId(), product.getId(), quantity);

        MvcResult created = mockMvc.perform(post("/purchase-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        long prId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));

        mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        return prId;
    }

    private MvcResult createPo(long purchaseRequestId, long supplierId) throws Exception {
        String body = """
                { "purchaseRequestId": %d, "supplierId": %d }
                """.formatted(purchaseRequestId, supplierId);

        return mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void cannotCreatePurchaseOrderFromNonApprovedPurchaseRequest() throws Exception {
        String createBody = """
                { "warehouseId": %d, "items": [ { "productId": %d, "quantity": 10 } ] }
                """.formatted(warehouse.getId(), product.getId());

        MvcResult created = mockMvc.perform(post("/purchase-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        long prId = json(created).at("/data/id").asLong(); // still DRAFT

        MvcResult result = createPo(prId, supplier.getId());

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_REQUEST_NOT_APPROVED");
    }

    @Test
    void createsPurchaseOrderFromApprovedPurchaseRequestCopyingItems() throws Exception {
        long prId = createApprovedPr(25);

        MvcResult result = createPo(prId, supplier.getId());
        JsonNode data = json(result).get("data");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(data.get("status").asText()).isEqualTo("DRAFT");
        assertThat(data.get("warehouse").get("id").asLong()).isEqualTo(warehouse.getId());
        assertThat(data.get("items")).hasSize(1);
        assertThat(data.get("items").get(0).get("orderedQuantity").asInt()).isEqualTo(25);
        assertThat(data.get("items").get(0).get("receivedQuantity").asInt()).isEqualTo(0);
        assertThat(data.get("poNumber").asText()).matches("PO-\\d{4}-\\d{6}");
    }

    @Test
    void cannotCreateMoreThanOnePurchaseOrderFromSamePurchaseRequest() throws Exception {
        long prId = createApprovedPr(10);

        MvcResult first = createPo(prId, supplier.getId());
        assertThat(first.getResponse().getStatus()).isEqualTo(201);

        MvcResult second = createPo(prId, supplier.getId());
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(second).at("/error/code").asText()).isEqualTo("PURCHASE_ORDER_ALREADY_EXISTS");
    }

    @Test
    void cannotCreatePurchaseOrderWithInactiveSupplier() throws Exception {
        long prId = createApprovedPr(10);

        MvcResult result = createPo(prId, inactiveSupplier.getId());

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("SUPPLIER_INACTIVE");
    }

    @Test
    void marksDraftPurchaseOrderAsOrdered() throws Exception {
        long prId = createApprovedPr(10);
        MvcResult created = createPo(prId, supplier.getId());
        long poId = json(created).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/mark-ordered", poId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ORDERED"));
    }
}

package com.procurement.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GoodsReceiptTests extends AbstractIntegrationTest {

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Creates an ORDERED Purchase Order for {@code quantity} units of the fixture product. */
    private long createOrderedPo(int quantity) throws Exception {
        String createBody = """
                { "warehouseId": %d, "items": [ { "productId": %d, "quantity": %d } ] }
                """.formatted(warehouse.getId(), product.getId(), quantity);

        MvcResult createdPr = mockMvc.perform(post("/purchase-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        long prId = json(createdPr).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));
        mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        String poBody = """
                { "purchaseRequestId": %d, "supplierId": %d }
                """.formatted(prId, supplier.getId());
        MvcResult createdPo = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andReturn();
        long poId = json(createdPo).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/mark-ordered", poId)
                .header("Authorization", "Bearer " + userToken));

        return poId;
    }

    /** Returns a DRAFT (not yet ordered) Purchase Order, for negative-path tests. */
    private long createDraftPo() throws Exception {
        String createBody = """
                { "warehouseId": %d, "items": [ { "productId": %d, "quantity": 10 } ] }
                """.formatted(warehouse.getId(), product.getId());

        MvcResult createdPr = mockMvc.perform(post("/purchase-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        long prId = json(createdPr).at("/data/id").asLong();

        mockMvc.perform(post("/purchase-requests/{id}/submit", prId)
                .header("Authorization", "Bearer " + userToken));
        mockMvc.perform(post("/purchase-requests/{id}/approve", prId)
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        String poBody = """
                { "purchaseRequestId": %d, "supplierId": %d }
                """.formatted(prId, supplier.getId());
        MvcResult createdPo = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andReturn();
        return json(createdPo).at("/data/id").asLong();
    }

    private MvcResult receive(long purchaseOrderId, String itemsJson) throws Exception {
        String body = """
                { "purchaseOrderId": %d, "items": %s }
                """.formatted(purchaseOrderId, itemsJson);

        return mockMvc.perform(post("/goods-receipts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private JsonNode getWarehouseStock(long warehouseId) throws Exception {
        MvcResult result = mockMvc.perform(get("/inventory/warehouses/{id}/stock", warehouseId)
                        .header("Authorization", "Bearer " + userToken))
                .andReturn();
        return json(result).at("/data/stock");
    }

    @Test
    void cannotReceiveAgainstPoNotYetMarkedOrdered() throws Exception {
        long poId = createDraftPo();

        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 5 } ]".formatted(product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_ORDER_NOT_ORDERED");
    }

    @Test
    void cannotReceiveQuantityGreaterThanOrdered() throws Exception {
        long poId = createOrderedPo(100);

        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 150 } ]".formatted(product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("RECEIVED_QUANTITY_EXCEEDS_ORDERED");
    }

    @Test
    void cannotReceiveCumulativeQuantityGreaterThanOrderedAcrossMultipleReceipts() throws Exception {
        long poId = createOrderedPo(100);

        MvcResult first = receive(poId, "[ { \"productId\": %d, \"quantity\": 80 } ]".formatted(product.getId()));
        assertThat(first.getResponse().getStatus()).isEqualTo(201);

        MvcResult second = receive(poId, "[ { \"productId\": %d, \"quantity\": 30 } ]".formatted(product.getId()));
        assertThat(second.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(second).at("/error/code").asText()).isEqualTo("RECEIVED_QUANTITY_EXCEEDS_ORDERED");
    }

    @Test
    void increasesWarehouseStockByReceivedQuantity() throws Exception {
        long poId = createOrderedPo(100);

        JsonNode before = getWarehouseStock(warehouse.getId());
        assertThat(before).isEmpty();

        receive(poId, "[ { \"productId\": %d, \"quantity\": 60 } ]".formatted(product.getId()));

        JsonNode after = getWarehouseStock(warehouse.getId());
        assertThat(after.get(0).get("quantity").asInt()).isEqualTo(60);
    }

    @Test
    void accumulatesStockAcrossMultiplePartialReceipts() throws Exception {
        long poId = createOrderedPo(100);

        receive(poId, "[ { \"productId\": %d, \"quantity\": 60 } ]".formatted(product.getId()));
        receive(poId, "[ { \"productId\": %d, \"quantity\": 40 } ]".formatted(product.getId()));

        JsonNode stock = getWarehouseStock(warehouse.getId());
        assertThat(stock.get(0).get("quantity").asInt()).isEqualTo(100);
    }

    @Test
    void marksPoAsPartiallyReceivedAfterPartialReceipt() throws Exception {
        long poId = createOrderedPo(100);

        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 60 } ]".formatted(product.getId()));

        assertThat(json(result).at("/data/purchaseOrder/status").asText()).isEqualTo("PARTIALLY_RECEIVED");
    }

    @Test
    void marksPoAsReceivedOnceFullyReceived() throws Exception {
        long poId = createOrderedPo(100);

        receive(poId, "[ { \"productId\": %d, \"quantity\": 60 } ]".formatted(product.getId()));
        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 40 } ]".formatted(product.getId()));

        assertThat(json(result).at("/data/purchaseOrder/status").asText()).isEqualTo("RECEIVED");
    }

    @Test
    void cannotReceiveAgainstAlreadyReceivedPo() throws Exception {
        long poId = createOrderedPo(50);
        receive(poId, "[ { \"productId\": %d, \"quantity\": 50 } ]".formatted(product.getId()));

        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 1 } ]".formatted(product.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PURCHASE_ORDER_ALREADY_RECEIVED");
    }

    @Test
    void recordsInventoryMovementForEveryReceipt() throws Exception {
        long poId = createOrderedPo(100);
        receive(poId, "[ { \"productId\": %d, \"quantity\": 60 } ]".formatted(product.getId()));

        MvcResult result = mockMvc.perform(get("/inventory/movements")
                        .param("warehouseId", String.valueOf(warehouse.getId()))
                        .header("Authorization", "Bearer " + userToken))
                .andReturn();

        JsonNode movements = json(result).get("data");
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).get("movementType").asText()).isEqualTo("PURCHASE_RECEIPT");
        assertThat(movements.get(0).get("quantity").asInt()).isEqualTo(60);
    }

    @Test
    void rejectsProductNotPartOfPurchaseOrder() throws Exception {
        long poId = createOrderedPo(100);

        MvcResult result = receive(poId, "[ { \"productId\": %d, \"quantity\": 5 } ]".formatted(secondProduct.getId()));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).at("/error/code").asText()).isEqualTo("PRODUCT_NOT_IN_PURCHASE_ORDER");
    }
}

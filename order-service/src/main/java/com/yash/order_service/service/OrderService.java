package com.yash.order_service.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.yash.order_service.dto.InventoryResponse;
import com.yash.order_service.dto.OrderLineItemsDto;
import com.yash.order_service.dto.OrderRequest;
import com.yash.order_service.model.Order;
import com.yash.order_service.model.OrderLineItems;
import com.yash.order_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) throws IllegalAccessException {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToEntity)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(orderLineItem -> orderLineItem.getSkuCode())
                .toList();

        InventoryResponse[] inventoryResponseArray = webClient.get()
                .uri("http://localhost:8082/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = inventoryResponseArray != null
                && inventoryResponseArray.length == skuCodes.size()
                && Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
        if (allProductsInStock) {
            orderRepository.save(order);
        } else {
            throw new IllegalAccessException("Order Failed, Products may be out of stock!");
        }

    }

    private OrderLineItems mapToEntity(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}

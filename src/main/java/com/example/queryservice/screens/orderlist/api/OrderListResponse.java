package com.example.queryservice.screens.orderlist.api;

import com.example.queryservice.screens.orderlist.entity.OrderListView;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderListResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        BigDecimal orderAmount,
        Instant orderedAt,
        Long memberId,
        String memberName,
        String memberGrade,
        Long productId,
        String productName,
        String productCategory,
        BigDecimal productPrice,
        String deliveryStatus,
        String deliveryAddress,
        Instant deliveryTrackedAt
) {

    public static OrderListResponse from(OrderListView v) {
        return new OrderListResponse(
                v.getOrderId(),
                v.getOrderNumber(),
                v.getOrderStatus(),
                v.getOrderAmount(),
                v.getOrderedAt(),
                v.getMemberId(),
                v.getMemberName(),
                v.getMemberGrade(),
                v.getProductId(),
                v.getProductName(),
                v.getProductCategory(),
                v.getProductPrice(),
                v.getDeliveryStatus(),
                v.getDeliveryAddress(),
                v.getDeliveryTrackedAt()
        );
    }
}

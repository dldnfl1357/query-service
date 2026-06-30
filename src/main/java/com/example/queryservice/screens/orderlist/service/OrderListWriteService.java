package com.example.queryservice.screens.orderlist.service;

import com.example.queryservice.common.idempotency.IdempotencyService;
import com.example.queryservice.screens.orderlist.entity.OrderListView;
import com.example.queryservice.screens.orderlist.event.DeliveryChangedEvent;
import com.example.queryservice.screens.orderlist.event.MemberChangedEvent;
import com.example.queryservice.screens.orderlist.event.OrderChangedEvent;
import com.example.queryservice.screens.orderlist.event.ProductChangedEvent;
import com.example.queryservice.screens.orderlist.repository.OrderListViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderListWriteService {

    private static final String CONSUMER = "orderList";

    private final OrderListViewRepository repository;
    private final IdempotencyService idempotency;

    @Transactional
    public void applyOrder(OrderChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }

        OrderListView view = repository.findById(event.orderId())
                .orElseGet(() -> OrderListView.empty(event.orderId()));

        if (view.getOrderSectionUpdatedAt() != null
                && !event.occurredAt().isAfter(view.getOrderSectionUpdatedAt())) {
            log.debug("skip stale order event orderId={} occurredAt={}", event.orderId(), event.occurredAt());
            return;
        }

        view.setOrderNumber(event.orderNumber());
        view.setOrderStatus(event.orderStatus());
        view.setOrderAmount(event.orderAmount());
        view.setOrderedAt(event.orderedAt());
        view.setMemberId(event.memberId());
        view.setProductId(event.productId());
        view.setOrderSectionUpdatedAt(event.occurredAt());

        repository.save(view);
    }

    @Transactional
    public void applyMember(MemberChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }
        int updated = repository.updateMemberSection(
                event.memberId(),
                event.memberName(),
                event.memberEmail(),
                event.memberGrade(),
                event.occurredAt()
        );
        log.debug("member section updated memberId={} rows={}", event.memberId(), updated);
    }

    @Transactional
    public void applyProduct(ProductChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }
        int updated = repository.updateProductSection(
                event.productId(),
                event.productName(),
                event.productCategory(),
                event.productPrice(),
                event.occurredAt()
        );
        log.debug("product section updated productId={} rows={}", event.productId(), updated);
    }

    @Transactional
    public void applyDelivery(DeliveryChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }
        int updated = repository.updateDeliverySection(
                event.orderId(),
                event.deliveryStatus(),
                event.deliveryAddress(),
                event.deliveryTrackedAt(),
                event.occurredAt()
        );
        log.debug("delivery section updated orderId={} rows={}", event.orderId(), updated);
    }
}

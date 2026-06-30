package com.example.queryservice.screens.orderlist.handler;

import com.example.queryservice.screens.orderlist.event.DeliveryChangedEvent;
import com.example.queryservice.screens.orderlist.event.MemberChangedEvent;
import com.example.queryservice.screens.orderlist.event.OrderChangedEvent;
import com.example.queryservice.screens.orderlist.event.ProductChangedEvent;
import com.example.queryservice.screens.orderlist.service.OrderListWriteService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도메인별 큐에서 받은 이벤트를 주문 목록 화면 테이블에 반영.
 * 각 메서드는 가상 스레드 위에서 병렬 처리됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListSqsHandlers {

    private final OrderListWriteService writeService;

    @SqsListener("${query-service.queues.order}")
    public void onOrderChanged(OrderChangedEvent event) {
        log.info("order event received orderId={}", event.orderId());
        writeService.applyOrder(event);
    }

    @SqsListener("${query-service.queues.member}")
    public void onMemberChanged(MemberChangedEvent event) {
        log.info("member event received memberId={}", event.memberId());
        writeService.applyMember(event);
    }

    @SqsListener("${query-service.queues.product}")
    public void onProductChanged(ProductChangedEvent event) {
        log.info("product event received productId={}", event.productId());
        writeService.applyProduct(event);
    }

    @SqsListener("${query-service.queues.delivery}")
    public void onDeliveryChanged(DeliveryChangedEvent event) {
        log.info("delivery event received orderId={}", event.orderId());
        writeService.applyDelivery(event);
    }
}

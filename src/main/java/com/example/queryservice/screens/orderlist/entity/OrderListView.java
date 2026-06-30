package com.example.queryservice.screens.orderlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 주문 목록 화면 1:1 비정규화 테이블.
 * 도메인 섹션별 updatedAt 으로 out-of-order 이벤트를 last-write-wins 로 처리.
 */
@Entity
@Table(
        name = "order_list_view",
        indexes = {
                @Index(name = "idx_olv_member_id", columnList = "member_id"),
                @Index(name = "idx_olv_product_id", columnList = "product_id"),
                @Index(name = "idx_olv_ordered_at", columnList = "ordered_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class OrderListView {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number", length = 64)
    private String orderNumber;

    @Column(name = "order_status", length = 32)
    private String orderStatus;

    @Column(name = "order_amount", precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "order_section_updated_at")
    private Instant orderSectionUpdatedAt;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", length = 128)
    private String memberName;

    @Column(name = "member_email", length = 256)
    private String memberEmail;

    @Column(name = "member_grade", length = 32)
    private String memberGrade;

    @Column(name = "member_section_updated_at")
    private Instant memberSectionUpdatedAt;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 256)
    private String productName;

    @Column(name = "product_category", length = 64)
    private String productCategory;

    @Column(name = "product_price", precision = 19, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "product_section_updated_at")
    private Instant productSectionUpdatedAt;

    @Column(name = "delivery_status", length = 32)
    private String deliveryStatus;

    @Column(name = "delivery_address", length = 512)
    private String deliveryAddress;

    @Column(name = "delivery_tracked_at")
    private Instant deliveryTrackedAt;

    @Column(name = "delivery_section_updated_at")
    private Instant deliverySectionUpdatedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public static OrderListView empty(Long orderId) {
        OrderListView v = new OrderListView();
        v.orderId = orderId;
        return v;
    }
}

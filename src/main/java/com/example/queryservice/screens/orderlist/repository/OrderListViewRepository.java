package com.example.queryservice.screens.orderlist.repository;

import com.example.queryservice.screens.orderlist.entity.OrderListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderListViewRepository extends JpaRepository<OrderListView, Long> {

    Page<OrderListView> findByMemberId(Long memberId, Pageable pageable);

    @Modifying
    @Query("""
            update OrderListView v
               set v.memberName = :name,
                   v.memberEmail = :email,
                   v.memberGrade = :grade,
                   v.memberSectionUpdatedAt = :updatedAt
             where v.memberId = :memberId
               and (v.memberSectionUpdatedAt is null or v.memberSectionUpdatedAt < :updatedAt)
            """)
    int updateMemberSection(
            @Param("memberId") Long memberId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("grade") String grade,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Query("""
            update OrderListView v
               set v.productName = :name,
                   v.productCategory = :category,
                   v.productPrice = :price,
                   v.productSectionUpdatedAt = :updatedAt
             where v.productId = :productId
               and (v.productSectionUpdatedAt is null or v.productSectionUpdatedAt < :updatedAt)
            """)
    int updateProductSection(
            @Param("productId") Long productId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("price") BigDecimal price,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Query("""
            update OrderListView v
               set v.deliveryStatus = :status,
                   v.deliveryAddress = :address,
                   v.deliveryTrackedAt = :trackedAt,
                   v.deliverySectionUpdatedAt = :updatedAt
             where v.orderId = :orderId
               and (v.deliverySectionUpdatedAt is null or v.deliverySectionUpdatedAt < :updatedAt)
            """)
    int updateDeliverySection(
            @Param("orderId") Long orderId,
            @Param("status") String status,
            @Param("address") String address,
            @Param("trackedAt") Instant trackedAt,
            @Param("updatedAt") Instant updatedAt
    );
}

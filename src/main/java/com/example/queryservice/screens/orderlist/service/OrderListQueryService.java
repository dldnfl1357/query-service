package com.example.queryservice.screens.orderlist.service;

import com.example.queryservice.screens.orderlist.api.OrderListResponse;
import com.example.queryservice.screens.orderlist.repository.OrderListViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderListQueryService {

    private final OrderListViewRepository repository;

    public Page<OrderListResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(OrderListResponse::from);
    }

    public Page<OrderListResponse> findByMember(Long memberId, Pageable pageable) {
        return repository.findByMemberId(memberId, pageable).map(OrderListResponse::from);
    }
}

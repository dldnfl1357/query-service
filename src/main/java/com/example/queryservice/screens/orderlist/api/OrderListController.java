package com.example.queryservice.screens.orderlist.api;

import com.example.queryservice.screens.orderlist.service.OrderListQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-list")
@RequiredArgsConstructor
public class OrderListController {

    private final OrderListQueryService queryService;

    @GetMapping
    public Page<OrderListResponse> list(
            @RequestParam(required = false) Long memberId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return memberId == null
                ? queryService.findAll(pageable)
                : queryService.findByMember(memberId, pageable);
    }
}

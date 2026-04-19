package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderDTO;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * 특정 사용자가 구매한 주문 내역 목록을 조회합니다.
     *
     * @param userId 주문 내역을 조회할 구매자의 사용자 식별자(UUID)
     * @return 조회된 주문 정보 목록 DTO 리스트
     */
    public List<OrderDTO> getMyOrders(UUID userId) {
        return orderRepository.findByBuyerId(userId).stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
    }

    // 필요 시 판매자 주문 조회, 상세 조회 추가
}
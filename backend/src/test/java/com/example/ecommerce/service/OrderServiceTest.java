package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderDTO;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("특정 사용자의 주문 내역을 성공적으로 조회한다.")
    void getMyOrders_success() {
        // Given
        UUID userId = UUID.randomUUID();
        Order order = Order.builder().id(UUID.randomUUID()).build();
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());

        given(orderRepository.findByBuyerId(userId)).willReturn(List.of(order));
        given(orderMapper.toDTO(order)).willReturn(orderDTO);

        // When
        List<OrderDTO> result = orderService.getMyOrders(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(order.getId());

        then(orderRepository).should(times(1)).findByBuyerId(userId);
        then(orderMapper).should(times(1)).toDTO(order);
    }
}

package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.CreateOrderRequest;
import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.service.OrderService;
import com.example.pharmacy.vo.CartSummaryVO;
import com.example.pharmacy.vo.OrderDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setViewResolvers(new NoOpViewResolver())
                .build();
    }

    /**
     * 场景：订单确认页应展示购物车汇总和默认收货信息。
     */
    @Test
    void confirm_shouldReturnConfirmView() throws Exception {
        when(orderService.previewOrder()).thenReturn(new CartSummaryVO());

        mockMvc.perform(get("/orders/confirm"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/confirm"))
                .andExpect(model().attributeExists("cart"))
                .andExpect(model().attributeExists("createOrderRequest"));
    }

    /**
     * 场景：创建订单成功后，应重定向到订单详情页。
     */
    @Test
    void create_shouldRedirectToOrderDetail() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(100L);
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders")
                        .param("receiverName", "测试用户")
                        .param("receiverPhone", "13800000000")
                        .param("receiverAddress", "测试地址")
                        .param("deliveryMethod", "标准配送"))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/orders/100"));
    }

    /**
     * 场景：订单列表页应返回订单集合模型。
     */
    @Test
    void list_shouldReturnOrderListModel() throws Exception {
        when(orderService.listOrders()).thenReturn(List.of(new OrderEntity()));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("orders", hasSize(1)));
    }

    /**
     * 场景：取消订单失败时，应仍重定向回订单详情并携带错误提示。
     */
    @Test
    void cancel_whenServiceThrows_shouldRedirectBackToDetail() throws Exception {
        doThrow(new BusinessException("不能取消")).when(orderService).cancelOrder(100L);

        mockMvc.perform(post("/orders/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/orders/100"));
    }

    private static class NoOpViewResolver implements ViewResolver {
        @Override
        public View resolveViewName(String viewName, Locale locale) {
            return new AbstractView() {
                @Override
                protected void renderMergedOutputModel(Map<String, Object> model,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
                    response.setStatus(HttpStatus.OK.value());
                }
            };
        }
    }
}

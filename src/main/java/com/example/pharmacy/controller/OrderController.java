package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.CreateOrderRequest;
import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/confirm")
    public String confirm(Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cart", orderService.previewOrder());
            model.addAttribute("createOrderRequest", defaultAddress());
            return "orders/confirm";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping
    public String create(@ModelAttribute CreateOrderRequest request, RedirectAttributes redirectAttributes) {
        try {
            OrderEntity order = orderService.createOrder(request);
            redirectAttributes.addFlashAttribute("success", "订单提交成功，请完成支付");
            return "redirect:/orders/" + order.getId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/orders/confirm";
        }
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.listOrders());
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("detail", orderService.getOrderDetail(id));
        return "orders/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("success", "订单已取消，库存已释放");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    private CreateOrderRequest defaultAddress() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setReceiverName("课程演示用户");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("北京市朝阳区健康路 88 号康宁社区 1 号楼");
        request.setDeliveryMethod("标准配送");
        return request;
    }
}

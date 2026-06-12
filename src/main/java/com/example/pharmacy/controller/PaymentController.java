package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/pay")
    public String simulatePay(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            paymentService.simulatePay(orderId);
            redirectAttributes.addFlashAttribute("success", "模拟支付成功，订单已变更为已支付");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }
}

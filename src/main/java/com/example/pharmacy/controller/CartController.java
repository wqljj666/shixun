package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String cart(Model model) {
        model.addAttribute("cart", cartService.getCartSummary());
        return "cart/index";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long medicineId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(medicineId, quantity);
            redirectAttributes.addFlashAttribute("success", "已加入购物车");
            return "redirect:/cart";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/medicines/" + medicineId;
        }
    }

    @PostMapping("/{id}/update")
    public String updateQuantity(@PathVariable Long id,
                                 @RequestParam Integer quantity,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(id, quantity);
            redirectAttributes.addFlashAttribute("success", "购物车数量已更新");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItem(id);
            redirectAttributes.addFlashAttribute("success", "商品已从购物车移除");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }
}

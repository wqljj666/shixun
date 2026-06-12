package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.service.AiConsultationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ai-consult")
public class AiConsultationController {

    private final AiConsultationService consultationService;

    public AiConsultationController(AiConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("consultationRequest", new ConsultationRequest());
        model.addAttribute("recentConsultations", consultationService.history().stream().limit(3).toList());
        return "ai-consult/index";
    }

    @PostMapping
    public String consult(@ModelAttribute ConsultationRequest request, Model model) {
        model.addAttribute("consultationRequest", request);
        model.addAttribute("recentConsultations", consultationService.history().stream().limit(3).toList());
        try {
            model.addAttribute("response", consultationService.consult(request));
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "ai-consult/index";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("records", consultationService.history());
        return "ai-consult/history";
    }
}

package com.example.pharmacy.controller;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.service.AiConsultationService;
import com.example.pharmacy.vo.ConsultationResponseVO;
import com.example.pharmacy.vo.ConversationDetailVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/ai-consult")
public class AiConsultationController {

    private final AiConsultationService consultationService;

    public AiConsultationController(AiConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String conversationId, Model model) {
        ConversationDetailVO conversation = StringUtils.hasText(conversationId)
                ? consultationService.getConversationDetail(conversationId)
                : consultationService.createConversation();
        model.addAttribute("conversation", conversation);
        model.addAttribute("conversations", consultationService.listConversations());
        return "ai-consult/index";
    }

    @PostMapping("/send")
    @ResponseBody
    public ConsultationResponseVO send(@RequestBody ConsultationRequest request) {
        return consultationService.sendMessage(request);
    }

    @GetMapping("/conversations/{conversationId}")
    @ResponseBody
    public ConversationDetailVO conversation(@PathVariable String conversationId) {
        return consultationService.getConversationDetail(conversationId);
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("records", consultationService.listConversations());
        return "ai-consult/history";
    }
}

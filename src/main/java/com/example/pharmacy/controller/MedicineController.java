package com.example.pharmacy.controller;

import com.example.pharmacy.dto.MedicineSearchRequest;
import com.example.pharmacy.service.MedicineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public String list(@ModelAttribute MedicineSearchRequest request, Model model) {
        model.addAttribute("categories", medicineService.listActiveCategories());
        model.addAttribute("medicines", medicineService.searchMedicines(request));
        model.addAttribute("searchRequest", request);
        return "medicine/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("medicine", medicineService.getMedicineDetail(id));
        return "medicine/detail";
    }
}

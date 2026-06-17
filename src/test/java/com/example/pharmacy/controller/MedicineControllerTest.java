package com.example.pharmacy.controller;

import com.example.pharmacy.entity.MedicineCategoryEntity;
import com.example.pharmacy.service.MedicineService;
import com.example.pharmacy.vo.MedicineDetailVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    @Mock
    private MedicineService medicineService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MedicineController(medicineService))
                .setViewResolvers(new NoOpViewResolver())
                .build();
    }

    /**
     * 场景：访问药品列表页时，应返回分类、药品列表和搜索条件模型。
     */
    @Test
    void list_shouldReturnMedicineListModel() throws Exception {
        when(medicineService.listActiveCategories()).thenReturn(List.of(category()));
        when(medicineService.searchMedicines(any())).thenReturn(List.of(medicineDetail()));

        mockMvc.perform(get("/medicines").param("keyword", "维生素"))
                .andExpect(status().isOk())
                .andExpect(view().name("medicine/list"))
                .andExpect(model().attribute("categories", hasSize(1)))
                .andExpect(model().attribute("medicines", hasSize(1)))
                .andExpect(model().attributeExists("searchRequest"));
    }

    /**
     * 场景：访问药品详情页时，应返回 medicine 模型。
     */
    @Test
    void detail_shouldReturnMedicineDetailModel() throws Exception {
        when(medicineService.getMedicineDetail(1L)).thenReturn(medicineDetail());

        mockMvc.perform(get("/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("medicine/detail"))
                .andExpect(model().attributeExists("medicine"));
    }

    private MedicineCategoryEntity category() {
        MedicineCategoryEntity category = new MedicineCategoryEntity();
        category.setId(1L);
        category.setName("感冒用药");
        return category;
    }

    private MedicineDetailVO medicineDetail() {
        MedicineDetailVO vo = new MedicineDetailVO();
        vo.setId(1L);
        vo.setName("维生素C咀嚼片");
        vo.setPrice(new BigDecimal("16.80"));
        vo.setStock(20);
        vo.setOtcFlag(true);
        vo.setPrescriptionRequired(false);
        return vo;
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

package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.MedicineSearchRequest;
import com.example.pharmacy.entity.MedicineCategoryEntity;
import com.example.pharmacy.entity.MedicineEntity;
import com.example.pharmacy.repository.MedicineCategoryRepository;
import com.example.pharmacy.repository.MedicineRepository;
import com.example.pharmacy.vo.MedicineDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineCategoryRepository medicineCategoryRepository;

    private MedicineService medicineService;

    @BeforeEach
    void setUp() {
        medicineService = new MedicineService(medicineRepository, medicineCategoryRepository);
    }

    /**
     * 场景：根据关键词“维生素”搜索药品，应该返回匹配药品并携带分类名、OTC 标识。
     */
    @Test
    void searchMedicines_shouldReturnKeywordMatchedMedicines() {
        MedicineSearchRequest request = new MedicineSearchRequest();
        request.setKeyword("维生素");

        MedicineCategoryEntity vitaminCategory = category(2L, "维生素类");
        MedicineEntity vitamin = medicine(10L, "维生素C咀嚼片", 2L, false);

        when(medicineRepository.search("维生素", null)).thenReturn(List.of(vitamin));
        when(medicineCategoryRepository.findByStatusOrderBySortOrderAscIdAsc(1)).thenReturn(List.of(vitaminCategory));

        List<MedicineDetailVO> result = medicineService.searchMedicines(request);

        assertEquals(1, result.size());
        assertEquals("维生素C咀嚼片", result.get(0).getName());
        assertEquals("维生素类", result.get(0).getCategoryName());
        assertTrue(result.get(0).getOtcFlag());
        assertFalse(result.get(0).getPrescriptionRequired());
    }

    /**
     * 场景：根据分类 ID 筛选药品，仓储层应该收到对应分类条件。
     */
    @Test
    void searchMedicines_shouldUseCategoryFilter() {
        MedicineSearchRequest request = new MedicineSearchRequest();
        request.setCategoryId(4L);

        MedicineEntity prescription = medicine(20L, "阿莫西林胶囊", 4L, true);
        when(medicineRepository.search(null, 4L)).thenReturn(List.of(prescription));
        when(medicineCategoryRepository.findByStatusOrderBySortOrderAscIdAsc(1)).thenReturn(List.of(category(4L, "处方药")));

        List<MedicineDetailVO> result = medicineService.searchMedicines(request);

        assertEquals(1, result.size());
        assertEquals("处方药", result.get(0).getCategoryName());
        assertTrue(result.get(0).getPrescriptionRequired());
    }

    /**
     * 场景：查询药品详情时，应该返回名称、规格、价格、库存等核心信息。
     */
    @Test
    void getMedicineDetail_shouldReturnDetailFields() {
        MedicineEntity medicine = medicine(1L, "连花清瘟胶囊", 1L, false);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));
        when(medicineCategoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "感冒用药")));

        MedicineDetailVO detail = medicineService.getMedicineDetail(1L);

        assertNotNull(detail);
        assertEquals("连花清瘟胶囊", detail.getName());
        assertEquals("0.35g*24粒/盒", detail.getSpecification());
        assertEquals(new BigDecimal("29.80"), detail.getPrice());
        assertEquals(100, detail.getStock());
    }

    /**
     * 场景：查询不存在的药品 ID，应该抛出业务异常。
     */
    @Test
    void getMedicineDetail_whenMedicineMissing_shouldThrowBusinessException() {
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicineService.getMedicineDetail(999L));
    }

    private MedicineCategoryEntity category(Long id, String name) {
        MedicineCategoryEntity category = new MedicineCategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setStatus(1);
        category.setSortOrder(1);
        return category;
    }

    private MedicineEntity medicine(Long id, String name, Long categoryId, boolean prescriptionRequired) {
        MedicineEntity medicine = new MedicineEntity();
        medicine.setId(id);
        medicine.setName(name);
        medicine.setCategoryId(categoryId);
        medicine.setSpecification("0.35g*24粒/盒");
        medicine.setPrice(new BigDecimal("29.80"));
        medicine.setStock(100);
        medicine.setManufacturer("测试药业");
        medicine.setDescription("测试说明");
        medicine.setContraindication("测试禁忌");
        medicine.setNotice("测试注意事项");
        medicine.setOtcFlag(!prescriptionRequired);
        medicine.setPrescriptionRequired(prescriptionRequired);
        medicine.setImageUrl("/img/medicine-default.svg");
        medicine.setStatus(1);
        medicine.setCreatedAt(LocalDateTime.now());
        return medicine;
    }
}

package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.MedicineSearchRequest;
import com.example.pharmacy.entity.MedicineCategoryEntity;
import com.example.pharmacy.entity.MedicineEntity;
import com.example.pharmacy.repository.MedicineCategoryRepository;
import com.example.pharmacy.repository.MedicineRepository;
import com.example.pharmacy.vo.MedicineDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * 药品信息业务服务。
 *
 * <p>负责药品分类查询、药品列表搜索和药品详情组装，是药品信息模块的核心业务层。
 * 本类会过滤已下架药品，并将药品实体与分类信息合并为页面展示所需的 VO。</p>
 */
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository medicineCategoryRepository;

    public MedicineService(MedicineRepository medicineRepository,
                           MedicineCategoryRepository medicineCategoryRepository) {
        this.medicineRepository = medicineRepository;
        this.medicineCategoryRepository = medicineCategoryRepository;
    }

    /**
     * 查询启用状态的药品分类。
     *
     * @return 按排序号和主键升序排列的分类列表
     */
    public List<MedicineCategoryEntity> listActiveCategories() {
        return medicineCategoryRepository.findByStatusOrderBySortOrderAscIdAsc(1);
    }

    /**
     * 根据关键词和分类搜索药品。
     *
     * @param request 搜索条件，包含关键词和分类 ID，可为空
     * @return 药品详情 VO 列表，包含分类名称、价格、库存和处方/OTC 标识
     */
    public List<MedicineDetailVO> searchMedicines(MedicineSearchRequest request) {
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;
        List<MedicineEntity> medicines = medicineRepository.search(keyword, request.getCategoryId());
        Map<Long, MedicineCategoryEntity> categoryMap = listActiveCategories().stream()
                .collect(Collectors.toMap(MedicineCategoryEntity::getId, Function.identity()));
        return medicines.stream()
                .map(medicine -> toVO(medicine, categoryMap.get(medicine.getCategoryId())))
                .toList();
    }

    /**
     * 查询单个药品详情。
     *
     * @param id 药品主键
     * @return 药品详情 VO
     * @throws BusinessException 当药品不存在或已下架时抛出
     */
    public MedicineDetailVO getMedicineDetail(Long id) {
        MedicineEntity medicine = medicineRepository.findById(id)
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(404, "药品不存在或已下架"));
        MedicineCategoryEntity category = medicineCategoryRepository.findById(medicine.getCategoryId()).orElse(null);
        return toVO(medicine, category);
    }

    private MedicineDetailVO toVO(MedicineEntity medicine, MedicineCategoryEntity category) {
        MedicineDetailVO vo = new MedicineDetailVO();
        vo.setId(medicine.getId());
        vo.setName(medicine.getName());
        vo.setCategoryId(medicine.getCategoryId());
        vo.setCategoryName(category == null ? "未分类" : category.getName());
        vo.setSpecification(medicine.getSpecification());
        vo.setPrice(medicine.getPrice());
        vo.setStock(medicine.getStock());
        vo.setManufacturer(medicine.getManufacturer());
        vo.setDescription(medicine.getDescription());
        vo.setContraindication(medicine.getContraindication());
        vo.setNotice(medicine.getNotice());
        vo.setOtcFlag(medicine.getOtcFlag());
        vo.setPrescriptionRequired(medicine.getPrescriptionRequired());
        vo.setImageUrl(medicine.getImageUrl());
        vo.setStatus(medicine.getStatus());
        vo.setCreatedAt(medicine.getCreatedAt());
        return vo;
    }
}

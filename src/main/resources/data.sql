DELETE FROM payment_record;
DELETE FROM order_item;
DELETE FROM pharmacy_order;
DELETE FROM cart_item;
DELETE FROM ai_consultation;
DELETE FROM medicine;
DELETE FROM medicine_category;

INSERT INTO medicine_category (id, name, description, sort_order, status) VALUES
(1, '感冒用药', '发热、鼻塞、咽痛等常见感冒症状用药', 1, 1),
(2, '维生素类', '维生素及矿物质补充剂', 2, 1),
(3, '肠胃用药', '胃痛、腹胀、腹泻等肠胃不适用药', 3, 1),
(4, '处方药', '需凭处方或药师审核后购买的药品', 4, 1);

INSERT INTO medicine
(id, name, category_id, specification, price, stock, manufacturer, description, contraindication, notice, otc_flag, prescription_required, image_url, status, created_at)
VALUES
(1, '连花清瘟胶囊', 1, '0.35g*24粒/盒', 29.80, 126, '石家庄以岭药业股份有限公司', '用于流行性感冒属热毒袭肺证，缓解发热、恶寒、肌肉酸痛、鼻塞流涕等症状。', '对本品成分过敏者禁用。风寒感冒者不适用。', '服药期间忌烟酒及辛辣、生冷、油腻食物。高血压、心脏病患者慎用。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(2, '999感冒灵颗粒', 1, '10g*9袋/盒', 18.50, 88, '华润三九医药股份有限公司', '解热镇痛，用于感冒引起的头痛、发热、鼻塞、流涕、咽痛。', '严重肝肾功能不全者禁用。对本品过敏者禁用。', '服用期间不得饮酒，不宜同时服用成分相似的其他抗感冒药。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(3, '布洛芬缓释胶囊', 1, '0.3g*20粒/盒', 22.90, 34, '中美天津史克制药有限公司', '用于缓解轻至中度疼痛，也可用于普通感冒或流行性感冒引起的发热。', '活动期消化道溃疡、孕妇及哺乳期妇女禁用。', '请按说明书剂量服用，连续用于止痛不得超过5天，用于解热不得超过3天。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(4, '维生素C咀嚼片', 2, '100mg*60片/瓶', 16.80, 210, '东北制药集团沈阳第一制药有限公司', '用于预防坏血病，也可用于各种急慢性传染疾病及紫癜等辅助治疗。', '对本品过敏者禁用。草酸盐结石患者慎用。', '长期过量服用可能引起尿酸盐、半胱氨酸盐或草酸盐结石。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(5, '钙尔奇碳酸钙D3片', 2, '60片/瓶', 58.00, 72, '惠氏制药有限公司', '用于妊娠和哺乳期妇女、更年期妇女、老年人等钙补充，并帮助防治骨质疏松症。', '高钙血症、高尿酸血症患者禁用。', '心肾功能不全者慎用，服用洋地黄类药物期间请咨询医生。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(6, '蒙脱石散', 3, '3g*10袋/盒', 19.90, 46, '博福-益普生制药有限公司', '用于成人及儿童急、慢性腹泻。', '对本品过敏者禁用。便秘患者慎用。', '治疗急性腹泻时应注意纠正脱水，儿童用药请遵医嘱。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(7, '奥美拉唑肠溶胶囊', 3, '20mg*14粒/盒', 25.60, 18, '阿斯利康制药有限公司', '用于胃溃疡、十二指肠溃疡、反流性食管炎等胃酸相关疾病。', '对奥美拉唑或其他苯并咪唑类药物过敏者禁用。', '症状持续或反复发作应及时就医，长期使用需遵医嘱。', true, false, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(8, '阿莫西林胶囊', 4, '0.25g*24粒/盒', 13.80, 12, '哈药集团制药总厂', '适用于敏感菌所致的呼吸道、泌尿生殖道、皮肤软组织等感染。', '青霉素过敏及青霉素皮肤试验阳性患者禁用。', '本品为处方药，需凭处方并经药师审核后购买。用药前应确认过敏史。', false, true, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP),
(9, '硝苯地平控释片', 4, '30mg*7片/盒', 31.50, 9, '拜耳医药保健有限公司', '用于高血压、冠心病慢性稳定型心绞痛的治疗。', '对硝苯地平过敏、心源性休克患者禁用。', '本品为处方药，应在医生指导下使用，不可自行停药或调整剂量。', false, true, '/img/medicine-default.svg', 1, CURRENT_TIMESTAMP);

package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.qc.dto.QcInspectionForm;
import ai.toafrica.agrios.qc.entity.QcInspection;
import ai.toafrica.agrios.qc.entity.QcInspectionItem;
import ai.toafrica.agrios.qc.mapper.QcInspectionItemMapper;
import ai.toafrica.agrios.qc.mapper.QcInspectionMapper;
import ai.toafrica.agrios.qc.vo.QcInspectionDetailVO;
import ai.toafrica.agrios.qc.vo.QcInspectionItemVO;
import ai.toafrica.agrios.qc.vo.QcInspectionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QcInspectionService {

    private final QcInspectionMapper inspMapper;
    private final QcInspectionItemMapper itemMapper;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PageResult<QcInspectionVO> page(String inspectionType, String result, String refType,
                                           LocalDate dateFrom, LocalDate dateTo, PageQuery pq) {
        QueryWrapper<QcInspectionVO> q = new QueryWrapper<>();
        if (inspectionType != null && !inspectionType.isBlank()) q.eq("q.inspection_type", inspectionType.trim());
        if (result != null && !result.isBlank())                 q.eq("q.result", result.trim());
        if (refType != null && !refType.isBlank())               q.eq("q.ref_type", refType.trim());
        if (dateFrom != null)                                    q.ge("q.inspect_date", dateFrom);
        if (dateTo != null)                                      q.le("q.inspect_date", dateTo);
        q.orderByDesc("q.inspect_date").orderByDesc("q.id");
        return PageResult.of(inspMapper.pageWithJoin(new Page<>(pq.getPage(), pq.getSize()), q));
    }

    public QcInspectionDetailVO detail(Long id) {
        QueryWrapper<QcInspectionVO> q = new QueryWrapper<>();
        q.eq("q.id", id);
        var records = inspMapper.pageWithJoin(new Page<>(1, 1), q).getRecords();
        if (records.isEmpty()) throw new BusinessException(R.NOT_FOUND, "QC inspection not found");
        QcInspectionDetailVO vo = new QcInspectionDetailVO();
        vo.setHeader(records.get(0));
        List<QcInspectionItemVO> items = itemMapper.findByInspectionId(id);
        vo.setItems(items);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(QcInspectionForm form, Long inspectorId) {
        String today = LocalDate.now().format(YMD);
        int seq = inspMapper.countByDate(today) + 1;
        String code = String.format("QC-%s-%04d", today, seq);

        QcInspection q = new QcInspection();
        BeanUtils.copyProperties(form, q);
        q.setCode(code);
        q.setInspectorId(inspectorId);
        if (q.getResult() == null || q.getResult().isBlank()) q.setResult("pending");
        inspMapper.insert(q);

        saveItems(q.getId(), form.getItems());
        log.info("[QC created] code={} type={} ref={}:{} items={}",
                code, form.getInspectionType(), form.getRefType(), form.getRefId(),
                form.getItems() != null ? form.getItems().size() : 0);
        return q.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, QcInspectionForm form) {
        QcInspection q = inspMapper.selectById(id);
        if (q == null) throw new BusinessException(R.NOT_FOUND, "QC inspection not found");
        BeanUtils.copyProperties(form, q, "id", "code", "createdAt", "createdBy", "inspectorId");
        inspMapper.updateById(q);
        saveItems(id, form.getItems());
    }

    private void saveItems(Long inspectionId, List<QcInspectionForm.Item> items) {
        itemMapper.delete(new LambdaQueryWrapper<QcInspectionItem>().eq(QcInspectionItem::getInspectionId, inspectionId));
        if (items == null) return;
        for (QcInspectionForm.Item it : items) {
            if (it.getCheckPoint() == null || it.getCheckPoint().isBlank()) continue;
            QcInspectionItem entity = new QcInspectionItem();
            entity.setInspectionId(inspectionId);
            entity.setCheckPoint(it.getCheckPoint());
            entity.setExpectedValue(it.getExpectedValue());
            entity.setActualValue(it.getActualValue());
            entity.setResult(it.getResult() != null && !it.getResult().isBlank() ? it.getResult() : "pending");
            entity.setRemark(it.getRemark());
            itemMapper.insert(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        itemMapper.delete(new LambdaQueryWrapper<QcInspectionItem>().eq(QcInspectionItem::getInspectionId, id));
        inspMapper.deleteById(id);
    }
}

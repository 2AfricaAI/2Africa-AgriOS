package ai.toafrica.agrios.production.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.production.entity.Batch;
import ai.toafrica.agrios.production.mapper.BatchMapper;
import ai.toafrica.agrios.production.vo.BatchVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchMapper batchMapper;

    private static final Set<String> ALL_STATUS = Set.of("pending", "processing", "packed", "sold_out", "lost");

    public PageResult<BatchVO> page(Long plotId, Long planId, String status,
                                    LocalDate dateFrom, LocalDate dateTo,
                                    String code, PageQuery pq) {
        QueryWrapper<BatchVO> q = new QueryWrapper<>();
        q.isNull("b.deleted_at");
        if (plotId != null) q.eq("b.plot_id", plotId);
        if (planId != null) q.eq("b.plan_id", planId);
        if (status != null && !status.isBlank()) q.eq("b.status", status.trim());
        if (code != null && !code.isBlank()) q.like("b.code", code.trim());
        if (dateFrom != null) q.ge("b.harvest_date", dateFrom);
        if (dateTo != null) q.le("b.harvest_date", dateTo);
        q.orderByDesc("b.harvest_date").orderByDesc("b.id");

        Page<BatchVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(batchMapper.pageWithJoin(p, q));
    }

    public BatchVO detail(Long id) {
        QueryWrapper<BatchVO> q = new QueryWrapper<>();
        q.isNull("b.deleted_at");
        q.eq("b.id", id);
        Page<BatchVO> p = new Page<>(1, 1);
        var pageData = batchMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "批次不存在");
        }
        return pageData.getRecords().get(0);
    }

    public void changeStatus(Long id, String status) {
        if (!ALL_STATUS.contains(status)) {
            throw new BusinessException("status 必须是 " + ALL_STATUS);
        }
        Batch b = batchMapper.selectById(id);
        if (b == null) throw new BusinessException(R.NOT_FOUND, "批次不存在");
        b.setStatus(status);
        batchMapper.updateById(b);
    }
}

package ai.toafrica.agrios.sales.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.sales.dto.CustomerForm;
import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.sales.mapper.CustomerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;

    public PageResult<Customer> page(String keyword, String type, String status, PageQuery pq) {
        LambdaQueryWrapper<Customer> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(Customer::getName, k)
                       .or().like(Customer::getCode, k)
                       .or().like(Customer::getContactName, k)
                       .or().like(Customer::getContactPhone, k));
        }
        if (type != null && !type.isBlank()) q.eq(Customer::getType, type.trim());
        if (status != null && !status.isBlank()) q.eq(Customer::getStatus, status.trim());
        q.orderByDesc(Customer::getId);

        Page<Customer> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(customerMapper.selectPage(p, q));
    }

    public Customer detail(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        return c;
    }

    /** 创建 - 自动生成 code = CUS-NNNNN */
    public Long create(CustomerForm form) {
        Customer c = new Customer();
        BeanUtils.copyProperties(form, c);
        c.setCode(nextCode());
        c.setStatus("active");
        if (c.getSinceDate() == null) c.setSinceDate(LocalDate.now());
        customerMapper.insert(c);
        log.info("[Customer created] id={} code={} name={}", c.getId(), c.getCode(), c.getName());
        return c.getId();
    }

    public void update(Long id, CustomerForm form) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        BeanUtils.copyProperties(form, c);
        customerMapper.updateById(c);
    }

    /** 启用 / 停用 (active / inactive) */
    public void changeStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new BusinessException("status must be active or inactive");
        }
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        c.setStatus(status);
        customerMapper.updateById(c);
    }

    /** 软删 */
    public void delete(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Customer not found");
        customerMapper.deleteById(id);
    }

    /** 自动 CUS-NNNNN, 取当前最大 + 1 */
    private String nextCode() {
        int next = customerMapper.maxCodeSeq() + 1;
        return String.format("CUS-%05d", next);
    }
}

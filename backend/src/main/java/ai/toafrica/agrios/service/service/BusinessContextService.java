package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.sales.entity.Customer;
import ai.toafrica.agrios.service.spi.impl.AgriOsBusinessContextProvider;
import ai.toafrica.agrios.service.vo.ConversationDetailVO.BusinessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @deprecated Sprint 48a — superseded by the CS-Core SPI
 * {@link ai.toafrica.agrios.service.spi.BusinessContextProvider} and its
 * AgriOS implementation {@link AgriOsBusinessContextProvider}.
 *
 * <p>This thin wrapper is kept for source-level backward compatibility with
 * any callers still importing the old type. New code should depend on the
 * SPI interface (so future products' implementations are pluggable). The
 * wrapper will be removed in a follow-up sprint once all callers have
 * migrated.</p>
 */
@Deprecated(forRemoval = true, since = "v3.3.0")
@Service
@RequiredArgsConstructor
public class BusinessContextService {

    private final AgriOsBusinessContextProvider delegate;

    /**
     * @deprecated Inject {@code BusinessContextProvider} (the SPI) or
     * {@code AgriOsBusinessContextProvider} (the AgriOS impl) directly
     * instead of this wrapper.
     */
    @Deprecated(forRemoval = true, since = "v3.3.0")
    public BusinessContext forCustomer(Customer customer) {
        return delegate.forCustomer(customer);
    }
}

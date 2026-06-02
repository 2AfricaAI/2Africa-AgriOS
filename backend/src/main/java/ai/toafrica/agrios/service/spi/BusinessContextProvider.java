package ai.toafrica.agrios.service.spi;

import ai.toafrica.agrios.service.vo.ConversationDetailVO;

/**
 * CS-Core SPI (Service Provider Interface).
 *
 * <p>Each consuming product (AgriOS / RetailOS / FactoryOS / TravelOS /
 * AgriCloud / MarketOS) implements this interface to plug its own business
 * entity (Customer / Buyer / Traveler / ...) into the CS-Core conversation
 * workspace.</p>
 *
 * <p>The CS-Core layer (Sprint 40-48) only knows about {@code subject_type}
 * and {@code subject_id} from {@code cs_contact_link}. When the conversation
 * detail endpoint resolves a link, it asks the implementation to produce a
 * {@link ConversationDetailVO.BusinessContext} for the agent's sidebar
 * (open orders / AR / complaints / etc.).</p>
 *
 * <p>Sprint 48a introduced this interface so the {@code service/*} package
 * can be lifted into a horizontal CS-Core module that other 2Africa products
 * adopt as-is. AgriOS ships
 * {@link ai.toafrica.agrios.service.spi.impl.AgriOsBusinessContextProvider}
 * as its implementation.</p>
 */
public interface BusinessContextProvider {

    /**
     * Returns the {@code subject_type} value this provider handles. Examples:
     * {@code "customer"} (AgriOS / AgriCloud), {@code "buyer"} (RetailOS),
     * {@code "traveler"} (TravelOS), {@code "client"} (FactoryOS).
     *
     * <p>Multiple providers can co-exist in one app context; the controller
     * dispatches based on this key.</p>
     */
    String subjectType();

    /**
     * Compute the business-context block for a given {@code subject_id}. The
     * subject id is the value from {@code cs_contact_link.subject_id}.
     *
     * <p>Implementations should return an empty (not null) builder result
     * when the subject doesn't exist or has nothing to report — the UI
     * gracefully renders zeros / dashes.</p>
     */
    ConversationDetailVO.BusinessContext forSubject(Long subjectId);
}

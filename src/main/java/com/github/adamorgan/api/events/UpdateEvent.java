package com.github.adamorgan.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Indicates that a value of an entity was updated
 *
 * @param <E>
 *        The entity type
 * @param <T>
 *        The value type
 */
public interface UpdateEvent<E, T> extends GenericEvent
{
    /**
     * The field name for the updated property
     *
     * <p><b>Example</b><br>
     * <pre><code>
     * {@literal @Override}
     * public void onGenericRoleUpdate(GenericRoleUpdateEvent event)
     * {
     *     switch (event.getPropertyIdentifier())
     *     {
     *     case RoleUpdateColorEvent.IDENTIFIER:
     *         System.out.printf("Updated color for role: %s%n", event);
     *         break;
     *     case RoleUpdatePositionEvent.IDENTIFIER:
     *         RoleUpdatePositionEvent update = (RoleUpdatePositionEvent) event;
     *         System.out.printf("Updated position for role: %s raw(%s{@literal ->}%s)%n", event, update.getOldPositionRaw(), update.getNewPositionRaw());
     *         break;
     *     default: return;
     *     }
     * }
     * </code></pre>
     *
     * @return The name of the updated property
     */
    @Nonnull
    String getPropertyIdentifier();

    /**
     * The affected entity
     *
     * @return The affected entity
     */
    @Nonnull
    E getEntity();

    /**
     * The old value
     *
     * @return The old value
     */
    @Nullable
    T getOldValue();

    /**
     * The new value
     *
     * @return The new value
     */
    @Nullable
    T getNewValue();
}

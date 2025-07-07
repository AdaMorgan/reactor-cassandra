package com.github.assertions.objectaction;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.internal.requests.Requester;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.mockito.ThrowingConsumer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.doNothing;

public class ObjectActionAssertions implements ThrowingConsumer<Request<?>>
{
    private final SnapshotHandler snapshotHandler;
    private final ObjectAction<?> action;
    private final List<ThrowingConsumer<Request<?>>> assertions = new ArrayList<>();
    private Consumer<? super ByteBuf> normalizeRequestBody = (v) ->
    {
    };

    public ObjectActionAssertions(SnapshotHandler snapshotHandler, ObjectAction<?> action)
    {
        this.snapshotHandler = snapshotHandler;
        this.action = action;
    }

    @CheckReturnValue
    public static ObjectActionAssertions assertThatNextAction(SnapshotHandler snapshotHandler, Requester requester, ObjectAction<?> action)
    {
        ObjectActionAssertions assertions = new ObjectActionAssertions(snapshotHandler, action);
        doNothing().when(requester).request(assertArg(assertions::acceptThrows));
        return assertions;
    }

    public void whenQueueCalled()
    {
        action.queue();
    }

    @CheckReturnValue
    @Contract("_->this")
    public ObjectActionAssertions withNormalizedBody(@Nonnull Consumer<? super ByteBuf> normalizer)
    {
        this.normalizeRequestBody = normalizer;
        return this;
    }

    @CheckReturnValue
    @Contract("_->this")
    public ObjectActionAssertions checkAssertions(@Nonnull ThrowingConsumer<Request<?>> assertion)
    {
        assertions.add(assertion);
        return this;
    }

    @CheckReturnValue
    @Contract("_->this")
    public ObjectActionAssertions hasBodyEqualTo(@Nonnull ByteBuf expected)
    {
        return checkAssertions(request ->
        {
            ByteBuf ByteBuf = getRequestBody(request);
            normalizeRequestBody.accept(expected);

            assertThat(ByteBuf.toString()).as("ObjectAction should send request using expected request body").isEqualTo(expected.toString());
        });
    }

    @CheckReturnValue
    @Contract("_->this")
    public ObjectActionAssertions hasBodyMatching(@Nonnull Predicate<? super ByteBuf> condition)
    {
        return checkAssertions(request ->
        {
            ByteBuf body = getRequestBody(request);
            assertThat(body).withRepresentation(new PrettyRepresentation()).matches(condition);
        });
    }

    @CheckReturnValue
    @Contract("->this")
    public ObjectActionAssertions hasBodyMatchingSnapshot()
    {
        return hasBodyMatchingSnapshot(null);
    }

    @CheckReturnValue
    @Contract("_->this")
    public ObjectActionAssertions hasBodyMatchingSnapshot(String suffix)
    {
        return checkAssertions(request ->
        {
            ByteBuf body = getRequestBody(request);
            snapshotHandler.compareWithSnapshot(body, suffix);
        });
    }

    @Override
    public void acceptThrows(Request<?> request) throws Throwable
    {
        for (ThrowingConsumer<Request<?>> assertion : assertions)
        {
            assertion.acceptThrows(request);
        }
    }

    @Nonnull
    private ByteBuf getRequestBody(@Nonnull Request<?> request)
    {
        ByteBuf body = request.getBody();
        assertThat(body).isNotNull().isInstanceOf(ByteBuf.class);

        normalizeRequestBody.accept(body);
        return body;
    }
}

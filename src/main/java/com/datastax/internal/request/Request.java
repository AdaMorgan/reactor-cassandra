package com.datastax.internal.request;

import com.datastax.api.audit.ThreadLocalReason;
import com.datastax.internal.ObjectFactoryImpl;
import com.datastax.internal.objectaction.ObjectActionImpl;

import java.util.function.Consumer;

public final class Request<T>
{
    private final ObjectFactoryImpl objectFactory;
    private final ObjectActionImpl<T> action;
    private final Consumer<? super T> onSuccess;
    private final Consumer<? super Throwable> onFailure;
    private final String route;
    private final String localReason;

    public Request(ObjectActionImpl<T> action, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure)
    {
        this.objectFactory = (ObjectFactoryImpl) action.getObjectFactory();
        this.action = action;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.route = action.finalizeRoute();
        this.localReason = ThreadLocalReason.getCurrent();
    }

    public void onSuccess(T successObj)
    {
        ObjectActionImpl.LOG.trace("Scheduling success callback for request with route {}", this.route);
        this.objectFactory.getCallbackPool().execute(() ->
        {
            try (ThreadLocalReason.Closable closable = ThreadLocalReason.closable(localReason);
                 CallbackContext context = CallbackContext.getInstance())
            {
                ObjectActionImpl.LOG.trace("Running success callback for request with route {}", this.route);
                onSuccess.accept(successObj);
            }
            catch (Throwable error)
            {
                ObjectActionImpl.LOG.error("Encountered error while processing success consumer", error);
                if (error instanceof Error)
                {
                    throw (Error) error;
                }
            }
        });
    }

    public void onFailure(Throwable failException)
    {
        ObjectActionImpl.LOG.trace("Scheduling failure callback for request with route {}", this.route);
        this.objectFactory.getCallbackPool().execute(() ->
        {
            try (ThreadLocalReason.Closable __ = ThreadLocalReason.closable(localReason);
                 CallbackContext ___ = CallbackContext.getInstance())
            {
                ObjectActionImpl.LOG.trace("Running failure callback for request with route {}", this.route);
                onFailure.accept(failException);
                if (failException instanceof Error) {

                }
            }
            catch (Throwable error)
            {
                ObjectActionImpl.LOG.error("Encountered error while processing failure consumer", error);
                if (error instanceof Error)
                {
                    throw (Error) error;
                }
            }
        });
    }

    public void handleResponse(Response response)
    {
        ObjectActionImpl.LOG.trace("Handling response for request with route {}", this.route);
        this.action.handleSuccess(this, response);
    }

    public String getRoute() {
        return route;
    }
}

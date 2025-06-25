/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.binary.BinaryRequestEvent;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.CallbackContext;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class Request<T>
{
    protected final LibraryImpl api;
    protected final ObjectActionImpl<T> objAction;
    protected final ByteBuf body;
    protected final Consumer<? super T> onSuccess;
    protected final Consumer<? super Throwable> onFailure;
    protected final long deadline;

    protected boolean done = false;
    protected boolean isCancelled = false;

    public Request(ObjectActionImpl<T> objAction, ByteBuf body, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure, long deadline)
    {
        this.objAction = objAction;
        this.body = body;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.deadline = deadline;

        this.api = (LibraryImpl) this.objAction.getLibrary();
    }

    @Nonnull
    public LibraryImpl getLibrary()
    {
        return api;
    }

    @Nonnull
    @CheckReturnValue
    public ObjectAction<T> getObjectAction()
    {
        return objAction;
    }

    @Nonnull
    public ByteBuf getBody()
    {
        return body;
    }

    public void cancel()
    {
        if (!this.isCancelled)
            onCancelled();
        this.isCancelled = true;
    }

    public void onSuccess(T successObj)
    {
        if (done)
        {
            return;
        }
        done = true;
        ObjectActionImpl.LOG.trace("Scheduling success callback for request");
        try (CallbackContext ___ = CallbackContext.getInstance())
        {
            ObjectActionImpl.LOG.trace("Running success callback for request");
            onSuccess.accept(successObj);
        }
        catch (Throwable t)
        {
            ObjectActionImpl.LOG.error("Encountered error while processing success consumer", t);
            if (t instanceof Error)
            {
                api.handleEvent(new ExceptionEvent(api, t, true));
                throw (Error) t;
            }
        }
    }

    public void onFailure(Throwable failException)
    {
        if (done)
        {
            return;
        }
        done = true;
        try (CallbackContext ___ = CallbackContext.getInstance())
        {
            ObjectActionImpl.LOG.trace("Running failure callback for request");
            onFailure.accept(failException);
            if (failException instanceof Error)
                api.handleEvent(new ExceptionEvent(api, failException, false));
        }
        catch (Throwable failure)
        {
            ObjectActionImpl.LOG.error("Encountered error while processing failure consumer", failure);
            if (failure instanceof Error)
            {
                api.handleEvent(new ExceptionEvent(api, failure, true));
                throw (Error) failure;
            }
        }
    }

    public void onCancelled()
    {
        onFailure(new CancellationException("RestAction has been cancelled"));
    }

    public void onTimeout()
    {
        this.onFailure(new TimeoutException("ObjectAction has timed out"));
    }

    public boolean isSkipped()
    {
        if (isCancelled())
        {
            onCancelled();
        }
        return isCancelled();
    }

    public boolean isCancelled()
    {
        return isCancelled;
    }

    public void handleResponse(Response response)
    {
        ObjectActionImpl.LOG.trace("Handling response for request with content {}", "");
        this.objAction.handleResponse(this, response);
        api.handleEvent(new BinaryRequestEvent(this, response));
    }
}

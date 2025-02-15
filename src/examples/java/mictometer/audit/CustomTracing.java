package mictometer.audit;

import brave.propagation.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class CustomTracing extends CurrentTraceContext
{

}

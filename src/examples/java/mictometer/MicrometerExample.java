package mictometer;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

public class MicrometerExample
{
    private final SimpleMeterRegistry simpleMeterRegistry;

    private MicrometerExample()
    {
        this.simpleMeterRegistry = new SimpleMeterRegistry();
    }

    public static void main(String[] args)
    {
        Timer timer = Metrics.timer("timer");

        Runnable task = () ->
        {
            for (int i = 0; i < 100000; i++) {
                System.out.println(i);
            }
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        };

        timer.record(task);
        System.out.println(timer.totalTime(TimeUnit.MILLISECONDS));
    }

    /**
     * This setup is based on
     * <a href="https://micrometer.io/docs/tracing#_micrometer_tracing_brave_setup">Micrometer Tracing Brave Setup</a>.
     */
    public void tracingBuilder()
    {
    }
}

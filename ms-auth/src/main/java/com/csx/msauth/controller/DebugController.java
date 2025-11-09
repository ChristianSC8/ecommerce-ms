package com.csx.msauth.controller;

import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DebugController {

    private final Counter testCounter;
    private final Tracer tracer;

    public DebugController(MeterRegistry meterRegistry, Tracer tracer) {
        this.testCounter = meterRegistry.counter("app.debug.counter");
        this.tracer = tracer;
    }

    @GetMapping("/debug/telemetry")
    public String debugTelemetry() {
        // 1. Crear un span manualmente
        Span span = tracer.spanBuilder("debug-telemetry-span").startSpan();

        try {
            // 2. Hacer el span actual
            span.setAttribute("debug.test", "true");
            span.setAttribute("service.name", "ms-auth");

            // 3. Generar LOG con traceId (debería aparecer automáticamente)
            log.info("✅ LOG TEST: Este mensaje debería tener traceId y spanId");

            // 4. Generar MÉTRICA
            testCounter.increment();
            log.info("📊 MÉTRICA: Contador incrementado");

            // 5. Simular trabajo
            Thread.sleep(100);

            return """
                🔍 DEBUG TELEMETRY - Revisa:
                1. LOGS: Este mensaje en consola con [trace-id,span-id]
                2. MÉTRICAS: app.debug.counter en /actuator/metrics
                3. TRAZAS: Span 'debug-telemetry-span' generado
                """;

        } catch (Exception e) {
            log.error("❌ ERROR TEST", e);
            return "Error en test";
        } finally {
            // 6. Finalizar el span
            span.end();
            log.info("🎯 SPAN: Span finalizado");
        }
    }

    @GetMapping("/debug/metrics")
    public String checkMetrics() {
        log.info("🔍 Revisando métricas...");
        return """
            📊 MÉTRICAS DISPONIBLES:
            - http://localhost:42201/actuator/metrics
            - http://localhost:42201/actuator/prometheus
            """;
    }
}
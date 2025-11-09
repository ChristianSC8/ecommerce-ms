package com.csx.msauth.config;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtelLogsInit {

    @Value("${spring.application.name}")
    private String serviceName;

    @PostConstruct
    public void init() {
        Resource resource = Resource.create(
                Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName
                )
        );

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(
                                OtlpHttpLogRecordExporter.builder()
                                        .setEndpoint("http://localhost:4318/v1/logs")
                                        .build()
                        ).build()
                )
                .build();

        OpenTelemetrySdk.builder()
                .setLoggerProvider(loggerProvider)
                .buildAndRegisterGlobal();
    }
}
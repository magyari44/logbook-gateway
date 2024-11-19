package com.logbook.async.logbookdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HeaderFilters;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.StructuredHttpLogFormatter;

import java.io.IOException;
import java.util.Map;


@Configuration
public class DemoLogbook {

    private static final Logger logger = LoggerFactory.getLogger(DemoLogbook.class);

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .headerFilter(HeaderFilters.replaceHeaders("Authorization", "xxx"))
                .sink(new DefaultSink(new TraceIdStructuredJsonLogFormatter(), new CustomHttpLogWriter()))
                .build();
    }

    private static class CustomHttpLogWriter implements HttpLogWriter {

        @Override
        public void write(final Precorrelation precorrelation, final String content) {
            logger.info(content);
        }

        @Override
        public void write(final Correlation correlation, final String content) {
            logger.info(content);
        }
    }

    public class TraceIdStructuredJsonLogFormatter implements StructuredHttpLogFormatter {

        private final ObjectMapper mapper;

        public TraceIdStructuredJsonLogFormatter() {
            this.mapper = new ObjectMapper();
        }

        @Override
        public String format(Map<String, Object> content) throws IOException {
            return this.mapper.writeValueAsString(content);
        }

        @Override
        public Map<String, Object> prepare(Precorrelation precorrelation, HttpRequest request) throws IOException {
            return StructuredHttpLogFormatter.super.prepare(precorrelation, request);
        }


        @Override
        public Map<String, Object> prepare(Correlation correlation, HttpResponse response) throws IOException {
            return StructuredHttpLogFormatter.super.prepare(correlation, response);
        }
    }
}

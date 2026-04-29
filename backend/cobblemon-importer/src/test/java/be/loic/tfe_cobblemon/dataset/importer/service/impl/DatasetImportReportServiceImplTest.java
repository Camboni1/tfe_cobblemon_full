package be.loic.tfe_cobblemon.dataset.importer.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasetImportReportServiceImplTest {

//    @Test
//    void logFinalReport_usesJsonFieldLookupForSyntheticItemDetection() {
//        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
//        List<String> executedSql = new ArrayList<>();
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
//                .thenAnswer(invocation -> {
//                    executedSql.add(invocation.getArgument(0, String.class));
//                    return Boolean.FALSE;
//                });
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(Timestamp.class), any(Object[].class)))
//                .thenAnswer(invocation -> {
//                    executedSql.add(invocation.getArgument(0, String.class));
//                    return Timestamp.from(Instant.parse("2026-04-14T18:00:00Z"));
//                });
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
//                .thenAnswer(invocation -> {
//                    executedSql.add(invocation.getArgument(0, String.class));
//                    return 0L;
//                });
//
//        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
//                .thenAnswer(invocation -> {
//                    executedSql.add(invocation.getArgument(0, String.class));
//                    return Map.of();
//                });
//
//        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
//                .thenAnswer(invocation -> {
//                    executedSql.add(invocation.getArgument(0, String.class));
//                    return List.of();
//                });
//
//        DatasetImportReportServiceImpl service = new DatasetImportReportServiceImpl(jdbcTemplate);
//
//        service.logFinalReport(
//                1L,
//                "cobblemon-test",
//                "Cobblemon Test",
//                "import-data/test",
//                false,
//                Instant.parse("2026-04-14T17:00:00Z")
//        );
//
//        assertThat(executedSql)
//                .anyMatch(sql -> sql.contains("raw_json ->> 'source'"))
//                .anyMatch(sql -> sql.contains("i.raw_json ->> 'source'"))
//                .noneMatch(sql -> sql.contains("raw_json LIKE")
//                        || sql.contains("raw_json NOT LIKE")
//                        || sql.contains("i.raw_json LIKE")
//                        || sql.contains("i.raw_json NOT LIKE"));
//    }
}

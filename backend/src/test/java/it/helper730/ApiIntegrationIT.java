package it.helper730;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * ApiIntegrationIT.
 *
 * The whole application (controllers, services, JPA, Flyway) on a real
 * PostgreSQL, used through HTTP like the frontend does.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiIntegrationIT {

    private static final String API = "/api/v1";
    private static final String HEADER = "Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Every test starts from an empty database; the schema stays, it belongs to
     * Flyway.
     */
    @BeforeEach
    void emptyDatabase() {
        jdbc.execute("TRUNCATE transactions, import_batches, year_end_prices, carried_losses, instruments "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void goldenCsvGivesTheExpectedTaxThroughTheWholeApplication() throws Exception {
        upload(golden())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(11))
                .andExpect(jsonPath("$.duplicates").value(0))
                .andExpect(jsonPath("$.issues", hasSize(1)))
                .andExpect(jsonPath("$.issues[0].file_row").value(8))
                .andExpect(jsonPath("$.issues[0].code").value("warning.implied_fee"));

        classify("5J50", "STOCK");
        for (String ticker : List.of("VGWD", "CEBS", "EUNK", "XEON")) {
            classify(ticker, "UCITS_ETF");
        }
        putVgwdPrice(2025, "70");

        mvc.perform(get(API + "/tax/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tax").value("1.84"))
                .andExpect(jsonPath("$.capital_taxable").value("7.07"))
                .andExpect(jsonPath("$.below_threshold").value(true))
                .andExpect(jsonPath("$.economic_result").value("-11.67"))
                .andExpect(jsonPath("$.zainetto.closing_total").value("16.9"))
                .andExpect(jsonPath("$.zainetto.realized_loss.last_usable_year").value(2030));

        mvc.perform(get(API + "/tax/2025/ivafe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("0.09"))
                .andExpect(jsonPath("$.complete").value(true));

        mvc.perform(get(API + "/portfolio"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.positions[?(@.ticker == 'XEON')].quantity").value(hasItem("0.00559597")));
    }

    @Test
    void reimportingTheSameFileAddsNothing() throws Exception {
        upload(golden()).andExpect(status().isCreated());

        upload(golden())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.duplicates").value(11));

        mvc.perform(get(API + "/imports")).andExpect(jsonPath("$", hasSize(2)));
        assertThat(count("transactions")).isEqualTo(11);
    }

    @Test
    void taxNeedsClassifiedInstruments() throws Exception {
        upload(golden());

        mvc.perform(get(API + "/tax/2026"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNCLASSIFIED_INSTRUMENTS"))
                .andExpect(jsonPath("$.tickers", hasItems("5J50", "XEON")))
                .andExpect(jsonPath("$.detail", containsString("classifica")));

        mvc.perform(get(API + "/tax/2026").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Classify")));

        // the year menu still works: the error becomes part of the summary
        mvc.perform(get(API + "/tax/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].error_code").value("UNCLASSIFIED_INSTRUMENTS"));
    }

    @Test
    void invalidClassificationIsRejectedWith400() throws Exception {
        upload(golden());

        mvc.perform(put(API + "/instruments/VGWD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asset_class\":\"UCITS_ETF\",\"foreign_withholding_rate\":0.15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.message_code").value("error.withholding_not_allowed"));
    }

    @Test
    void replacingPricesKeepsOneRowPerTicker() throws Exception {
        upload(golden());

        putVgwdPrice(2025, "70");
        putVgwdPrice(2025, "71");

        mvc.perform(get(API + "/prices/2025"))
                .andExpect(jsonPath("$.prices.VGWD").value("71"));
        assertThat(count("year_end_prices")).isEqualTo(1);
    }

    @Test
    void taxYearFollowsItalianTime() throws Exception {
        // 23:30 UTC on 31 December 2025 is 00:30 on 1 January 2026 in Italy
        String csv = HEADER + "\n2025-12-31T23:30:00.000Z,VGWD,BUY - MARKET,1,EUR 70.00,EUR 70.00,EUR\n";
        upload(csv.getBytes(UTF_8), "new-year.csv")
                .andExpect(jsonPath("$.imported").value(1));

        mvc.perform(get(API + "/transactions").param("year", "2026"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tax_year").value(2026));
        mvc.perform(get(API + "/transactions").param("year", "2025")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteAllRemovesTransactionsHistoryAndUnusedInstruments() throws Exception {
        upload(golden());

        mvc.perform(delete(API + "/transactions").param("instruments", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").value(11))
                .andExpect(jsonPath("$.imports").value(1))
                .andExpect(jsonPath("$.instruments").value(5));

        mvc.perform(get(API + "/instruments")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(API + "/imports")).andExpect(jsonPath("$", hasSize(0)));
    }

    // --- helpers ------------------------------------------------------------

    private static byte[] golden() throws IOException {
        return new ClassPathResource("golden/revolut_golden.csv").getContentAsByteArray();
    }

    private ResultActions upload(byte[] csv) throws Exception {
        return upload(csv, "revolut_golden.csv");
    }

    private ResultActions upload(byte[] csv, String filename) throws Exception {
        return mvc.perform(multipart(API + "/imports").file(new MockMultipartFile("file", filename, "text/csv", csv)));
    }

    private void classify(String ticker, String assetClass) throws Exception {
        mvc.perform(put(API + "/instruments/" + ticker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asset_class\":\"" + assetClass + "\"}"))
                .andExpect(status().isOk());
    }

    private void putVgwdPrice(int year, String price) throws Exception {
        mvc.perform(put(API + "/prices/" + year)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prices\":{\"VGWD\":\"" + price + "\"}}"))
                .andExpect(status().isOk());
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}

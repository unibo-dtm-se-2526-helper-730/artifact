package it.helper730;

import it.helper730.domain.AssetClass;
import it.helper730.domain.Instrument;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The operations of revolut_golden.csv, written by hand, and their
 * classification.
 */
public final class GoldenData {

    private GoldenData() {}

    public static List<Transaction> operations() {
        return List.of(
                tx(3, "2025-10-13T06:00:44.187Z", "VGWD", OperationType.BUY, "2.83427915", "66.80", "189.33"),
                tx(4, "2026-01-05T17:52:54.885708Z", "VGWD", OperationType.DIVIDEND, null, null, "1.11"),
                tx(5, "2026-01-07T11:11:42.252Z", "VGWD", OperationType.BUY, "2.9440628", "71.33", "210"),
                tx(6, "2026-01-12T08:36:05.199Z", "5J50", OperationType.BUY, "47", "8.64", "407"),
                tx(7, "2026-01-16T13:29:07.734Z", "CEBS", OperationType.BUY, "20", "8.73", "174.62"),
                tx(8, "2026-01-19T07:45:18.487Z", "EUNK", OperationType.BUY, "2", "96.20", "193.40"),
                tx(9, "2026-02-27T17:20:41.244Z", "VGWD", OperationType.BUY, "5", "76.41", "382.05"),
                tx(10, "2026-04-07T15:42:19.811099Z", "VGWD", OperationType.DIVIDEND, null, null, "4"),
                tx(11, "2026-04-08T16:46:06.125Z", "XEON", OperationType.BUY, "4.90559597", "148.82", "730.04"),
                tx(12, "2026-05-27T07:44:19.205Z", "5J50", OperationType.SELL, "47", "8.30", "390.10"),
                tx(13, "2026-05-29T06:00:32.920Z", "XEON", OperationType.SELL, "4.9", "149.22", "729.33"));
    }

    /** All UCITS ETFs except 5J50, a stock; VGWD and XEON with their ISIN. */
    public static Map<String, Instrument> instruments() {
        Map<String, Instrument> instruments = new HashMap<>();
        instruments.put("VGWD", new Instrument("VGWD", "IE00B8GKDB10", null, AssetClass.UCITS_ETF, null, null));
        instruments.put("XEON", new Instrument("XEON", "LU0290358497", null, AssetClass.UCITS_ETF, null, null));
        instruments.put("CEBS", new Instrument("CEBS", null, null, AssetClass.UCITS_ETF, null, null));
        instruments.put("EUNK", new Instrument("EUNK", null, null, AssetClass.UCITS_ETF, null, null));
        instruments.put("5J50", new Instrument("5J50", null, null, AssetClass.STOCK, null, null));
        return instruments;
    }

    private static Transaction tx(
            int fileRow, String at, String ticker, OperationType type, String quantity, String price, String total) {
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(at),
                type,
                ticker,
                quantity == null ? null : new BigDecimal(quantity),
                price == null ? null : new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                fileRow);
    }
}

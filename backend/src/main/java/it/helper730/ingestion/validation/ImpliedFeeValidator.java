package it.helper730.ingestion.validation;

import it.helper730.domain.Money;
import it.helper730.domain.Msg;
import it.helper730.domain.Transaction;
import it.helper730.ingestion.RowIssue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * ImpliedFeeValidator
 *
 * Rejects trades with an implied fee that is too high, which is a sign of a
 * mis-typed quantity or price.
 */
public final class ImpliedFeeValidator implements TransactionValidator {

    /**
     * 0.5% of the trade value: above rounding, below what a commission-free broker
     * charges openly.
     */
    public static final BigDecimal THRESHOLD = BigDecimal.valueOf(0.05);

    @Override
    public List<RowIssue> validate(Transaction transaction) {
        if (!transaction.type().isTrade()) {
            return List.of();
        }
        BigDecimal fee = transaction.impliedFee();
        BigDecimal gross = transaction.quantity().multiply(transaction.pricePerShare());
        BigDecimal ratio = fee.abs().divide(gross, Money.CALC_SCALE, RoundingMode.HALF_UP);
        if (ratio.compareTo(THRESHOLD) <= 0) {
            return List.of();
        }

        String percent =
                ratio.movePointRight(2).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return List.of(
                RowIssue.warning(transaction.fileRow(), Msg.of("warning.implied_fee", fee.toPlainString(), percent)));
    }
}

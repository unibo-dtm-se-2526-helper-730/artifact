package it.helper730.domain.zainetto;

import java.math.BigDecimal;

/**
 * Part of a loss from the Zainetto used to offset the miscellaneous gains of a
 * year.
 *
 * @param lossYear year in which the loss used was realized
 * @param used     amount used, positive, in cents
 */
public record Compensation(int lossYear, BigDecimal used) {}

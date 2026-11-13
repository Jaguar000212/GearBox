package com.jaguar.gearbox.logic

import java.util.Locale

/** Loan payment summary, or null for invalid input or results outside finite Double range. */
fun computeLoanResult(loanAmount: Double, interestRate: Double, loanTerm: Double): String? {
    if (!loanAmount.isFinite() || !interestRate.isFinite() || !loanTerm.isFinite() ||
        loanAmount <= 0 || interestRate < 0 || loanTerm <= 0
    ) return null
    val monthlyInterestRate = interestRate / 100 / 12
    val tenure = loanTerm * 12
    if (!tenure.isFinite() || tenure <= 0) return null
    // At 0% interest, the standard EMI formula divides by (1+r)^n - 1 = 0, producing NaN.
    // A zero-interest loan is just the principal split evenly across the term.
    val emi = if (monthlyInterestRate == 0.0) {
        loanAmount / tenure
    } else {
        // Equivalent to r*(1+r)^n / ((1+r)^n-1), without an overflowing power
        // or cancellation when r is small.
        val denominator = -kotlin.math.expm1(-tenure * kotlin.math.ln1p(monthlyInterestRate))
        if (denominator == 0.0) return null
        loanAmount * (monthlyInterestRate / denominator)
    }

    val totalAmount = emi * tenure
    val totalInterest = totalAmount - loanAmount
    val interestPercentage = if (totalAmount == 0.0) 0.0 else totalInterest / totalAmount * 100

    if (!emi.isFinite() || emi <= 0 || !totalAmount.isFinite() || totalAmount <= 0 || !totalInterest.isFinite() ||
        !interestPercentage.isFinite()
    ) return null

    return String.format(
        Locale.US,
        "Monthly Installment: %.2f\nTotal Amount Repayable: %.2f\nTotal Interest Repayable: %.2f\nInterest Percentage: %.2f%%",
        emi, totalAmount, totalInterest, interestPercentage,
    )
}

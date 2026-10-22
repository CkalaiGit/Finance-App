package com.cairedine.finance.app.financialanalysis.domain;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Classe utilitaire finale pour les calculs mathématiques financiers.
 * Contient les méthodes statiques pour le calcul de métriques financières.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FinancialMath {

    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;


    /**
     * Calcule la croissance entre deux valeurs.
     *
     * @param current  la valeur courante
     * @param previous la valeur précédente
     * @return le taux de croissance, ou ZERO si les données sont invalides
     */
    public static BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous.abs(), MC)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calcule un ratio entre deux valeurs.
     *
     * @param numerator   le numérateur
     * @param denominator le dénominateur
     * @return le ratio, ou ZERO si les données sont invalides ou la division est impossible
     */
    public static BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MC).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calcule le ratio PEG (Price/Earnings to Growth).
     *
     * @param pe        le ratio PE (Price to Earnings)
     * @param epsGrowth la croissance du EPS
     * @return le ratio PEG, ou ZERO si les données sont invalides
     */
    public static BigDecimal calculatePegRatio(BigDecimal pe, BigDecimal epsGrowth) {
        if (pe == null || epsGrowth == null || epsGrowth.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal epsGrowthPercent = epsGrowth.multiply(BigDecimal.valueOf(100));
        return pe.divide(epsGrowthPercent, MC).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Formate une valeur BigDecimal en appliquant l'échelle et le mode d'arrondi standards.
     *
     * @param value la valeur à formater
     * @return la valeur formatée, ou ZERO si la valeur est null
     */
    public static BigDecimal safeValue(BigDecimal value) {
        return value != null ? value.setScale(SCALE, ROUNDING_MODE) : BigDecimal.ZERO;
    }

    /**
     * Calcule le taux de croissance annuel composé (CAGR) sur une période de 3 ans.
     * La formule standard utilisée est : {@code (endValue / startValue)^(1/3) - 1}.
     * <p><strong>Cas particuliers :</strong></p>
     * Si l'une des valeurs est {@code null}, la méthode retourne {@link BigDecimal#ZERO}.
     * Si l'une des valeurs est négative ou nulle, le calcul du CAGR (puissance fractionnaire)  n'est pas mathématiquement possible ou pertinent.
     * Dans ce cas, la méthode bascule automatiquement sur un calcul de croissance simple via {@link FinancialMath#calculateGrowth}.
     *
     * @param endValue   La valeur à la fin de la période (ex: Revenu année N).
     * @param startValue La valeur au début de la période (ex: Revenu année N-3).
     * @return Le CAGR calculé avec une précision de 4 décimales (HALF_UP), 
     * ou {@link BigDecimal#ZERO} en cas de données manquantes.
     */
    public static BigDecimal calculateCAGR(BigDecimal endValue, BigDecimal startValue) {
        // Return ZERO if either value is null
        if (startValue == null || endValue == null) {
            return BigDecimal.ZERO;
        }

        // If both values are positive, calculate proper CAGR
        if (startValue.signum() > 0 && endValue.signum() > 0) {
            double ratio = endValue.divide(startValue, new MathContext(8, RoundingMode.HALF_UP)).doubleValue();
            double cagr = Math.pow(ratio, 1.0 / 3.0) - 1;
            return BigDecimal.valueOf(cagr).setScale(4, RoundingMode.HALF_UP);
        }

        // Fallback: use simple growth rate when CAGR is not calculable
        // This handles cases like negative-to-positive transitions
        return FinancialMath.calculateGrowth(endValue, startValue);
    }
}


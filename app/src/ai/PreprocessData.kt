package ai

import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.ArrayDeque
import kotlin.math.sqrt

class PreprocessData {
    companion object {
        // z, y, x
        fun preprocessDataForGame(accelerometerData: ArrayDeque<List<Double>>): List<Double> {
            val features = extractFeatures(accelerometerData)
            features.map { formatFeature(it) }
            return features
        }

        private fun extractFeatures(accelerometerData: ArrayDeque<List<Double>>): List<Double> {
            val zValues = accelerometerData.map { it[0] }
            val yValues = accelerometerData.map { it[1] }
            val xValues = accelerometerData.map { it[2] }

            // Calculate features for each axis
            val zStatistics = computeAxisStatistics(zValues)
            val yStatistics = computeAxisStatistics(yValues)
            val xStatistics = computeAxisStatistics(xValues)

            // Calculate correlation coefficients
            val zyxCorr = computeCorrelationCoefficient(zValues, yValues)
            val zxyCorr = computeCorrelationCoefficient(zValues, xValues)
            val yxzCorr = computeCorrelationCoefficient(yValues, xValues)

            // Combine all features into a single list
            val features = zStatistics + yStatistics + xStatistics + listOf(zyxCorr, zxyCorr, yxzCorr)

            return features
        }

        // Compute the mean, standard deviation, median, maximum, minimum, and range for each axis
        fun computeAxisStatistics(values: List<Double>): List<Double> {
            val mean = values.average()
            val stdDev = sqrt(values.map { (it - mean) * (it - mean) }.average())
            val median = calculateMedian(values)
            val max = values.maxOrNull() ?: Double.NaN
            val min = values.minOrNull() ?: Double.NaN
            val range = max - min

            return listOf(mean, stdDev, median, max, min, range)
        }

        // Calculate the median of a list of values
        private fun calculateMedian(values: List<Double>): Double {
            val sortedValues = values.sorted()
            return if (sortedValues.size % 2 == 0) {
                (sortedValues[sortedValues.size / 2] + sortedValues[sortedValues.size / 2 - 1]) / 2
            } else {
                sortedValues[sortedValues.size / 2]
            }
        }

        // Calculate the Pearson correlation coefficient between two lists of values
        fun computeCorrelationCoefficient(
            values1: List<Double>,
            values2: List<Double>
        ): Double {
            val mean1 = values1.average()
            val mean2 = values2.average()
            val numerator = values1.zip(values2).sumOf { (v1, v2) -> (v1 - mean1) * (v2 - mean2) }
            val denominator =
                sqrt(values1.sumOf { (it - mean1) * (it - mean1) } * values2.sumOf { (it - mean2) * (it - mean2) })

            return if (denominator == 0.0) 0.0 else numerator / denominator
        }

        // Format the feature values to 6 decimal places
        fun formatFeature(value: Double): String {
            val df = DecimalFormat("#.######")
            df.roundingMode = RoundingMode.HALF_UP
            return df.format(value)
        }

//        fun standardScaling(
//            trainDataset: Instances,
//            testDataset: Instances
//        ): Pair<Instances, Instances> {
//            val filter = Normalize().apply { setInputFormat(trainDataset) }
//            val standardizedTrainDataset = Filter.useFilter(trainDataset, filter)
//            val standardizedTestDataset = Filter.useFilter(testDataset, filter)
//            return Pair(standardizedTrainDataset, standardizedTestDataset)
//        }
    }
}
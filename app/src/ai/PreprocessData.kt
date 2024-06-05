package ai

import android.content.Context
import android.util.Log
import weka.core.Instances
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Normalize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.ArrayDeque
import kotlin.math.sqrt

class PreprocessData {
    companion object {
        private const val NORMALIZATION_PARAMS_FILE_NAME = "normalization_params.txt"
        private lateinit var normalizationParams: MutableList<Pair<Double, Double>>
        private var paramsLoaded = false

        fun preprocessDataForGame(context: Context, accelerometerData: ArrayDeque<List<Double>>): List<Double> {
            val features = extractFeatures(accelerometerData)
            val normalizedFeatures = normalizeRealTime(context, features)
            // round to 6 decimal places
            return normalizedFeatures.map { it.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble() }
        }

        // Extract features from the CSV data
        fun extractFeatures(groupedRows: List<List<List<String>>>): List<List<Double>> {
            val featureList = mutableListOf<List<Double>>()

            // Iterate through each group of rows and extract features
            for (group in groupedRows) {
                val zValues = group.map { it[2].toDouble() }
                val yValues = group.map { it[3].toDouble() }
                val xValues = group.map { it[4].toDouble() }

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
                featureList.add(features)
            }

            return featureList
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

        fun normalization(context: Context, trainDataset: Instances, testDataset: Instances): Pair<Instances, Instances> {
            val filter = Normalize().apply { setInputFormat(trainDataset) }
            val standardizedTrainDataset = Filter.useFilter(trainDataset, filter)
            val standardizedTestDataset = Filter.useFilter(testDataset, filter)

            // Save normalization parameters (min and max for each attribute)
            saveNormalizationParameters(context, filter)

            return Pair(standardizedTrainDataset, standardizedTestDataset)
        }

        private fun saveNormalizationParameters(context: Context, filter: Normalize) {
            val minArray = filter.minArray
            val maxArray = filter.maxArray

            val file = File(context.filesDir, NORMALIZATION_PARAMS_FILE_NAME)
            FileOutputStream(file).use { fos ->
                for (i in minArray.indices) {
                    fos.write("${minArray[i]},${maxArray[i]}\n".toByteArray())
                }
            }
        }

        // Normalizing features in real-time
        private fun normalizeRealTime(context: Context, features: List<Double>): List<Double> {
            if (!paramsLoaded) {
                loadNormalizationParameters(context)
            }

            return features.mapIndexed { index, value ->
                val (min, max) = normalizationParams[index]
                (value - min) / (max - min)
            }
        }

        private fun loadNormalizationParameters(context: Context) {
            if (paramsLoaded) return

            val file = File(context.filesDir, NORMALIZATION_PARAMS_FILE_NAME)
            normalizationParams = mutableListOf()

            FileInputStream(file).use { fis ->
                fis.bufferedReader().forEachLine { line ->
                    val (min, max) = line.split(",").map { it.toDouble() }
                    normalizationParams.add(Pair(min, max))
                }
            }
            Log.i("ai", "Normalization parameters loaded: $normalizationParams")
            paramsLoaded = true
        }
    }
}
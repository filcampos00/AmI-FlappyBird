package ai

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.sqrt

class CsvDataAggregator {
    internal fun generateDataset(context: Context, datasetFileName: String) {
        val headers = listOf(
            "z_mean", "z_stdDev", "z_median", "z_max", "z_min", "z_range",
            "y_mean", "y_stdDev", "y_median", "y_max", "y_min", "y_range",
            "x_mean", "x_stdDev", "x_median", "x_max", "x_min", "x_range",
            "zy_correlation", "zx_correlation", "yx_correlation", "label"
        )
        val positiveFolder = "sampledata/positive"
        val negativeFolder = "sampledata/negative"

        aggregateCsvData(context, headers, datasetFileName, positiveFolder, 1, true)
        aggregateCsvData(context, headers, datasetFileName, negativeFolder, 0, false)
    }

    // Read all CSV sample data files from the resources folder and aggregates them into 1 CSV file in the internal storage
    // createDatasetFile should be true for the first method call, and false for subsequent calls
    private fun aggregateCsvData(
        context: Context,
        headers: List<String>,
        datasetFileName: String,
        directory: String,
        label: Int,
        createDatasetFile: Boolean
    ) {
        val assetManager = context.resources.assets
        val files = assetManager.list(directory) ?: emptyArray()
        val datasetFile = File(context.filesDir, datasetFileName)

        if (createDatasetFile)
            recreateFileWithHeaders(datasetFile, headers)

        files.forEach { file ->
            val fileName = "$directory/$file"
            val inputStream = assetManager.open(fileName)

            // Read the CSV file and process the data
            val csvRows = csvReader().readAll(inputStream)
            val groupedRows = csvRows.drop(1).chunked(100) // Drop header and create 100-row chunks
            val rowsFeatures = extractFeatures(groupedRows)

            // Prepare dataset rows to be written
            val datasetRows = rowsFeatures.map { row -> row.map { formatFeature(it) } + label.toString() }
            csvWriter().writeAll(datasetRows, datasetFile, append = true)
        }
    }

    // Recreate the dataset file and write headers
    private fun recreateFileWithHeaders(datasetFile: File, headers: List<String>) {
        if (datasetFile.exists()) {
            datasetFile.delete()
        }
        datasetFile.createNewFile()
        csvWriter().writeAll(listOf(headers), datasetFile, append = false)
    }

    // Extract features from the CSV data
    private fun extractFeatures(groupedRows: List<List<List<String>>>): List<List<Double>> {
        val featureList = mutableListOf<List<Double>>()

        for (group in groupedRows) {
            val zValues = group.map { it[2].toDouble() }
            val yValues = group.map { it[3].toDouble() }
            val xValues = group.map { it[4].toDouble() }

            // Calculate features for each axis
            val zFeatures = computeAxisStatistics(zValues)
            val yFeatures = computeAxisStatistics(yValues)
            val xFeatures = computeAxisStatistics(xValues)

            // Calculate correlation coefficients
            val zyxCorr = computeCorrelationCoefficient(zValues, yValues)
            val zxyCorr = computeCorrelationCoefficient(zValues, xValues)
            val yxzCorr = computeCorrelationCoefficient(yValues, xValues)

            // Combine all features into a single list
            val features = zFeatures + yFeatures + xFeatures + listOf(zyxCorr, zxyCorr, yxzCorr)
            featureList.add(features)
        }

        return featureList
    }

    // Compute the mean, standard deviation, median, maximum, minimum, and range for each axis
    private fun computeAxisStatistics(values: List<Double>): List<Double> {
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
    private fun computeCorrelationCoefficient(
        values1: List<Double>,
        values2: List<Double>
    ): Double {
        val mean1 = values1.average()
        val mean2 = values2.average()
        val numerator = values1.zip(values2).sumOf { (v1, v2) -> (v1 - mean1) * (v2 - mean2) }
        val denominator = sqrt(values1.sumOf { (it - mean1) * (it - mean1) } * values2.sumOf { (it - mean2) * (it - mean2) })

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    // Format the feature values to 6 decimal places
    private fun formatFeature(value: Double): String {
        val df = DecimalFormat("#.######")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value)
    }
}
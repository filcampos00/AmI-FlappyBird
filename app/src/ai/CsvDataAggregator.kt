package ai

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat

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

        aggregateCsvData(context, headers, datasetFileName, positiveFolder, label = true, createDatasetFile = true)
        aggregateCsvData(context, headers, datasetFileName, negativeFolder, label = false, createDatasetFile = false)
    }

    // Read all CSV sample data files from the resources folder and aggregates them into 1 CSV file in the internal storage
    // createDatasetFile should be true for the first method call, and false for subsequent calls
    private fun aggregateCsvData(
        context: Context,
        headers: List<String>,
        datasetFileName: String,
        directory: String,
        label: Boolean,
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

            // Convert each element to string and append the label
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

        // Iterate through each group of rows and extract features
        for (group in groupedRows) {
            val zValues = group.map { it[2].toDouble() }
            val yValues = group.map { it[3].toDouble() }
            val xValues = group.map { it[4].toDouble() }

            // Calculate features for each axis
            val zStatistics = PreprocessData.computeAxisStatistics(zValues)
            val yStatistics = PreprocessData.computeAxisStatistics(yValues)
            val xStatistics = PreprocessData.computeAxisStatistics(xValues)

            // Calculate correlation coefficients
            val zyxCorr = PreprocessData.computeCorrelationCoefficient(zValues, yValues)
            val zxyCorr = PreprocessData.computeCorrelationCoefficient(zValues, xValues)
            val yxzCorr = PreprocessData.computeCorrelationCoefficient(yValues, xValues)

            // Combine all features into a single list
            val features = zStatistics + yStatistics + xStatistics + listOf(zyxCorr, zxyCorr, yxzCorr)
            featureList.add(features)
        }

        return featureList
    }

    // Format the feature values to 6 decimal places
    private fun formatFeature(value: Double): String {
        val df = DecimalFormat("#.######")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value)
    }
}
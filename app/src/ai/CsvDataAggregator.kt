import ai.PreprocessData
import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File

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
            val groupedRows =
                csvRows.drop(1).chunked(ROWS_PER_CHUNK) // Drop header and create N-row chunks
            val rowsFeatures = PreprocessData.extractFeatures(groupedRows)

            // Initialize previousRow variable
            var previousRow: List<String>? = null

            rowsFeatures.forEachIndexed { index, row ->
                // Convert each element in the row to string
                val stringRow = row.map { it.toString() }
                Log.d("rows", row.toString())
                // If it's not the first row, append previous row data to current row
                val combinedRow = if (index != 0) {
                    previousRow!!.mapIndexed { i, value -> "$value,${stringRow[i]}" }
                } else {
                    stringRow
                }

                // Write the combined row to the CSV file
                csvWriter().writeAll(listOf(combinedRow), datasetFile, append = true)

                // Update previousRow for the next iteration
                previousRow = stringRow
            }


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

    companion object {
        // time window
        private const val ROWS_PER_CHUNK = 100
    }
}

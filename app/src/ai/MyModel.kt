package ai

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File

class MyModel {


    // Read all CSV sample data files from the resources folder and aggregates them into 1 CSV file in the internal storage
    private fun readAndWriteCsv(
        context: Context,
        headers: List<String>,
        targetFileName: String,
        directory: String,
        label: Int,
        createTargetFile: Boolean
    ) {
        val assetManager = context.resources.assets
        val files = assetManager.list(directory) ?: emptyArray()
        val targetFile = File(context.filesDir, targetFileName)

        // createTargetFile should be true for the first method call, and false for subsequent calls
        if (createTargetFile) {
            // Delete the file if it exists
            if (targetFile.exists()) {
                targetFile.delete()
            }

            targetFile.createNewFile()
            // Write headers to the target file
            csvWriter().writeAll(listOf(headers), targetFile, append = false)
        }

        // for each CSV file
        for (file in files) {
            val fileName = "$directory/$file"
            val inputStream = assetManager.open(fileName)

            // Read the CSV file
            val csvRows: List<List<String>> = (csvReader().readAll(inputStream))
            // Drop header row, then create chunks of 100 rows (segmentation: for 100Hz sampling rate, 100 samples = 1 second)
            val groupedRows = csvRows.drop(1).chunked(100)

            val rowsAverage = calculateAverages(groupedRows)

            val datasetRows: MutableList<List<String>> = mutableListOf()
            for (i in rowsAverage[0].indices) {
                val row = listOf(
                    rowsAverage[0][i].toString(),  // zAverage
                    rowsAverage[1][i].toString(),  // yAverage
                    rowsAverage[2][i].toString(),  // xAverage
                    label.toString()
                )
                datasetRows.add(row)
            }
            // Write the dataset rows to the target file
            csvWriter().writeAll(datasetRows, targetFile, append = true)
        }
    }

    // Calculate the average values for each chunk
    private fun calculateAverages(groupedRows: List<List<List<String>>>): List<MutableList<Double>> {
        val zList = mutableListOf<Double>()
        val yList = mutableListOf<Double>()
        val xList = mutableListOf<Double>()

        for (group in groupedRows) {
            val zAverage = group.map { it[2].toDouble() }.average()
            val yAverage = group.map { it[3].toDouble() }.average()
            val xAverage = group.map { it[4].toDouble() }.average()

            zList.add(zAverage)
            yList.add(yAverage)
            xList.add(xAverage)
        }

        return listOf(zList, yList, xList)
    }

    private fun preprocessData(context: Context) {
        val headers = listOf("z", "y", "x", "label")
        val targetFileName = "accelerometer_dataset.csv"
        val positiveFolder = "sampledata/positive"
        val negativeFolder = "sampledata/negative"

        readAndWriteCsv(context, headers, targetFileName, positiveFolder, 1, true)
        readAndWriteCsv(context, headers, targetFileName, negativeFolder, 0, false)
    }

    companion object {
        fun doStuff(context: Context) {
            val myModel = MyModel()

            myModel.preprocessData(context)
        }
    }
}


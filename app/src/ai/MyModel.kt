package ai

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyModel {


    // Read all CSV sample data files from the resources folder and aggregates them into 1 CSV file in the internal storage
    private fun readAndWriteCsv(
        context: Context,
        headers: List<String>,
        targetFileName: String,
        directory: String,
        createTargetFile: Boolean
    ) {
        val assetManager = context.resources.assets
        val files = assetManager.list(directory) ?: emptyArray()
        val targetFile = File(context.filesDir, targetFileName)

        if (createTargetFile) {
            // Delete the file if it exists
            if (targetFile.exists()) {
                targetFile.delete()
            }

            targetFile.createNewFile()

            // Write headers to the output file
            csvWriter().writeAll(listOf(headers), targetFile, append = false)
        }

        // for each CSV file
        for (file in files) {
            val fileName = "$directory/$file"
            val inputStream = assetManager.open(fileName)

            // Read the CSV file
            val rows: List<List<String>> = (csvReader().readAll(inputStream))
            // Drop header row, then create chunks of 100 rows (segmentation: for 100Hz sampling rate, 100 samples = 1 second)
            val groupedRows = rows.drop(1).chunked(100)

            calculateAverages(groupedRows)

//                val rowsAverage = listOf(listOf(zAverage) , listOf(yAverage), listOf(xAverage))
//                csvWriter().writeAll(rowsAverage, targetFile, append = true)
        }
    }

    // Calculate the average values for each chunk
    private fun calculateAverages(groupedRows: List<List<List<String>>>) {
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        for (group in groupedRows) {
            // Calculate averages for z, y, x
            val zAverage = group.map { it[2].toDouble() }.average()
            val yAverage = group.map { it[3].toDouble() }.average()
            val xAverage = group.map { it[4].toDouble() }.average()

            // Calculate mean time in nanoseconds
            val meanTimeNanos = group.map { it[0].toLong() }.average().toLong()
            // Convert nanoseconds to milliseconds
            val meanTimeMillis = meanTimeNanos / 1_000_000
            // Convert milliseconds to a human-readable format
            val meanTime = timeFormatter.format(Date(meanTimeMillis))

            // Calculate average seconds elapsed (optional)
            val secondsElapsedAverage = group.map { it[1].toDouble() }.average()

            // Output
            println("Mean Time: $meanTime")
            println(
                "Average Seconds Elapsed: ${
                    String.format(
                        Locale.getDefault(),
                        "%.2f",
                        secondsElapsedAverage
                    )
                }"
            )
            println("z: $zAverage")
            println("y: $yAverage")
            println("x: $xAverage")
            println("---")
        }
    }

    private fun preprocessData(context: Context) {
        val headers = listOf("time", "seconds_elapsed", "z", "y", "x", "label")
        val targetFileName = "accelerometer_data.csv"
        val positiveFolder = "sampledata/positive"
        val negativeFolder = "sampledata/negative"

        readAndWriteCsv(context, headers, targetFileName, positiveFolder, true)
//        readAndWriteCsv(context, headers, targetFileName, negativeFolder, false)
    }

    companion object {
        fun doStuff(context: Context) {
            val myModel = MyModel()

            myModel.preprocessData(context)
        }
    }
}


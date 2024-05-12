package ai

import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File

class MyModel {
    // Read CSV files from the resources folder and write them to 1 CSV file in the internal storage
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

        if (createTargetFile){
            // Delete the file if it exists
            if (targetFile.exists()) {
                targetFile.delete()
            }

            targetFile.createNewFile()

            // Write headers to the output file
            csvWriter().writeAll(listOf(headers), targetFile, append = false)
        }

//        var totalRows = 0
        // for each CSV file
        for (file in files) {
            val fileName = "$directory/$file"
            val inputStream = assetManager.open(fileName)

            // Read the CSV file
            val rows: List<List<String>> = (csvReader().readAll(inputStream))
//            Log.d("MyModel", "FileName: $fileName")
//            totalRows += rows.size
//            Log.d("MyModel", "Size: ${rows.size}")

            // Skip the first row
            val rowsWithoutHeader = rows.drop(1)

            // Write to the target file
            csvWriter().writeAll(rowsWithoutHeader, targetFile, append = true)
        }

//        Log.d("MyModel", "Total rows: $totalRows")
    }

    companion object {
        fun doStuff(context: Context) {
            val myModel = MyModel()
            val headers = listOf("time", "seconds_elapsed", "z", "y", "x")
            val targetFileName = "accelerometer_data.csv"
            val positiveFolder = "sampledata/positive"
            val negativeFolder = "sampledata/negative"

            myModel.readAndWriteCsv(context, headers, targetFileName, positiveFolder, true)
            myModel.readAndWriteCsv(context, headers, targetFileName, negativeFolder, false)
        }
    }
}


package ai

import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

class MyModel {

    companion object {
        fun doStuff(context: Context) {
            readAllCsvFiles(context, "sampledata/positive")
//            readAllCsvFiles(context, "sampledata/negative")
        }

        private fun readAllCsvFiles(context: Context, directory: String) {
            val assetManager = context.assets
            val files = assetManager.list(directory) ?: emptyArray()

            for (file in files) {
                val fileName = "$directory/$file"
                val inputStream = assetManager.open(fileName)

                // Read the CSV file (with header)
                val rows: List<Map<String, String>> = csvReader().readAllWithHeader(inputStream)

                Log.d("MyModel", "FileName: $fileName")
                Log.d("MyModel", "Size: ${rows.size}")
                Log.d("MyModel", "Rows: $rows")


            }
        }
    }
}


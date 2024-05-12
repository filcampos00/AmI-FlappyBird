package ai

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import java.io.InputStreamReader

class MyModel {

    companion object {
        fun readAllCsvFiles(context: Context, directory: String) {
            val assetManager = context.assets
            val files = assetManager.list(directory) ?: emptyArray()

            for (file in files) {
                val inputStream = assetManager.open("$directory/$file")
                val reader = CSVReader(InputStreamReader(inputStream))

                // Process the CSV records
                val records = reader.readAll()
                for (record in records) {
                    for (column in record) {
                        Log.d("MyModel", "$record, $column")
                    }
                }

                // Close the reader
                reader.close()
            }
        }
    }
}


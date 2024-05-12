package ai

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MyModel {

    companion object {
        fun readCsvFileLineByLine(context: Context) {
            val assetManager = context.assets
            val inputStream = assetManager.open("sampledata/positive/Accelerometer00.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.forEachLine { line ->
                val tokens = line.split(",")
                // Process tokens here
                Log.d("MyModel", tokens[0])
            }

            reader.close()
        }
    }
}


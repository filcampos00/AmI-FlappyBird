package ai

import android.content.Context

class MyModel {
    companion object {
        fun doStuff(context: Context) {
            val csvDataAggregator = CsvDataAggregator()
            csvDataAggregator.generateDataset(context)

        }
    }
}


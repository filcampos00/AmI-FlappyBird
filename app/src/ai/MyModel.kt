package ai

import android.content.Context
import weka.core.converters.ConverterUtils.DataSource

class MyModel {
    companion object {
        private const val DATASET_FILE_NAME = "accelerometer_dataset.csv"
        private const val MODEL_FILE_NAME = "movement_model.model"
        fun doStuff(context: Context) {
            val csvDataAggregator = CsvDataAggregator()
            csvDataAggregator.generateDataset(context, DATASET_FILE_NAME)


        }

        private fun trainModel(context: Context) {
            val datasetInputStream = context.resources.assets.open(DATASET_FILE_NAME)
            val ds = DataSource(datasetInputStream)
            val instances = ds.dataSet

            // TODO: split dataset into training and testing sets
            // TODO: train model on training set
            // TODO: save model to file

        }

    }
}


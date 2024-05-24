package ai

import android.content.Context
import android.util.Log
import weka.classifiers.Classifier
import weka.classifiers.Evaluation
import weka.classifiers.functions.SMO
import weka.classifiers.lazy.IBk
import weka.classifiers.trees.J48
import weka.classifiers.trees.RandomForest
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.converters.CSVLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random

class MyModel {
    companion object {
        private const val DATASET_FILE_NAME = "accelerometer_dataset.csv"
        const val MODEL_FILE_NAME = "movement_model.model"

        fun processAndSaveModel(context: Context): Boolean {
            return try {
                val csvDataAggregator = CsvDataAggregator()
                csvDataAggregator.generateDataset(context, DATASET_FILE_NAME)
                trainAndPredict(context)
                true
            } catch (e: Exception) {
                Log.e("ai", "Error processing and saving model", e)
                false
            }
        }

        private fun trainAndPredict(context: Context) {
            val instances = loadDataset(context)
            instances.setClassIndex(instances.numAttributes() - 1)
            val (trainDataset, testDataset) = splitDataset(instances)
//            val (standardizedTrainDataset, standardizedTestDataset) = PreprocessData.standardScaling(
//                trainDataset,
//                testDataset
//            )

            val bestClassifier = chooseBestClassifier(trainDataset)
            Log.i("ai", "Best classifier: ${bestClassifier.javaClass.simpleName}")

            predict(bestClassifier, testDataset)
            saveModel(context, bestClassifier)
        }

        private fun loadDataset(context: Context): Instances {
            val datasetFile = File(context.filesDir, DATASET_FILE_NAME)
            FileInputStream(datasetFile).use { fis ->
                val csvLoader = CSVLoader().apply { setSource(fis) }
                return csvLoader.dataSet
            }
        }

        private fun splitDataset(instances: Instances): Pair<Instances, Instances> {
            val trainSize = (instances.numInstances() * 0.8).toInt()
            val testSize = instances.numInstances() - trainSize
            instances.randomize(Random(1))
            val trainDataset = Instances(instances, 0, trainSize)
            val testDataset = Instances(instances, trainSize, testSize)
            return Pair(trainDataset, testDataset)
        }

        private fun chooseBestClassifier(trainDataset: Instances): Classifier {
//            val classifiers = listOf(SMO(), IBk(), J48(), RandomForest())
            val classifiers = listOf(J48(), RandomForest())
            var bestClassifier: Classifier? = null
            var bestAccuracy = 0.0

            classifiers.forEach { classifier ->
                classifier.buildClassifier(trainDataset)
                val accuracy = validate(classifier, trainDataset)
                if (accuracy > bestAccuracy) {
                    bestClassifier = classifier
                    bestAccuracy = accuracy
                }
            }

            return bestClassifier ?: throw IllegalStateException("No classifier selected")
        }

        private fun validate(classifier: Classifier, train: Instances): Double {
            val eval = Evaluation(train)
            eval.crossValidateModel(classifier, train, 10, Random(1))
            Log.i(
                "ai",
                "Cross-Validation Results for ${classifier.javaClass.simpleName}:\n${eval.toSummaryString()}\n${eval.toClassDetailsString()}\n${eval.toMatrixString()}"
            )
            return eval.pctCorrect()
        }

        private fun predict(classifier: Classifier, test: Instances) {
            val eval = Evaluation(test)
            eval.evaluateModel(classifier, test)
            Log.i(
                "ai",
                "Evaluation on Test Set:\n${eval.toSummaryString()}\n${eval.toClassDetailsString()}\n${eval.toMatrixString()}"
            )
        }

        private fun saveModel(context: Context, classifier: Classifier) {
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            FileOutputStream(modelFile).use { fos ->
                SerializationHelper.write(fos, classifier)
            }
        }
    }
}

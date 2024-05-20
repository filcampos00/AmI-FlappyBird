package ai

import android.content.Context
import weka.classifiers.Classifier
import weka.classifiers.Evaluation
import weka.classifiers.functions.SMO
import weka.classifiers.lazy.IBk
import weka.classifiers.trees.J48
import weka.classifiers.trees.RandomForest
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.converters.CSVLoader
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Normalize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random

class MyModel {
    companion object {
        private const val DATASET_FILE_NAME = "accelerometer_dataset.csv"
        private const val MODEL_FILE_NAME = "movement_model.model"
        fun doStuff(context: Context) {
            val csvDataAggregator = CsvDataAggregator()
            csvDataAggregator.generateDataset(context, DATASET_FILE_NAME)
            trainAndPredict(context)
        }

        private fun trainAndPredict(context: Context) {
            // Open dataset file from internal storage
            val datasetFile = File(context.filesDir, DATASET_FILE_NAME)
            val datasetInputStream = FileInputStream(datasetFile)

            // Load CSV file using CSVLoader
            val csvLoader = CSVLoader()
            csvLoader.setSource(datasetInputStream)
            val instances = csvLoader.dataSet

            // Set class index to the label (last attribute)
            instances.setClassIndex(instances.numAttributes() - 1)

            // Normalize features
            val normalizedInstances = normalizeFeatures(instances)

            // Split dataset - 80% training, 20% testing
            val trainSize = (normalizedInstances.numInstances() * 0.8).toInt()
            val testSize = normalizedInstances.numInstances() - trainSize
            normalizedInstances.randomize(Random(1))
            val trainDataset = Instances(normalizedInstances, 0, trainSize)
            val testDataset = Instances(normalizedInstances, trainSize, testSize)

            val bestClassifier = chooseBestClassifier(trainDataset)
            println("Best classifier: ${bestClassifier.javaClass.simpleName}")

            // predict and save model
            predict(bestClassifier, testDataset)
            saveModel(context, bestClassifier)
        }

        private fun normalizeFeatures(instances: Instances): Instances {
            val filter = Normalize()
            filter.setInputFormat(instances)
            return Filter.useFilter(instances, filter)
        }

        private fun chooseBestClassifier(trainDataset: Instances): Classifier {
            val classifiers = listOf(
                SMO(), // SVM
                IBk(), // K-NN
                J48(), // DT
                RandomForest() // RF
            )

            var bestClassifier: Classifier? = null
            var bestAccuracy = 0.0

            for (classifier in classifiers) {
                classifier.buildClassifier(trainDataset)
                val accuracy = validate(classifier, trainDataset)

                if (accuracy > bestAccuracy) {
                    bestClassifier = classifier
                    bestAccuracy = accuracy
                }
            }

            return bestClassifier ?: throw IllegalStateException("No classifier selected")
        }

        // Cross-validation
        private fun validate(classifier: Classifier, train: Instances): Double {
            val eval = Evaluation(train)
            eval.crossValidateModel(classifier, train, 10, Random(1))

            print("Cross-Validation Results for ${classifier.javaClass.simpleName}:")
            println("${eval.toSummaryString()}\n")
            println("${eval.toClassDetailsString()}\n")
            println("${eval.toMatrixString()}\n\n")

            return eval.pctCorrect()
        }

        // Predict on testing set and evaluate
        private fun predict(classifier: Classifier, test: Instances) {
            val eval = Evaluation(test)
            eval.evaluateModel(classifier, test)

            print("Evaluation on Test Set:")
            println("${eval.toSummaryString()}\n")
            println("${eval.toClassDetailsString()}\n")
            println("${eval.toMatrixString()}\n")
        }

        // Save model to file
        private fun saveModel(context: Context, classifier: Classifier) {
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            val fos = FileOutputStream(modelFile)
            SerializationHelper.write(fos, classifier)
            fos.close()
        }
    }
}


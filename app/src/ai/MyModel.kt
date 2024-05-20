package ai

import android.content.Context
import weka.classifiers.Classifier
import weka.classifiers.Evaluation
import weka.classifiers.meta.CVParameterSelection
import weka.classifiers.trees.J48
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.Utils
import weka.core.converters.CSVLoader
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

            // Split dataset - 80% training, 20% testing
            val trainSize = (instances.numInstances() * 0.8).toInt()
            val testSize = instances.numInstances() - trainSize
            instances.randomize(Random(1))
            val trainDataset = Instances(instances, 0, trainSize)
            val testDataset = Instances(instances, trainSize, testSize)

            // tune hyper parameters
            val bestClassifier = tuneHyperParameters(trainDataset)

            // Validate, predict, and save model
//            validate(bestClassifier, trainDataset)
            predict(bestClassifier, testDataset)
            saveModel(context, bestClassifier)
        }

        // Hyper parameter Tuning
        private fun tuneHyperParameters(trainDataset: Instances): J48 {
            val paramSelection = CVParameterSelection()
            paramSelection.classifier = J48()
            paramSelection.numFolds = 10
            paramSelection.addCVParameter("C 0.001 0.5 10") // Confidence factor from 0.001 to 0.5 in 10 steps
            paramSelection.addCVParameter("M 1 10 10") // MinNumObj from 1 to 10 in 10 steps

            paramSelection.buildClassifier(trainDataset)

            println("Best hyper parameters:")
            println(Utils.joinOptions(paramSelection.bestClassifierOptions))

            val bestClassifier = J48()
            bestClassifier.options = Utils.splitOptions(Utils.joinOptions(paramSelection.bestClassifierOptions))
            bestClassifier.buildClassifier(trainDataset)

            return bestClassifier
        }

        // Cross-validation
        private fun validate(classifier: Classifier, train: Instances) {
            val eval = Evaluation(train)
            eval.crossValidateModel(classifier, train, 10, Random(1))

            print("Cross-Validation Results:")
            println(eval.toSummaryString() + "\n")
            println(eval.toClassDetailsString() + "\n")
            println(eval.toMatrixString() + "\n\n")
        }

        // Predict on testing set and evaluate
        private fun predict(classifier: Classifier, test: Instances) {
            val eval = Evaluation(test)
            eval.evaluateModel(classifier, test)

            print("Evaluation on Test Set:")
            println(eval.toSummaryString() + "\n")
            println(eval.toClassDetailsString() + "\n")
            println(eval.toMatrixString() + "\n")
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


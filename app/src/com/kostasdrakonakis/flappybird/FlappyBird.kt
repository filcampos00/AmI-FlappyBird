/*
 * Copyright 2018 Konstantinos Drakonakis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kostasdrakonakis.flappybird

import ai.MyModel
import ai.PreprocessData
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import weka.classifiers.Classifier
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instances
import weka.core.SerializationHelper
import java.io.File
import java.io.FileInputStream
import java.util.ArrayDeque
import java.util.Random

class FlappyBird(private val context: Context) : ApplicationAdapter(), SensorEventListener {

    private lateinit var batch: SpriteBatch
    private lateinit var background: Texture
    private lateinit var gameOver: Texture
    private lateinit var birds: Array<Texture>
    private lateinit var topTubeRectangles: Array<Rectangle?>
    private lateinit var bottomTubeRectangles: Array<Rectangle?>
    private lateinit var birdCircle: Circle
    private lateinit var font: BitmapFont
    private lateinit var topTube: Texture
    private lateinit var bottomTube: Texture
    private lateinit var random: Random

    private var flapState = 0
    private var birdY: Float = 0f
    private var velocity: Float = 0f
    private var score: Int = 0
    private var scoringTube: Int = 0
    private var gameState: Int = 0
    private val numberOfTubes: Int = 4
    private var gdxHeight: Int = 0
    private var gdxWidth: Int = 0
    private var topTubeWidth: Int = 0
    private var bottomTubeWidth: Int = 0
    private var topTubeHeight: Int = 0
    private var bottomTubeHeight: Int = 0

    private val tubeX = FloatArray(numberOfTubes)
    private val tubeOffset = FloatArray(numberOfTubes)
    private var distanceBetweenTubes: Float = 0.toFloat()

    private val accelerometerData = ArrayDeque<List<Double>>(ACCELEROMETER_DATA_SIZE)
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var model: Classifier


    override fun create() {
        batch = SpriteBatch()
        background = Texture("bg.png")
        gameOver = Texture("gameover.png")
        birdCircle = Circle()
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(10f)

        birds = arrayOf(Texture("bird.png"), Texture("bird2.png"))

        gdxHeight = Gdx.graphics.height
        gdxWidth = Gdx.graphics.width

        topTube = Texture("toptube.png")
        bottomTube = Texture("bottomtube.png")
        random = Random()
        distanceBetweenTubes = gdxWidth * 3f / 4f
        topTubeRectangles = arrayOfNulls(numberOfTubes)
        bottomTubeRectangles = arrayOfNulls(numberOfTubes)

        topTubeWidth = topTube.width
        topTubeHeight = topTube.height
        bottomTubeWidth = bottomTube.width
        bottomTubeHeight = bottomTube.height


        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        Log.d("SensorStatus", "Sensor listener registered")
        model = loadModel(context)

        startGame()
    }

    override fun pause() {
        super.pause()
        sensorManager.unregisterListener(this)
        Log.d("SensorStatus", "Sensor listener unregistered")
    }

    override fun render() {
        batch.begin()
        batch.draw(background, 0f, 0f, gdxWidth.toFloat(), gdxHeight.toFloat())

        if (gameState == 1) {
            if (tubeX[scoringTube] < gdxWidth / 2) {
                score++
                if (scoringTube < numberOfTubes - 1) {
                    scoringTube++
                } else {
                    scoringTube = 0
                }
            }

            for (i in 0 until numberOfTubes) {

                if (tubeX[i] < -topTubeWidth) {
                    tubeX[i] += numberOfTubes * distanceBetweenTubes
                    tubeOffset[i] = (random.nextFloat() - 0.5f) * (gdxHeight.toFloat() - GAP - 200f)
                } else {
                    tubeX[i] = tubeX[i] - TUBE_VELOCITY
                }

                batch.draw(topTube, tubeX[i], gdxHeight / 2f + GAP / 2 + tubeOffset[i])
                batch.draw(
                    bottomTube,
                    tubeX[i],
                    gdxHeight / 2f - GAP / 2 - bottomTubeHeight.toFloat() + tubeOffset[i]
                )

                topTubeRectangles[i] = Rectangle(
                    tubeX[i],
                    gdxHeight / 2f + GAP / 2 + tubeOffset[i],
                    topTubeWidth.toFloat(),
                    topTubeHeight.toFloat()
                )

                bottomTubeRectangles[i] = Rectangle(
                    tubeX[i],
                    gdxHeight / 2f - GAP / 2 - bottomTubeHeight.toFloat() + tubeOffset[i],
                    bottomTubeWidth.toFloat(),
                    bottomTubeHeight.toFloat()
                )
            }

            if (birdY > 0) {
                velocity += GRAVITY
                birdY -= velocity
            } else {
                gameState = 2
            }

        } else if (gameState == 0) {
            if (Gdx.input.justTouched()) {
                gameState = 1
            }
        } else if (gameState == 2) {
            batch.draw(
                gameOver,
                gdxWidth / 2f - gameOver.width / 2f,
                gdxHeight / 2f - gameOver.height / 2f
            )

            if (Gdx.input.justTouched()) {
                gameState = 1
                startGame()
                score = 0
                scoringTube = 0
                velocity = 0f
            }
        }

        flapState = if (flapState == 0) 1 else 0

        batch.draw(birds[flapState], gdxWidth / 2f - birds[flapState].width / 2f, birdY)
        font.draw(batch, score.toString(), 100f, 200f)
        birdCircle.set(
            gdxWidth / 2f,
            birdY + birds[flapState].height / 2f,
            birds[flapState].width / 2f
        )

        for (i in 0 until numberOfTubes) {
            if (Intersector.overlaps(birdCircle, topTubeRectangles[i])
                || Intersector.overlaps(birdCircle, bottomTubeRectangles[i])
            ) {
                gameState = 2
            }
        }

        batch.end()
    }

    private fun startGame() {
        birdY = gdxHeight / 2f - birds[0].height / 2f

        for (i in 0 until numberOfTubes) {
            tubeOffset[i] = (random.nextFloat() - 0.5f) * (gdxHeight.toFloat() - GAP - 200f)
            tubeX[i] =
                gdxWidth / 2f - topTubeWidth / 2f + gdxWidth.toFloat() + i * distanceBetweenTubes
            topTubeRectangles[i] = Rectangle()
            bottomTubeRectangles[i] = Rectangle()
        }
    }

    override fun dispose() {
        batch.dispose()
        background.dispose()
        gameOver.dispose()
        birds.forEach { it.dispose() }
        topTube.dispose()
        bottomTube.dispose()
        font.dispose()
    }

    private fun shouldJump(): Boolean {
        val featuresValues = PreprocessData.preprocessDataForGame(context, accelerometerData)
        Log.d("ai", "Features values: $featuresValues")
        val attributes = generateAttributes(featuresValues)
        Log.d("ai", "Attributes: $attributes")
        val dataset = Instances("dataset", attributes, 0)
        dataset.setClassIndex(dataset.numAttributes() - 1)
//        Log.d("ai", "Dataset: $dataset")

        val instance = DenseInstance(1.0, featuresValues.toDoubleArray())
        dataset.add(instance)
        instance.setDataset(dataset)
        Log.d("ai", "Instance: $instance")

        val prediction = model.classifyInstance(instance)
        Log.d("ai", "Instance prediction: $prediction")
        val shouldJump = prediction == 1.0
        Log.d("ai", "Should jump: $shouldJump")
        return shouldJump
    }

    private fun generateAttributes(features: List<Double>): ArrayList<Attribute> {
        val label = listOf("true", "false")
        val attributes: ArrayList<Attribute> = ArrayList()

        for (i in features.indices) {
            attributes.add(Attribute("feature${i + 1}"))
        }
        attributes.add(Attribute("label", label))

        return attributes
    }

    private fun loadModel(context: Context): Classifier {
        val modelFile = File(context.filesDir, MyModel.MODEL_FILE_NAME)
        FileInputStream(modelFile).use { fis ->
            return SerializationHelper.read(fis) as Classifier
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (gameState == 1) {
                // Add the new sensor data to the queue
                accelerometerData.addLast(
                    listOf(
                        event.values[0].toDouble(),
                        event.values[1].toDouble(),
                        event.values[2].toDouble()
                    )
                )

                // If the queue is too big, remove the oldest data
                if (accelerometerData.size > ACCELEROMETER_DATA_SIZE) {
                    // Perform jump check after handling the data
                    if (shouldJump()) {
                        velocity = -30f
                    }

                    // Clear half of the old data
                    val halfSize = accelerometerData.size / 2

                    // Remove the first half of the elements from the deque
                    repeat(halfSize) {
                        accelerometerData.removeFirst()
                    }
                }
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    companion object {
        private const val GRAVITY = 0.5f
        private const val TUBE_VELOCITY = 4f
        private const val GAP = 800f
        private const val ACCELEROMETER_DATA_SIZE = 200
    }
}

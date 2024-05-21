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
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.github.kostasdrakonakis.androidnavigator.IntentNavigator

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<Button>(R.id.playButton).setOnClickListener {
            IntentNavigator.startMainActivity(this)
        }

        findViewById<Button>(R.id.aiButton).setOnClickListener {
            val boolean = MyModel.processAndSaveModel(context = this)
            Toast.makeText(this, if (boolean) "Model saved" else "Model not saved", Toast.LENGTH_LONG).show()
        }
    }
}
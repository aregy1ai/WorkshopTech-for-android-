package com.workshoptech.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TFLiteModelHelper {
    private var interpreter: Interpreter? = null

    fun loadModel(context: Context, modelFileName: String = "damage_model.tflite"): Boolean {
        return try {
            val modelBuffer = loadModelFile(context, modelFileName)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(modelBuffer, options)
            true
        } catch (e: Exception) { false }
    }

    fun loadModelFromFile(file: File): Boolean {
        return try {
            val modelBuffer = FileInputStream(file).channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            interpreter = Interpreter(modelBuffer)
            true
        } catch (e: Exception) { false }
    }

    fun getInterpreter(): Interpreter? = interpreter

    fun isLoaded(): Boolean = interpreter != null

    fun close() { interpreter?.close(); interpreter = null }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        return inputStream.channel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.length)
    }
}

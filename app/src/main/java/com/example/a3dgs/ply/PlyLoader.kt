package com.example.a3dgs.ply

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class Point(
    val x: Float,
    val y: Float,
    val z: Float,
    val r: UByte,
    val g: UByte,
    val b: UByte
)

data class PointCloud(val points: List<Point>)

object PlyLoader {
    fun loadAsciiPly(file: File): PointCloud {
        BufferedReader(FileReader(file)).use { br ->
            var line: String
            var numVertices = 0
            var headerEnded = false
            val points = mutableListOf<Point>()
            while (true) {
                line = br.readLine() ?: break
                if (!headerEnded) {
                    if (line.startsWith("element vertex")) {
                        numVertices = line.split(" ").last().toInt()
                    }
                    if (line == "end_header") {
                        headerEnded = true
                    }
                    continue
                }
                if (points.size >= numVertices) break
                val parts = line.trim().split(" ")
                if (parts.size < 6) continue
                val x = parts[0].toFloat()
                val y = parts[1].toFloat()
                val z = parts[2].toFloat()
                val r = parts[3].toInt().toUByte()
                val g = parts[4].toInt().toUByte()
                val b = parts[5].toInt().toUByte()
                points.add(Point(x, y, z, r, g, b))
            }
            return PointCloud(points)
        }
    }
}



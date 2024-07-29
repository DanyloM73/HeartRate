package com.example.heartrate;

class ImageProcessing {
    companion object {
        private fun decodeYUV420SPtoRedSum(yuv420sp: ByteArray, width: Int, height: Int): Int {
            val frameSize = width * height
            var sum = 0
            var uvp = frameSize
            var u = 0
            var v = 0

            for (j in 0 until height) {
                var yp = j * width
                for (i in 0 until width) {
                    var y = (0xff and yuv420sp[yp].toInt()) - 16
                    if (y < 0) y = 0
                    if (i and 1 == 0) {
                        if (uvp + 1 < yuv420sp.size) {
                            v = (0xff and yuv420sp[uvp++].toInt()) - 128
                            u = (0xff and yuv420sp[uvp++].toInt()) - 128
                        }
                    }

                    val y1192 = 1192 * y
                    var r = y1192 + 1634 * v
                    if (r < 0) r = 0
                    else if (r > 262143) r = 262143

                    sum += r shr 10 and 0xff
                    yp++
                }
            }
            return sum
        }

        // Decode YUV to Red Avg
        fun decodeYUV420SPtoRedAvg(yuv420sp: ByteArray, width: Int, height: Int): Int {
            val frameSize = width * height
            val sum = decodeYUV420SPtoRedSum(yuv420sp, width, height)
            return sum / frameSize
        }
    }
}

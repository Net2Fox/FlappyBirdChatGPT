package ru.net2fox.flappybirdchatgpt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.util.*

@SuppressLint("ClickableViewAccessibility")
class FlappyBirdView(context: Context?, attrs: AttributeSet?) : View(context, attrs), Runnable  {

    private val JUMP_VELOCITY = -25 // bird's jump velocity in pixels per frame

    private val bird = Bird()
    private val pipes = Pipes()
    private var background: Background? = null

    private var gameOverBitMap: Bitmap? = null
    val darkeningPaint = Paint().apply {
        color = Color.BLACK
        alpha = 128
        style = Paint.Style.FILL
    }



    private val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val displayMetrics = DisplayMetrics()

    init {
        gameOverBitMap = getScaledBitMap(R.drawable.gameover, 901, 504)
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        background = context?.let { Background() }
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Increase the bird's velocity when the screen is tapped
                bird.velocity = (JUMP_VELOCITY).toFloat()
            }
            if (event.action == MotionEvent.ACTION_DOWN && !isPlaying) {
                restart()
            }
            true
        }
    }

    private var isPlaying = true

    private val FRAME_PERIOD = 17 // desired frame rate in milliseconds

    private var touchAction = MotionEvent.ACTION_UP

    override fun run() {
        while (isPlaying) {
            val startTime = System.currentTimeMillis()

            update()
            postInvalidate()

            val timeElapsed = System.currentTimeMillis() - startTime
            if (timeElapsed < FRAME_PERIOD) {
                try {
                    Thread.sleep(FRAME_PERIOD - timeElapsed)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun restart() {
        bird.x = (width / 2).toFloat()
        bird.y = (height / 2).toFloat()
        bird.velocity = 0f
        pipes.x = width
        pipes.y = height
        isPlaying = true
        Thread(this).start()
    }

    private fun checkCollision() {
        // Check if the bird has collided with the top or bottom of the screen
        if (bird.y + bird.height >= height || bird.y <= 0) {
            isPlaying = false
            Thread(this).interrupt()
            return
        }

        // Create rectangles for the bird and each pipe
        val birdRect = Rect(
            bird.x.toInt(), bird.y.toInt(),
            (bird.x + bird.width).toInt(), (bird.y + bird.height).toInt()
        )
            val topPipeRect = Rect(pipes.x, pipes.y - pipes.gapHeight - pipes.bitmapTop.height, pipes.x + pipes.bitmapTop.width, pipes.y - pipes.gapHeight)
            val bottomPipeRect = Rect(pipes.x, pipes.y - pipes.gapHeight + pipes.gapDistance, pipes.x + pipes.bitmapBottom.width, pipes.y - pipes.gapHeight + pipes.gapDistance + pipes.bitmapBottom.height)

            // Check for a collision between the bird and the top or bottom pipe
            if (birdRect.intersect(topPipeRect) || birdRect.intersect(bottomPipeRect)) {
                isPlaying = false
                Thread(this).interrupt()

            }

    }

    override fun onDraw(canvas: Canvas) {
        background!!.draw(canvas)
        pipes.draw(canvas)
        bird.draw(canvas)
        if (!isPlaying) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), darkeningPaint)
            canvas.drawBitmap(gameOverBitMap!!, (width - gameOverBitMap!!.width) / 2.toFloat(), (height - gameOverBitMap!!.height) / 2.toFloat(), null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchAction = event.action
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bird.x = ((w / 2).toFloat())
        bird.y = ((h / 2).toFloat())
        pipes.x = w
        pipes.y = h
        Thread(this).start()
    }

    fun update() {
        bird.update()
        checkCollision()
        pipes.update()
        background!!.update()
        invalidate()
    }

    private inner class Bird {
        var x = 0f
        var y = 0f
        var bitmap = getScaledBitMap(R.drawable.flappy_bird, 136, 96)
        var height = 0
        var width = 0

        var velocity = 0f
        var gravity = 2f


        init {
            height = bitmap.height
            width = bitmap.width
        }

        fun update() {
            y += velocity
            velocity += gravity
        }

        fun draw(canvas: Canvas) {
            canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
        }
    }

    private inner class Pipes {
        var x = 0
        var y = 0
        var gapHeight = 500
        var gapDistance = 500
        var minPipeHeight = 1000
        var maxPipeHeight = 1900
        var bitmapTop = getScaledBitMap(R.drawable.pipe_top, 208, 2000)
        var bitmapBottom = getScaledBitMap(R.drawable.pipe_bottom, 208, 2000)
        var velocity = 5

        init {
            y = (minPipeHeight + maxPipeHeight) / 2
            x = width
        }

        fun update() {
            x -= velocity
            if (x <= -bitmapTop.width) {
                // when a pipe goes off the screen, generate a new height for it and move it to the right of the screen
                x += width + bitmapTop.width
                gapHeight = minPipeHeight + Random().nextInt(maxPipeHeight - minPipeHeight)
            }
        }

        fun draw(canvas: Canvas) {
            canvas.drawBitmap(bitmapTop, x.toFloat(), y.toFloat() - gapHeight - bitmapTop.height.toFloat(), null)
            canvas.drawBitmap(bitmapBottom, x.toFloat(), y.toFloat() - gapHeight + gapDistance, null)
        }
    }

    private inner class Background {

        private val bgImage: Bitmap = getScaledBitMap(R.drawable.flappy_bird_background, displayMetrics.widthPixels, displayMetrics.heightPixels)
        private var bgX: Int = 0
        private var bgY: Int = 0

        fun update() {
            bgX -= 5
            if (bgX < -width) {
                bgX = 0
            }
        }

        fun draw(canvas: Canvas) {
            canvas.drawBitmap(bgImage, bgX.toFloat(), bgY.toFloat(), null)
            canvas.drawBitmap(bgImage, bgX.toFloat() + width, bgY.toFloat(), null)
        }
    }

    private fun getScaledBitMap(resourceId: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }


    private fun getBitmap(resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, resourceId)
    }
}

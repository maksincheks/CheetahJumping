package com.example.cheetahjumpgame

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var player: ImageView
    private lateinit var scoreText: TextView
    private lateinit var rootLayout: RelativeLayout
    private var score = 0
    private var consecutivePasses = 0
    private val minObstacleDistance = 6000f
    private val handler = Handler(Looper.getMainLooper())
    private val gravity = 1.2f
    private val jumpForce = -25f
    private var velocityY = 0f
    private var obstacleSpeed = 10f
    private var isGameOver = false

    // Звуковые эффекты
    private lateinit var backgroundMusic: MediaPlayer
    private lateinit var jumpSound: MediaPlayer
    private lateinit var loseSound: MediaPlayer

    private val obstacles = mutableListOf<Pair<ImageView, ImageView>>()
    private var nextObstacleX = 0f
    private var gapBetweenObstacles = 2000f
    private val obstacleWidth = 150
    private val minGapHeight = 600
    private var lastObstaclePair: Pair<ImageView, ImageView>? = null
    private var passedObstacle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация звуков
        backgroundMusic = MediaPlayer.create(this, R.raw.fon_music)
        jumpSound = MediaPlayer.create(this, R.raw.jump)
        loseSound = MediaPlayer.create(this, R.raw.lose)
        backgroundMusic.isLooping = true

        scoreText = findViewById(R.id.scoreText)
        rootLayout = findViewById(R.id.rootLayout)

        // Устанавливаем фоновое изображение
        rootLayout.background = ContextCompat.getDrawable(this, R.drawable.fon)

        // Игрок
        player = ImageView(this).apply {
            setImageResource(R.drawable.rabbit)
            layoutParams = RelativeLayout.LayoutParams(150, 150)
        }
        rootLayout.addView(player)

        nextObstacleX = rootLayout.width.toFloat()
        createNewObstacle()

        rootLayout.post {
            startGame()
        }
    }

    private fun startGame() {
        isGameOver = false
        score = 0
        consecutivePasses = 0
        scoreText.text = "Очки: $score"
        obstacleSpeed = 10f
        passedObstacle = false

        backgroundMusic.start()

        player.x = rootLayout.width * 0.2f
        player.y = rootLayout.height / 2f
        velocityY = 0f
        player.rotation = 0f

        obstacles.forEach { (top, bottom) ->
            rootLayout.removeView(top)
            rootLayout.removeView(bottom)
        }
        obstacles.clear()

        nextObstacleX = rootLayout.width.toFloat()
        createNewObstacle()

        handler.post(gameLoop)
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                updateGame()
                handler.postDelayed(this, 16)
            }
        }
    }

    private fun updateGame() {
        velocityY += gravity
        player.y += velocityY
        player.rotation = velocityY * 2

        if (player.y < 0) {
            player.y = 0f
            velocityY = 0f
        } else if (player.y > rootLayout.height - player.height) {
            gameOver()
        }

        moveObstacles()
        checkObstaclePassed()
        checkCollisions()
    }

    private fun moveObstacles() {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val (topPipe, bottomPipe) = iterator.next()
            topPipe.x -= obstacleSpeed
            bottomPipe.x -= obstacleSpeed

            if (topPipe.x + topPipe.width < 0) {
                rootLayout.removeView(topPipe)
                rootLayout.removeView(bottomPipe)
                iterator.remove()
            }
        }

        if (obstacles.isEmpty() ||
            (obstacles.last().first.x + obstacles.last().first.width) < (rootLayout.width - minObstacleDistance)
        ) {
            createNewObstacle()
        }
    }

    private fun createNewObstacle() {
        val maxTopHeight = rootLayout.height - minGapHeight - 100
        val topHeight = Random.nextInt(100, maxTopHeight.coerceAtLeast(200))
        val gapHeight = minGapHeight + Random.nextInt(0, 200)

        val topPipe = ImageView(this).apply {
            setImageResource(R.drawable.obstacle)
            layoutParams = RelativeLayout.LayoutParams(
                obstacleWidth,
                topHeight
            ).apply {
                topMargin = 0
            }
            scaleType = ImageView.ScaleType.FIT_XY
            rotation = 180f
        }

        val bottomPipe = ImageView(this).apply {
            setImageResource(R.drawable.obstacle)
            layoutParams = RelativeLayout.LayoutParams(
                obstacleWidth,
                rootLayout.height - topHeight - gapHeight
            ).apply {
                topMargin = topHeight + gapHeight
            }
            scaleType = ImageView.ScaleType.FIT_XY
        }

        nextObstacleX = rootLayout.width.toFloat()
        topPipe.x = nextObstacleX
        bottomPipe.x = nextObstacleX

        rootLayout.addView(topPipe)
        rootLayout.addView(bottomPipe)

        obstacles.add(topPipe to bottomPipe)
        lastObstaclePair = topPipe to bottomPipe
        passedObstacle = false
    }

    private fun checkObstaclePassed() {
        lastObstaclePair?.let { (topPipe, _) ->
            if (!passedObstacle && player.x > topPipe.x + topPipe.width) {
                passedObstacle = true
                consecutivePasses++
                score++

                if (consecutivePasses % 5 == 0) {
                    score += 5
                }

                scoreText.text = "Очки: $score"

                if (score % 5 == 0) {
                    obstacleSpeed += 0.5f
                    gapBetweenObstacles = (gapBetweenObstacles * 0.95f).coerceAtLeast(1000f)
                }
            }
        }
    }

    private fun checkCollisions() {
        if (player.y < 0 || player.y > rootLayout.height - player.height) {
            gameOver()
            return
        }

        for ((topPipe, bottomPipe) in obstacles) {
            if (checkCollisionWithPipe(player, topPipe) ||
                checkCollisionWithPipe(player, bottomPipe)) {
                gameOver()
                return
            }
        }
    }

    private fun checkCollisionWithPipe(player: View, pipe: View): Boolean {
        return player.x < pipe.x + pipe.width &&
                player.x + player.width > pipe.x &&
                player.y < pipe.y + pipe.height &&
                player.y + player.height > pipe.y
    }

    private fun gameOver() {
        if (isGameOver) return

        isGameOver = true
        consecutivePasses = 0
        handler.removeCallbacks(gameLoop)
        backgroundMusic.pause()
        loseSound.start()

        // Создаем контейнер для диалогового окна
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.argb(220, 40, 40, 40))
        }

        // Заголовок "ИГРА ОКОНЧЕНА"
        val title = TextView(this).apply {
            text = "ИГРА ОКОНЧЕНА"
            setTextColor(Color.RED)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)

        // Текст с очками
        val scoreText = TextView(this).apply {
            text = "Счёт: $score"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }
        layout.addView(scoreText)

        // Создаем диалоговое окно
        AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Заново") { _, _ ->
                startGame()
            }
            .setNegativeButton("В меню") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()

                // Настройка кнопок
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.argb(100, 0, 150, 0))
                }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.argb(100, 150, 0, 0))
                }
            }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isGameOver) {
                    velocityY = jumpForce
                    player.rotation = -30f
                    jumpSound.start()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        backgroundMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!isGameOver) {
            backgroundMusic.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic.release()
        jumpSound.release()
        loseSound.release()
    }
}
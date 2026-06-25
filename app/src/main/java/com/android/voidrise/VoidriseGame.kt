package com.android.voidrise

import com.android.voidrise.game.GameScreen
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class VoidriseGame : Game() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var shapes: ShapeRenderer
        private set
    lateinit var hudFont: BitmapFont
        private set
    lateinit var hudFontLarge: BitmapFont
        private set

    override fun create() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        val scale = 1.6f * (Gdx.graphics.density / 2f).coerceIn(0.85f, 1.4f)
        hudFont = BitmapFont().apply { data.setScale(scale * 0.75f) }
        hudFontLarge = BitmapFont().apply { data.setScale(scale) }
        setScreen(GameScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        shapes.dispose()
        hudFont.dispose()
        hudFontLarge.dispose()
        screen?.dispose()
    }
}

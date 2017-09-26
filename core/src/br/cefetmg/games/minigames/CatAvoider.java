package br.cefetmg.games.minigames;

import static br.cefetmg.games.Config.*;
import br.cefetmg.games.minigames.util.DifficultyCurve;
import br.cefetmg.games.graphics.MultiAnimatedSprite;
import static br.cefetmg.games.minigames.CatAvoider.*;
import br.cefetmg.games.minigames.util.TimeoutBehavior;
import br.cefetmg.games.screens.BaseScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer.Task;
import java.util.HashMap;
import com.badlogic.gdx.audio.Sound;
import br.cefetmg.games.minigames.util.MiniGameStateObserver;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import java.util.Random;

class AnimatedSprite {

    public Animation moviment;
    public float timeAnimation;
    private Texture spriteSheet;
    private TextureRegion[][] animationPictures;

    public AnimatedSprite(String sprite, float time, int frameWidth, int frameHeight, int frames, int sense, int startPosiiton) {
        spriteSheet = new Texture(sprite);
        animationPictures = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        timeAnimation = 0;
        createMoviment(time, sense, frames, startPosiiton);
    }

    public void configurePlayMode(Animation.PlayMode p) {
        moviment.setPlayMode(p);
    }

    public void createMoviment(float time, int sense, int frames, int startPosiiton) {
        TextureRegion[] t = new TextureRegion[frames];
        int i, k;
        k = 0;
        if (sense == LEFT) {
            for (i = startPosiiton; i >= 0; i--) {
                t[k] = animationPictures[0][i];
                k++;
            }
            for (i = frames-1; i > startPosiiton; i--) {
                t[k] = animationPictures[0][i];
                k++;
            }
        } else {
            for (i = startPosiiton; i < frames; i++) {
                t[k] = animationPictures[0][i];
                k++;
            }
            for (i = 0; i < startPosiiton; i++) {
                t[k] = animationPictures[0][i];
                k++;
            }
        }

        moviment = new Animation(time, t);
        moviment.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
    }
}


public class CatAvoider extends MiniGame {

    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    public CatAvoider(BaseScreen screen,
            MiniGameStateObserver observer, float difficulty) {
        super(screen, observer, difficulty, 10f,
                TimeoutBehavior.WINS_WHEN_MINIGAME_ENDS);
    }

    private Texture backgroundTexture; //backgroud texture of the world
    private Sprite backgroundSprite;//sprite created for the background

    private Texture limits;//texture created for the screen limits
    private float limitsWidth;//width of the screen limits
    private float limitsHeight;//height of the screen limits
    private Obstacle down;//scren limit down
    private Obstacle up;//screen limit up
    private Obstacle left;//screen limit left
    private Obstacle right;//screen limit right

    protected Random randomGenerator = new Random();//objetct to generate random numbers
    protected int signal = randomGenerator.nextInt(2);//set the direction of cat moviment in random mode

    protected AnimatedSprite catMovingUpR, catMovingUpL, catMovingDownR, catMovingDownL;
    protected AnimatedSprite catMovingLeft, catMovingRight;
    protected static Music backgroundMusic, impact;
    protected enum catMoveType{upR, upL, downL, downR, left, right}

    class Wool {

        protected Circle circle;//circle to enclose the cat and treat the colision
        protected Sprite mouse;//sprite for the mouse(playable character)
        protected Vector2 position;//vector to get the mouse positon on the screen
        protected Vector2 direction;//vetor to get the cat direction on the screen
        protected Texture texture;
        protected Sprite sprite;
        protected int life = 1;

        public void getPosition() {
            position.x = Gdx.input.getX() * WORLD_WIDTH / viewport.getScreenWidth();
            position.y = WORLD_HEIGHT - (Gdx.input.getY() * WORLD_HEIGHT / viewport.getScreenHeight());
        }
    }

    protected Wool wool = new Wool();

    class Cat {
        protected Rectangle rect;//rectangle to enclose the cat and treat the collision
        protected Texture texture;//texture for the non playable character ninja cat
        protected Sprite sprite;//sprite of the non non playable character ninja cat
        protected float speed = 40;//variable used to set the cat ninja speed
        protected float delta = 5;//variable used to set the delta of displacement of the cat ninja in the screen per period of time
        protected catMoveType moveType; //variable to set the type of moviment of the cat (jump or random)
        protected int state;//variable to indicate the type of moviment of the cat (jump or random)
        protected final int randomState = 0;//constant to indicate that the cat is moving randomly
        protected final int jumpState = 1;//constante to indicate that the cat will jump towars the mouse
        
        public void lookAhead() {
            double angle = Math.atan(wool.direction.y / wool.direction.x);

            angle += (wool.direction.x > 0) ? -Math.PI / 2 : Math.PI / 2;
            angle *= 180 / Math.PI;
            sprite.setRotation((float) angle);
        }

        public void setDirection() {
            wool.direction.x = wool.position.x - (sprite.getX() + sprite.getWidth() / 2);
            wool.direction.y = wool.position.y - (sprite.getY() + sprite.getHeight() / 2);
            lookAhead();
        }

        public void incrementX(float delta) {
            if ((sprite.getX() + sprite.getWidth() + delta) < WORLD_WIDTH && (moveType.equals(catMoveType.upR) || moveType.equals(catMoveType.upL) || moveType.equals(catMoveType.downR) || moveType.equals(catMoveType.downL))) {
                sprite.setX(sprite.getX() + delta);
            }
        }

        public void decrementX(float delta) {
            if ((sprite.getX() - delta) > limitsWidth && (moveType.equals(catMoveType.upR) || moveType.equals(catMoveType.upL) || moveType.equals(catMoveType.downR) || moveType.equals(catMoveType.downL))) {
                sprite.setX(sprite.getX() - delta);
            }
        }

        public void incrementY(float delta) {
            if ((sprite.getY() + sprite.getHeight() + delta) < WORLD_HEIGHT - limitsHeight && (moveType.equals(catMoveType.left) || moveType.equals(catMoveType.right))) {
                sprite.setY(sprite.getY() + delta);
            }
        }

        public void decrementY(float delta) {
            if ((sprite.getY() - delta) > limitsHeight && (moveType.equals(catMoveType.left) || moveType.equals(catMoveType.right))) {
                sprite.setY(sprite.getY() - delta);
            }
        }

        public void randomMovementX(int signal) {
            if (signal == 0) {
                incrementX(delta);
            } else if (signal == 1) {
                decrementX(delta);
            }
        }

        public void randomMovementY(int signal) {
            if (signal == 0) {
                incrementY(delta);
            } else if (signal == 1) {
                decrementY(delta);
            }
        }

        public void moveRandom(int signal) {
            final int move = 1;
            int movement = move;

            if (movement == move) {
                if (moveType.equals(catMoveType.downL) || moveType.equals(catMoveType.downR) || moveType.equals(catMoveType.upL) || moveType.equals(catMoveType.upR)) {
                    randomMovementX(signal);
                }
                if (moveType.equals(catMoveType.left) || moveType.equals(catMoveType.right)) {
                    randomMovementY(signal);
                }
            }
        }

        public void move() {
            if (state == jumpState) {
                jump();
                verifyCollision();
                signal = randomGenerator.nextInt(2);
            } else {
                moveRandom(signal);
            }
        }

        public void jump() {
            Vector2 normalized = new Vector2(wool.direction);
            normalized.nor();
            normalized.scl(speed);

            normalized.x += sprite.getX();
            normalized.y += sprite.getY();

            sprite.setPosition(normalized.x, normalized.y);
            System.out.println("Wool: " + wool.position.x + " " + wool.position.y);
            System.out.println("Cat: " + sprite.getX() + " " + sprite.getY());
        }

        public void reflect() {
            wool.direction.x = wool.position.x - (sprite.getX() + sprite.getWidth() / 2);
            wool.direction.y = wool.position.y - (sprite.getY() + sprite.getHeight() / 2);
            lookAhead();
        }
    }

    Cat cat = new Cat();

    public void verifyCollision() {
        cat.rect = cat.sprite.getBoundingRectangle();

        wool.circle = new Circle();
        wool.circle.set(wool.sprite.getX(), wool.sprite.getY(), 0.25f);

        /*
        /**collision cat wool*/
        if (Colision.rectCircleOverlap(cat.rect, wool.circle) != null) {
            wool.life = 0;
            impact.play();
            super.challengeFailed();
            backgroundMusic.stop();
        }

        /**
         * collision cat floor
         */
        if (Colision.rectsOverlap(down.getRec(), cat.rect)) {
            cat.sprite.setPosition(cat.sprite.getX(), down.getRec().y+down.getRec().height);
            cat.reflect();
            if(wool.position.x > cat.sprite.getX())
                cat.moveType = catMoveType.downR;
            else if(wool.position.x < cat.sprite.getX())
                cat.moveType = catMoveType.downL;
            cat.state = cat.randomState;
        } /**
         * collision cat roof
         */
        else if (Colision.rectsOverlap(up.getRec(), cat.rect)) {
            cat.sprite.setPosition(cat.sprite.getX(), up.getRec().y - up.getRec().height - cat.sprite.getHeight());
            cat.reflect();
            if(wool.position.x > cat.sprite.getX())
                cat.moveType = catMoveType.upR;
            else if(wool.position.x < cat.sprite.getX())
                cat.moveType = catMoveType.upL;
            cat.state = cat.randomState;
        }

        /**
         * collision cat left wall
         */
        if (Colision.rectsOverlap(left.getRec(), cat.rect)) {
            cat.sprite.setPosition(left.getRec().x + left.getRec().width , cat.sprite.getY());
            cat.reflect();
            cat.moveType = catMoveType.left;
            cat.state = cat.randomState;
        } /**
         * collision cat right wall
         */
        else if (Colision.rectsOverlap(right.getRec(), cat.rect)) {
            cat.sprite.setPosition(right.getRec().x - right.getRec().width - cat.sprite.getWidth(), cat.sprite.getY());
            cat.reflect();
            cat.moveType = catMoveType.right;
            cat.state = cat.randomState;
        }
    }

    @Override
    protected void onStart() {
        backgroundTexture = assets.get("avoider/backgroundTexture.png", Texture.class);
        backgroundSprite = new Sprite(backgroundTexture);
        backgroundSprite.setSize(WORLD_WIDTH, WORLD_HEIGHT);

        limitsWidth = 20;
        limitsHeight = 20;
        limits = assets.get("avoider/grey.png", Texture.class);
        up = new Obstacle(batch, new Vector2(0, WORLD_HEIGHT - limitsHeight), WORLD_WIDTH, limitsHeight);
        down = new Obstacle(batch, new Vector2(0, 0), WORLD_WIDTH, limitsHeight);
        left = new Obstacle(batch, new Vector2(0, limitsWidth), limitsWidth, WORLD_HEIGHT);
        right = new Obstacle(batch, new Vector2(WORLD_WIDTH - limitsWidth, limitsWidth), limitsWidth, WORLD_HEIGHT);

        cat.texture = assets.get("avoider/catNinja.png", Texture.class);
        cat.sprite = new Sprite(cat.texture);
        cat.sprite.setSize(100, 150);
        cat.sprite.setOrigin(cat.sprite.getWidth() / 2, cat.sprite.getHeight() / 2);
        cat.sprite.setPosition(WORLD_WIDTH / 2, limitsWidth);
        int random = randomGenerator.nextInt();
        cat.moveType = catMoveType.downL;
        cat.state = cat.randomState;

        catMovingUpL = new AnimatedSprite("avoider/cat-moving-upL.png", 0.1f, 144, 150, 12 , RIGHT, 0);
        catMovingUpR = new AnimatedSprite("avoider/cat-moving-upR.png", 0.1f, 144, 150, 12 , RIGHT, 0);
        
        catMovingDownL = new AnimatedSprite("avoider/cat-moving-downL.png", 0.1f, 144, 150, 12 , RIGHT, 0);
        catMovingDownR = new AnimatedSprite("avoider/cat-moving-downR.png", 0.1f, 144, 150, 12 , RIGHT, 0);
        
        catMovingLeft =  new AnimatedSprite("avoider/cat-moving-left.png", 0.1f, 142, 150, 12 , RIGHT, 0);
        catMovingRight = new AnimatedSprite("avoider/cat-moving-right.png", 0.1f, 142, 150, 12 , RIGHT, 0);
        
        wool.direction = new Vector2(0, 0);
        wool.texture = assets.get("avoider/wool.png", Texture.class);
        wool.position = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        wool.sprite = new Sprite(wool.texture);
        wool.sprite.setSize(50, 50);

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("avoider/ninja_theme.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.play();

        impact = Gdx.audio.newMusic(Gdx.files.internal("avoider/impact.mp3"));
    }

    public float getCurveValue(float value) {
        return (float) (1f / (1f + Math.pow(Math.E, -6 * (value - 0.5f))));
    }

    @Override
    protected void configureDifficultyParameters(float difficulty) {

        cat.speed = cat.speed + getCurveValue(cat.speed);
    }

    @Override
    public void onHandlePlayingInput() {
    }

    @Override
    public void onUpdate(float dt) {
        int changeState = randomGenerator.nextInt(30);
        if (changeState == 29) {
            cat.state = cat.jumpState;
        }
        cat.move();
        wool.getPosition();
        wool.sprite.setPosition(wool.position.x, wool.position.y);

        catMovingUpL.timeAnimation += Gdx.graphics.getDeltaTime();
        catMovingUpR.timeAnimation += Gdx.graphics.getDeltaTime();
        catMovingDownL.timeAnimation += Gdx.graphics.getDeltaTime();
        catMovingDownR.timeAnimation += Gdx.graphics.getDeltaTime();
        catMovingLeft.timeAnimation += Gdx.graphics.getDeltaTime();
        catMovingRight.timeAnimation += Gdx.graphics.getDeltaTime();
    }

    @Override
    public void onDrawGame() {
        backgroundSprite.draw(batch);

        down.draw();
        up.draw();
        left.draw();
        right.draw();
        
        if(cat.moveType.equals(catMoveType.upL))
            batch.draw((TextureRegion) catMovingUpL.moviment.getKeyFrame(catMovingUpL.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        else if(cat.moveType.equals(catMoveType.upR))
            batch.draw((TextureRegion) catMovingUpR.moviment.getKeyFrame(catMovingUpR.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        else if(cat.moveType.equals(catMoveType.downL))
            batch.draw((TextureRegion) catMovingDownL.moviment.getKeyFrame(catMovingDownL.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        else if(cat.moveType.equals(catMoveType.downR))
            batch.draw((TextureRegion) catMovingDownR.moviment.getKeyFrame(catMovingDownR.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        else if(cat.moveType.equals(catMoveType.left))
            batch.draw((TextureRegion) catMovingLeft.moviment.getKeyFrame(catMovingLeft.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        else if(cat.moveType.equals(catMoveType.right))
            batch.draw((TextureRegion) catMovingRight.moviment.getKeyFrame(catMovingRight.timeAnimation), cat.sprite.getX(), cat.sprite.getY());
        
        if (wool.life == 1) {
            wool.sprite.draw(batch);
        }
    }

    @Override
    public String getInstructions() {
        return "Não deixe o gato pegar o novelo!!!";
    }

    @Override
    public boolean shouldHideMousePointer() {
        return true;
    }
}

/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.pong;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.ui.UI;
import com.almasb.fxglgames.pong.PongApp.MessageReaderS;
import com.almasb.fxglgames.pong.PongApp.MessageWriterS;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.pong.NetworkMessages.*;

/**
 * A simple clone of Pong.
 * Sounds from https://freesound.org/people/NoiseCollector/sounds/4391/ under CC BY 3.0.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PongApp extends GameApplication implements MessageHandler<String> {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Pong");
        settings.setVersion("1.0");
        settings.setFontUI("pong.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private Entity player3;
    private Entity ball;
    private Entity powerUp;
    private BatComponent player1Bat;
    private BatComponent player2Bat;
    private BatComponent player3Bat;
    private boolean powerUpActive;


    private Server<String> server;

    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Up1") {
            @Override
            protected void onAction() {
                player1Bat.up();
            }

            @Override
            protected void onActionEnd() {
                player1Bat.stop();
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Down1") {
            @Override
            protected void onAction() {
                player1Bat.down();
            }

            @Override
            protected void onActionEnd() {
                player1Bat.stop();
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Up2") {
            @Override
            protected void onAction() {
                player2Bat.up();
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
            }
        }, KeyCode.I);

        getInput().addAction(new UserAction("Down2") {
            @Override
            protected void onAction() {
                player2Bat.down();
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
            }
        }, KeyCode.K);

            getInput().addAction(new UserAction("Left3") {
            @Override
            protected void onAction() {
                player3Bat.left();
            }

            @Override
            protected void onActionEnd() {
                player3Bat.stop();
            }
        }, KeyCode.F);

        getInput().addAction(new UserAction("Right3") {
            @Override
            protected void onAction() {
                player3Bat.right();
            }

            @Override
            protected void onActionEnd() {
                player3Bat.stop();
            }
        }, KeyCode.G);


    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1score", 0);
        vars.put("player2score", 0);
        vars.put("player3score", 0);
    }



    @Override
    protected void initGame() {
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));

        server.setOnConnected(connection -> {
            connection.addMessageHandlerFX(this);
            // When a new client connects
             UUID playerId = UUID.randomUUID(); // Assign a unique ID
             connection.send("PLAYER_ID," + playerId.toString()); // Send ID to client
        });

        getGameWorld().addEntityFactory(new PongFactory());
        getGameScene().setBackgroundColor(Color.rgb(0, 0, 5));

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();

     
    }



    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 0);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            @Override
            protected void onHitBoxTrigger(Entity ball, Entity wall, HitBox boxA, HitBox boxB) {
                BallComponent ballComp = ball.getComponent(BallComponent.class);
                Entity lastBatHit = ballComp.getLastBatHit();
        
                if (lastBatHit != null) {
                    // Check which player's bat it was and increment their score
                    if (lastBatHit == player1) {
                        inc("player1score", +1);
                    } else if (lastBatHit == player2) {
                        inc("player2score", +1);
                    } else if (lastBatHit == player3) {
                        inc("player3score", +1);
                    }
                    //System.out.println("Ball hit the wall. Last bat hit: " + (lastBatHit == player1 ? "Player 1" : lastBatHit == player2 ? "Player 2" : "Player 3"));
        
                    // Broadcast updated scores
                    server.broadcast("SCORES," + geti("player1score") + "," + geti("player2score") + "," + geti("player3score"));
        
                    // Reset last bat hit
                    ballComp.setLastBatHit(null);
                }
        
                if (boxB.getName().equals("LEFT") || boxB.getName().equals("RIGHT")) {
                   
                } else if (boxB.getName().equals("TOP") || boxB.getName().equals("BOT")) {
                  
                }
        
                // Additional effects, like screen shake
                getGameScene().getViewport().shakeTranslational(5);
            }
        });
        



       CollisionHandler ballBatHandler = new CollisionHandler(EntityType.BALL, EntityType.PLAYER_BAT) {
    @Override
    protected void onCollisionBegin(Entity ball, Entity bat) {
        BallComponent ballComp = ball.getComponent(BallComponent.class);
        ballComp.setLastBatHit(bat);
        //System.out.println("Ball hit by bat. Bat ID: " + (bat == player1 ? "Player 1" : bat == player2 ? "Player 2" : "Player 3"));
                if (bat == player1) {
                    server.broadcast(BALL_HIT_BAT1);
                } else if (bat == player2) {
                    server.broadcast(BALL_HIT_BAT2);
                } else if (bat == player3) {
                    server.broadcast(BALL_HIT_BAT3);
                }
            }
        };
        

        getPhysicsWorld().addCollisionHandler(ballBatHandler);
        getPhysicsWorld().addCollisionHandler(ballBatHandler.copyFor(EntityType.BALL, EntityType.ENEMY_BAT));
        getPhysicsWorld().addCollisionHandler(ballBatHandler.copyFor(EntityType.BALL, EntityType.EXTRA_BAT));


        CollisionHandler ballpowerUpHandler = new CollisionHandler(EntityType.BALL, EntityType.POWER_UP) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity powerUp) {
                powerUp.removeFromWorld();
                powerUpActive = false; 
                BallComponent ballComponent = ball.getComponent(BallComponent.class);
                ballComponent.slowDown();
                server.broadcast(BALL_HIT_powerUp);
        
        }
            
        };
        getPhysicsWorld().addCollisionHandler(ballpowerUpHandler);
        
        
      
    }

    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1score").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2score").asString());
        controller.getLabelScoreExtra().textProperty().bind(getip("player3score").asString());

        getGameScene().addUI(ui);
    }
    



    @Override
    protected void onUpdate(double tpf) {
        if (!server.getConnections().isEmpty()) {
            var message = "GAME_DATA," + player1.getY() + "," + player2.getY() + "," + player3.getX() + "," + ball.getX() + "," + ball.getY() + "," + getPowerUpData();
            server.broadcast(message);
        }

 

        // Check if the game should end
    boolean endGame = geti("player1score") >= 10 || geti("player2score") >= 10 || geti("player3score") >= 10;
    if (endGame) {

        //server.broadcast(message);
    }



   
    }


    private String getPowerUpData() {
        if (powerUp.isActive()) {
            return powerUp.getX() + "," + powerUp.getY() + ",1"; // 1 indicates active
        } else {
            return "0,0,0"; // 0,0,0 indicates inactive (0 for X, Y, and active status)
        }
    }
    

    private void initScreenBounds() {
        Entity walls = entityBuilder()
                .type(EntityType.WALL)
                .collidable()
                .buildScreenBounds(150);

        getGameWorld().addEntity(walls);
    }

private void initGameObjects() {
    ball = spawn("ball", getAppWidth() / 2 - 30, 0);
    player1 = spawn("bat", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 - 30).put("playerId", 1));
    player2 = spawn("bat", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 - 30).put("playerId", 2));
    player3 = spawn("bat", new SpawnData(getAppWidth() / 2 - 60 / 2, getAppHeight() - 50 - 30).put("playerId", 3));
    powerUp = spawn("powerUp", new SpawnData(getAppWidth() / 2, getAppHeight() / 2));
    powerUpActive = true;

    player1Bat = player1.getComponent(BatComponent.class);
    player2Bat = player2.getComponent(BatComponent.class);
    player3Bat = player3.getComponent(BatComponent.class);
}


    private void playHitAnimation(Entity bat) {
        animationBuilder()
                .autoReverse(true)
                .duration(Duration.seconds(0.5))
                .interpolator(Interpolators.BOUNCE.EASE_OUT())
                .rotate(bat)
                .from(FXGLMath.random(-25, 25))
                .to(0)
                .buildAndPlay();
    }

    @Override
    public void onReceive(Connection<String> connection, String message) {
        var tokens = message.split(",");
    
        Arrays.stream(tokens).skip(1).forEach(key -> {
            if (key.endsWith("_DOWN")) {
                getInput().mockKeyPress(KeyCode.valueOf(key.substring(0, 1)));
            } else if (key.endsWith("_UP")) {
                getInput().mockKeyRelease(KeyCode.valueOf(key.substring(0, 1)));
            } 
        });
    }
    
        
            
            
    
    


    static class MessageWriterS implements TCPMessageWriter<String> {

        private OutputStream os;
        private PrintWriter out;

        MessageWriterS(OutputStream os) {
            this.os = os;
            out = new PrintWriter(os, true);
        }

        @Override
        public void write(String s) throws Exception {
            out.print(s.toCharArray());
            out.flush();
        }
    }

    static class MessageReaderS implements TCPMessageReader<String> {

        private BlockingQueue<String> messages = new ArrayBlockingQueue<>(50);

        private InputStreamReader in;

        MessageReaderS(InputStream is) {
            in =  new InputStreamReader(is);

            var t = new Thread(() -> {
                try {

                    char[] buf = new char[36];

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        var message = new String(Arrays.copyOf(buf, len));

                        System.out.println("Recv message: " + message);

                        messages.put(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.setDaemon(true);
            t.start();
        }

        @Override
        public String read() throws Exception {
            return messages.take();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

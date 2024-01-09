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

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.beans.binding.Bindings;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PongFactory implements EntityFactory {

    @Spawns("ball")
    public Entity newBall(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().density(0.3f).restitution(1.0f));
        physics.setOnPhysicsInitialized(() -> physics.setLinearVelocity(5 * 60, -5 * 60));

        var endGame = getip("player1score").isEqualTo(10).or(getip("player2score").isEqualTo(10).or (getip("player3score").isEqualTo(10)));

        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
        emitter.startColorProperty().bind(
                Bindings.when(endGame)
                        .then(Color.LIGHTYELLOW)
                        .otherwise(Color.LIGHTYELLOW)
        );

        emitter.endColorProperty().bind(
                Bindings.when(endGame)
                        .then(Color.RED)
                        .otherwise(Color.LIGHTBLUE)
        );

        emitter.setBlendMode(BlendMode.SRC_OVER);
        emitter.setSize(5, 10);
        emitter.setEmissionRate(1);

        return entityBuilder(data)
                .type(EntityType.BALL)
                .bbox(new HitBox(BoundingShape.circle(5)))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new ParticleComponent(emitter))
                .with(new BallComponent())
                .build();
    }

    @Spawns("bat")
    public Entity newBat(SpawnData data) {
        int playerId = data.get("playerId");
    
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);

        Color batColor;
    
        EntityType entityType;
        switch (playerId) {
            case 1:
                entityType = EntityType.PLAYER_BAT;
                batColor = Color.RED;
                 return entityBuilder(data)
                .type(entityType)
                .viewWithBBox(new Rectangle(20, 100, batColor))
                .with(new CollidableComponent(true))
                .with(physics)
                .with(new BatComponent())
                .build();
                
            case 2:
                entityType = EntityType.ENEMY_BAT;
                batColor = Color.BLUE;
                 return entityBuilder(data)
                .type(entityType)
                .viewWithBBox(new Rectangle(20, 100, batColor))
                .with(new CollidableComponent(true))
                .with(physics)
                .with(new BatComponent())
                .build();
             
            case 3:
                entityType = EntityType.EXTRA_BAT;
                batColor = Color.GREEN;
                return entityBuilder(data)
                .type(entityType)
                .viewWithBBox(new Rectangle(100, 20, batColor)) // Width 60, Height 20
                   .with(new CollidableComponent(true))
                .with(physics)
                .with(new BatComponent())
                .build();
             
            default:
                throw new IllegalArgumentException("Unknown player ID: " + playerId);
        }
    
  
    }



    @Spawns("powerUp") 
    public Entity powerUp(SpawnData data) {
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityType.POWER_UP)
                .viewWithBBox(new Rectangle(40, 40, Color.RED))
                .with(new CollidableComponent(true))
                .with(physics)
                .build();
    }
    
    
}

package com.example.bhop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class BhopClient implements ClientModInitializer {

    private static double bhopBoost = 0.0;
    private static int ticksSinceGround = 0;
    private static boolean wasOnGround = false;
    private static float lastYaw = 0f;

    private static final double MAX_NORMAL = 0.4; // ~8 BPS
    private static final double MAX_TURN = 0.6;   // ~12 BPS

    private static final int TIMING_WINDOW = 10;
    private static final int RESET_TIME = 40;

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            PlayerEntity player = client.player;

            boolean onGround = player.isOnGround();

            if (onGround) {
                ticksSinceGround++;
            } else {
                ticksSinceGround = 0;
            }

            // reset if too long
            if (ticksSinceGround > RESET_TIME) {
                bhopBoost = 0;
            }

            float yaw = player.getYaw();
            float yawDelta = Math.abs(yaw - lastYaw);
            lastYaw = yaw;

            boolean turning = yawDelta > 3.0;

            // hop gain
            if (!wasOnGround && onGround) {
                if (ticksSinceGround <= TIMING_WINDOW) {

                    double gain = 0.45;

                    if (turning) {
                        gain *= 2.0;
                    }

                    bhopBoost += gain;
                } else {
                    bhopBoost *= 0.65; // penalty
                }
            }

            double max = turning ? MAX_TURN : MAX_NORMAL;
            bhopBoost = Math.min(bhopBoost, max);

            // apply velocity
            Vec3d vel = player.getVelocity();
            Vec3d horizontal = new Vec3d(vel.x, 0, vel.z);

            double len = horizontal.length();

            if (len > 0.0001) {
                Vec3d dir = horizontal.normalize();
                Vec3d newVel = dir.multiply(len + bhopBoost);
                player.setVelocity(newVel.x, vel.y, newVel.z);
            }

            wasOnGround = onGround;
        });
    }
}


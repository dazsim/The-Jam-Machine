package org.atomicworkshop.jammachine.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.atomicworkshop.jammachine.JamMachineMod;


public final class CollisionMaths {
    private CollisionMaths() {}

    /**
     * Calculates the point on a plane where a line intersects
     * @param head the point where the player's head is
     * @param look the vector that the player is looking
     * @param planeOrigin the center of the plane
     * @param planeNormal the normal of the plane
     * @return the point relative to the origin that the look vector intersects, or null if it does not.
     */
    public static Vec3 intersectionLinePlane(Vec3 head, Vec3 look, Vec3 planeOrigin, Vec3 planeNormal)
    {
        final double dot = planeNormal.dot(look);
        if (Mth.abs((float)dot) > 1.0e-6) {
            /*
             *  0-1.0 = intersection
             *  <1.0  = in front of plane
             *  <0    = behind plane
             */
            final Vec3 w = head.subtract(planeOrigin);
            final double factor = -planeNormal.dot(w) / dot;

            return head.add(look.scale(factor));
        } else {
            return null;
        }
    }

    public static Vec3 calculateSlopeHit(BlockPos pos, Direction blockFacing, Player player)
    {
        final Vec3 headPosition = getPlayerHeadPosition(player);
        final Vec3 lookVector = getPlayerLookVector(player);

        //FIXME: rotate Origin according to block direction
        //Steven: My brain is fried and I should be able to solve this with a matrix, but for various reasons
        //I've decided to brute force it for the modjam release. Need to find a server-friendly Matrix4f that
        //can work properly with Vec3, or find another vector class that works with matrices.
        final Vec3 planeOrigin;
        final Vec3 bottomLeft;
        final Vec3 bottomRightCorner;
        switch (blockFacing)
        {
            case NORTH:
                planeOrigin = new Vec3(pos.getX() + 1, pos.getY() + 7.5f / 16.0f, pos.getZ() + 15.75f / 16.0f);
                bottomLeft = new Vec3(pos.getX() + 1, pos.getY() + 1 / 16.0f, pos.getZ());
                bottomRightCorner = new Vec3(pos.getX(), pos.getY() + 1 / 16.0f, pos.getZ());
                break;
            case SOUTH:
                planeOrigin = new Vec3(pos.getX(), pos.getY() + 7.5f / 16.0f, pos.getZ() + (1 - (15.75f / 16.0f)));
                bottomLeft = new Vec3(pos.getX(), pos.getY() + 1 / 16.0f, pos.getZ() + 1);
                bottomRightCorner = new Vec3(pos.getX() + 1, pos.getY() + 1 / 16.0f, pos.getZ() + 1);
                break;
            case WEST:
                planeOrigin = new Vec3(pos.getX() + 15.75f / 16.0f, pos.getY() + 7.5f / 16.0f, pos.getZ());
                bottomLeft = new Vec3(pos.getX(), pos.getY() + 1 / 16.0f, pos.getZ());
                bottomRightCorner = new Vec3(pos.getX(), pos.getY() + 1 / 16.0f, pos.getZ() + 1);
                break;
            case EAST:
                planeOrigin = new Vec3(pos.getX() + (1 - (15.75f / 16.0f)), pos.getY() + 7.5f / 16.0f, pos.getZ() + 1);
                bottomLeft = new Vec3(pos.getX() + 1, pos.getY() + 1 / 16.0f, pos.getZ() + 1);
                bottomRightCorner = new Vec3(pos.getX() + 1, pos.getY() + 1 / 16.0f, pos.getZ());
                break;
            default:
                return null;
        }

        //build matrix to rotate
        //GlStateManager.translate(0.5, 0.0, 0.5);
        //GlStateManager.rotate(), 0.0f, 1.0f, 0.0f);
        //GlStateManager.translate(-0.5, 0.0, -0.5);


        final Vec3 u = bottomLeft.subtract(planeOrigin);
        final Vec3 v = bottomRightCorner.subtract(planeOrigin);
        final Vec3 planeNormal = u.cross(v);

        final Vec3 vector3d = intersectionLinePlane(headPosition, lookVector, planeOrigin, planeNormal);

        if (vector3d == null) {
            JamMachineMod.LOGGER.info("player missed");
            return null;
        } else
        {
            final Vec3 hitVector = vector3d.subtract(pos.getX(), pos.getY(), pos.getZ());

            final Vec3 finalHitVector;
            //FIXME: HAAAAACCCKKKKK
            switch (blockFacing)
            {
                case NORTH:
                    finalHitVector = new Vec3(1-hitVector.x, hitVector.y, 1-hitVector.z);
                    break;
                case SOUTH:
                    finalHitVector = hitVector;
                    break;
                case WEST:
                    finalHitVector = new Vec3(hitVector.z, hitVector.y, 1-hitVector.x);
                    break;
                case EAST:
                    finalHitVector = new Vec3(1-hitVector.z, hitVector.y, hitVector.x);
                    break;
                default:
                    finalHitVector = hitVector;
            }
            JamMachineMod.LOGGER.info("player clicked at {},{},{}", finalHitVector.x, finalHitVector.y, finalHitVector.z);
            return finalHitVector;
        }

    }

    public static Vec3 getPlayerLookVector(Player playerIn)
    {
        final float playerPitch = playerIn.getXRot();
        final float playerYaw = playerIn.getYRot();

        final float yawCosine = Mth.cos((float)(-playerYaw * (Math.PI / 180) - Math.PI));
        final float yawSine = Mth.sin((float)(-playerYaw * (Math.PI / 180) - Math.PI));
        final float pitchCosine = -Mth.cos((float)(-playerPitch * (Math.PI / 180)));
        final float pitchSine = Mth.sin((float)(-playerPitch * (Math.PI / 180)));

        final float lookX = yawSine * pitchCosine;
        final float lookZ = yawCosine * pitchCosine;
        final float playerReach = (float)playerIn.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        final Vec3 lookVector = new Vec3(
                lookX * playerReach,
                pitchSine * playerReach,
                lookZ * playerReach
        );
        return lookVector;
    }

    public static Vec3 getPlayerHeadPosition(Player playerIn)
    {
        final Vec3 headPosition = new Vec3(
                playerIn.getX(),
                playerIn.getEyeY(),
                playerIn.getZ()
        );
        return headPosition;
    }
}
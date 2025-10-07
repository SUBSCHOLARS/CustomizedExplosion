package com.example.examplemod.explosion;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class CustomExplosion extends Explosion {
    // privateなフィールドにアクセスできないため、自クラスで値を保持する
    private final float radius;
    private final ExplosionDamageCalculator damageCalculator;
    private final float powerFactor;
    @Nullable private final Player player;
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();

    public CustomExplosion(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource pDamageSource, @Nullable ExplosionDamageCalculator pDamageCalculator, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, boolean pFire, Explosion.BlockInteraction pBlockInteraction,float pPowerFactor, @Nullable Player player){
        super(pLevel,pSource,pDamageSource,pDamageCalculator,pToBlowX,pToBlowY,pToBlowZ,pRadius,pFire,pBlockInteraction);
        this.radius = pRadius;
        this.damageCalculator = pDamageCalculator == null ? this.makeDamageCalculator(pSource) : pDamageCalculator;
        this.powerFactor=pPowerFactor;
        this.player=player;
    }
    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity pEntity) {
        return pEntity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(pEntity);
    }

    @Override
    public void explode() {
        Vec3 playerLookVec= Objects.requireNonNull(player).getLookAngle();
        Level level= Objects.requireNonNull(this.getExploder()).getLevel();
        Entity source=this.getExploder();
        double x=this.getPosition().x;
        double y=this.getPosition().y;
        double z=this.getPosition().z;
        level.gameEvent(source, GameEvent.EXPLODE,new BlockPos(x,y,z));
        Set<BlockPos> set= Sets.newHashSet();

        for(int j=0; j<16; j++){
            for(int k=0; k<16; k++){
                for(int l=0; l<16; l++){
                    if(j==0||j==15||k==0||k==15||l==0||l==15){
                        double d0=(double) j/15.0D*2.0D-1.0D;
                        double d1=(double) k/15.0D*2.0D-1.0D;
                        double d2=(double) l/15.0D*2.0D-1.0D;
                        double d3=Math.sqrt(d0*d0+d1*d1+d2*d2);
                        d0/=d3;
                        d1/=d3;
                        d2/=d3;
                        //爆発の威力減衰に使用される値
                        float initialStrength=this.radius*(0.7F+level.random.nextFloat()*0.6F);
                        float f=initialStrength;
                        Vec3 rayVector=new Vec3(d0,d1,d2);
                        double dotProduct=playerLookVec.dot(rayVector);
                        if(dotProduct>0.7){
                            f=initialStrength*3.0F;
                        }
                        double d4=x;
                        double d6=y;
                        double d8=z;

                        for(float f1=0.3F; f>0.0F; f-=0.22500001F){
                            BlockPos blockPos=new BlockPos(d4,d6,d8);
                            BlockState blockState=level.getBlockState(blockPos);
                            FluidState fluidState=level.getFluidState(blockPos);
                            if(!level.isInWorldBounds(blockPos)){
                                break;
                            }
                            Optional<Float> optional=this.damageCalculator.getBlockExplosionResistance(this,level,blockPos,blockState,fluidState);
                            if(optional.isPresent()){
                                float resistance=optional.get();
                                f-=(resistance/powerFactor+0.3F)*0.3F;
                            }
                            if(f>0.0F&&this.damageCalculator.shouldBlockExplode(this,level,blockPos,blockState,f)){
                                set.add(blockPos);
                            }
                            d4 += d0 * (double)0.3F;
                            d6 += d1 * (double)0.3F;
                            d8 += d2 * (double)0.3F;
                        }
                    }
                }
            }
        }

        this.getToBlow().addAll(set);
        float f2=this.radius*2.0F;
        int k1= Mth.floor(x-(double) f2-1.0D);
        int l1 = Mth.floor(x + (double)f2 + 1.0D);
        int i2 = Mth.floor(y - (double)f2 - 1.0D);
        int i1 = Mth.floor(y + (double)f2 + 1.0D);
        int j2 = Mth.floor(z - (double)f2 - 1.0D);
        int j1 = Mth.floor(z + (double)f2 + 1.0D);

        List<Entity> list=level.getEntities(source, new AABB(k1, i2, j2, l1, i1, j1));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(level, this, list, f2);
        Vec3 vec3 = new Vec3(x, y, z);
        for (Entity entity : list) {
            if (entity instanceof Player) {
                continue;
            }
            if (!entity.ignoreExplosion()) {
                double d12 = Math.sqrt(entity.distanceToSqr(vec3)) / f2;
                if (d12 < 1.0D) {
                    double d5 = entity.getX() - x;
                    double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - y;
                    double d9 = entity.getZ() - z;
                    double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                    if (d13 != 0.0D) {
                        d5 /= d13;
                        d7 /= d13;
                        d9 /= d13;
                        double d14 = getSeenPercent(vec3, entity);
                        double d10 = (1.0D - d12) * d14;
                        entity.hurt(this.getDamageSource(), (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f2 + 1.0D)));
                        double d11 = d10;
                        if (entity instanceof LivingEntity) {
                            d11 = ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity) entity, d10);
                        }

                        entity.setDeltaMovement(entity.getDeltaMovement().add(d5 * d11, d7 * d11, d9 * d11));
                    }
                }
            }
        }
    }
}

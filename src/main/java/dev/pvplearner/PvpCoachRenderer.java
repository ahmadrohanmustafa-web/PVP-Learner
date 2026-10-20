package dev.pvplearner;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/** Renders the sparring zombie's AI and hitbox with Minecraft's Steve player model. */
public final class PvpCoachRenderer extends MobEntityRenderer<PvpCoachEntity, PlayerEntityModel<PvpCoachEntity>> {
    private static final Identifier STEVE = Identifier.ofVanilla("textures/entity/player/wide/steve.png");

    public PvpCoachRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(PvpCoachEntity entity) {
        return STEVE;
    }
}

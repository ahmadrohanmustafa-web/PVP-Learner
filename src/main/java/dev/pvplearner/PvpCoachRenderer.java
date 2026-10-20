package dev.pvplearner;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;

/** Renders the coach using the standard Steve player model. */
public final class PvpCoachRenderer extends LivingEntityRenderer<PvpCoachEntity, PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier STEVE = Identifier.ofVanilla("textures/entity/player/wide/steve.png");

    public PvpCoachRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        return STEVE;
    }
}

package net.world.downloader.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.text.Text;
import net.world.downloader.WDL;
import net.world.downloader.WDLHooks;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	// Automatic remapping sometimes fails; see
	// https://github.com/Pokechu22/WorldDownloader/issues/175
	@Shadow(remap = false, aliases = {"f", "field_147300_g"})
	private ClientWorld world;

	@Inject(method="onUnloadChunk", at=@At("HEAD"))
	private void onProcessChunkUnload(UnloadChunkS2CPacket packetIn, CallbackInfo ci) {
		/* WDL >>> */
		WDLHooks.onNHPCHandleChunkUnload((ClientPlayNetworkHandler)(Object)this, this.world, packetIn);
		/* <<< WDL */
		//more down here
	}
	
	@Inject(method="onDisconnected", at=@At("HEAD"))
	private void onDisconnect(Text reason, CallbackInfo ci) {
		/* WDL >>> */
		WDLHooks.onNHPCDisconnect((ClientPlayNetworkHandler)(Object)this, reason);
		/* <<< WDL */
		//more down here
	}
	
	@Inject(method="onGameMessage", at=@At("RETURN"))
	private void onHandleChat(GameMessageS2CPacket p_147251_1_, CallbackInfo ci) {
		//more up here
		/* WDL >>> */
		WDLHooks.onNHPCHandleChat((ClientPlayNetworkHandler)(Object)this, p_147251_1_);
		/* <<< WDL */
	}
	
	@Inject(method="onBlockEvent", at=@At("RETURN"))
	private void onHandleBlockAction(BlockEventS2CPacket packetIn, CallbackInfo ci) {
		//more up here
		/* WDL >>> */
		WDLHooks.onNHPCHandleBlockAction((ClientPlayNetworkHandler)(Object)this, packetIn);
		/* <<< WDL */
	}
	
	@Inject(method="onMapUpdate", at=@At("RETURN"))
	private void onHandleMaps(MapUpdateS2CPacket packetIn, CallbackInfo ci) {
		//more up here
		/* WDL >>> */
		WDLHooks.onNHPCHandleMaps((ClientPlayNetworkHandler)(Object)this, packetIn);
		/* <<< WDL */
	}
	
	@Inject(method="onCustomPayload", at=@At("HEAD"))
	private void onHandleCustomPayload(CustomPayloadS2CPacket packetIn, CallbackInfo ci) {
		// Inject at HEAD because otherwise Forge will read the packet content first,
		// which irreversibly clears the buffer (without doing other weird hacky things)
		/* WDL >>> */
		WDLHooks.onNHPCHandleCustomPayload((ClientPlayNetworkHandler)(Object)this, packetIn);
		/* <<< WDL */
		//more down here
	}
	
	@Inject(method="onGameJoin", at=@At(value="FIELD", shift=Shift.AFTER, target="Lnet/minecraft/client/"
			+ "network/ClientPlayNetworkHandler;world:Lnet/minecraft/client/world/ClientWorld;",
			opcode=Opcodes.PUTFIELD, ordinal=0))
	private void onHandleGameJoin(GameJoinS2CPacket packetIn, CallbackInfo ci) {
		/* WDL >>> */
		WDL.onWorldLoad(this.world);
		/* <<< WDL */
		//more down here
	}

	@Inject(method="onGameJoin", at=@At(value="FIELD", shift=Shift.AFTER, target="Lnet/minecraft/client/"
			+ "network/ClientPlayNetworkHandler;world:Lnet/minecraft/client/world/ClientWorld;",
			opcode=Opcodes.PUTFIELD, ordinal=0))
	private void onHandlePlayerRespawn(PlayerRespawnS2CPacket packetIn, CallbackInfo ci) {
		/* WDL >>> */
		WDL.onWorldLoad(this.world);
		/* <<< WDL */
		//more down here
	}
}

package us.spaceclouds42.wurst_dupe_patch.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;

@Mixin(ServerPlayNetworkHandler.class)
abstract class AntiDupeMixin {
    @Shadow public ServerPlayerEntity player;
    @Shadow @Final private MinecraftServer server;

    /**
     * @author wusrt dupe patch
     * @reason mojank
     */
    @Inject(
            method = "onBookUpdate",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    public void onBookUpdate(BookUpdateC2SPacket packet, CallbackInfo ci) {
        boolean caughtDupe = false;
        ItemStack itemStack = packet.getBook();

        // Prevents null pointers
        if (itemStack.isOf(Items.WRITABLE_BOOK)) {
            NbtCompound nbtCompound = itemStack.getTag();
            if (WritableBookItem.isValid(nbtCompound)) {

                // Checks for impossible titles
                if (packet.wasSigned()) {
                    String title = nbtCompound.getString("title");
                    if (title.length() > 16) {
                        caughtDupe = true;
                    }
                }

                // Checks for impossible page content
                NbtList nbtList = nbtCompound.getList("pages", 8);
                for (int j = 0; j < nbtList.size(); ++j) {
                    String page = nbtList.getString(j);
                    if (page.length() > 2000) {
                        caughtDupe = true;
                    }
                }
            }
        }

        if (caughtDupe) {
            dupeCatchMessageLogger();
            ci.cancel();
        }
    }

    private void dupeCatchMessageLogger() {
        System.out.println("CAUGHT [" + player.getEntityName() + "] MANIPULATING BOOK UPDATE PACKETS!");
        System.out.println("COORDS: " + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ());
        System.out.println("DIMENSION: " + player.getEntityWorld().getRegistryKey().getValue().getPath());
        System.out.println("TIMESTAMP: " + LocalTime.now());

        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        String world = player.getEntityWorld().getRegistryKey().getValue().toString();

        MutableText chatMsg = new LiteralText(player.getEntityName()).formatted(Formatting.YELLOW).append(
                new LiteralText(" thought they could get away with duping!").formatted(Formatting.RED).append(
                        new LiteralText("\n[Click to teleport to where the player tried to dupe]").formatted(Formatting.DARK_GREEN)
                )
        );

        Text msg = chatMsg.styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in "+world+" run tp @s "+x+" "+y+" "+z+"")));

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.hasPermissionLevel(2)) {
                p.sendMessage(msg, false);
            }
        }
    }
}

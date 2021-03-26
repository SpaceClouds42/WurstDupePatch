package us.spaceclouds42.wurst_dupe_patch.mixin;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerPlayNetworkHandler.class)
abstract class AntiDupeMixin {
    @Shadow protected abstract void filterTexts(List<String> texts, Consumer<List<TextStream.Message>> consumer);
    @Shadow protected abstract void addBook(TextStream.Message title, List<TextStream.Message> pages, int slotId);
    @Shadow protected abstract void updateBookContent(List<TextStream.Message> pages, int slotId);
    @Shadow public ServerPlayerEntity player;
    @Shadow @Final private MinecraftServer server;

    // This is the character the wurst uses to cause the packet overload
    private final String illegalChar = Character.toString((char) 2077);

    // This is the title that wurst uses in the packet
    private final String illegalTitle = "If you can see this, it didn't work";

    /**
     * @author wusrt dupe patch
     * @reason mojank
     */
    @Overwrite
    public void onBookUpdate(BookUpdateC2SPacket packet) {
        boolean caughtDupe = false;
        int i = packet.getSlot();
        if (PlayerInventory.isValidHotbarIndex(i) || i == 40) {
            ItemStack itemStack = packet.getBook();
            if (itemStack.isOf(Items.WRITABLE_BOOK)) {
                NbtCompound nbtCompound = itemStack.getTag();
                if (WritableBookItem.isValid(nbtCompound)) {
                    List<String> list = Lists.newArrayList();
                    boolean bl = packet.wasSigned();
                    if (bl) {
                        String title = nbtCompound.getString("title");
                        if (title.equals(illegalTitle)) {
                            caughtDupe = true;
                            list.add("No duping allowed!");
                        } else {
                            list.add(title);
                        }
                    }

                    NbtList nbtList = nbtCompound.getList("pages", 8);

                    for(int j = 0; j < nbtList.size(); ++j) {
                        String page = nbtList.getString(j);
                        if (page.contains(illegalChar)) {
                            caughtDupe = true;
                            list.add("nice try");
                        } else if (page.contains("Wurst!!!Wurst!!!Wurst!!!Wurst!!!")) {
                            list.add("lol you're bad, get banned");
                        } else {
                            list.add(page);
                        }
                    }

                    this.filterTexts(list, bl ? (listx) -> {
                        this.addBook((TextStream.Message)listx.get(0), listx.subList(1, listx.size()), i);
                    } : (listx) -> {
                        this.updateBookContent(listx, i);
                    });
                }
            }
        }

        if (caughtDupe) {
            System.out.println("CAUGHT " + player.getEntityName() + " USING WURST .DUPE COMMAND!");
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
}

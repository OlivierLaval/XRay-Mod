package com.xray.common.reference.block;

import com.xray.common.utils.OutlineColor;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

public class BlockStore {

    private HashMap<String, Deque<BlockData>> store = new HashMap<>();
    private List<Integer> drawStore = new ArrayList<>();

    // This is used to avoid having to scan the BlockData used in this.store.list<BlockData>
    public List<String> defaultStore = new ArrayList<>();

    public boolean putBlock(String key, BlockData data) {
        if (this.defaultStore.contains(key))
            return false;

        // Start a list if we're the first instance
        if (!this.store.containsKey(key)) {
            Deque<BlockData> list = new LinkedList<>();
            list.add(data);
            this.store.put(key, list);
        } else {
            // If we're adding a default it's possible that the list already contains alternative
            // versions of the same block. if this happens the render will pick the first index
            // and use that version of the block to render. To solve this we simply insert to the
            // start of the list :D
            if (data.isDefault())
                this.store.get(key).addFirst(data);
            else {
                if( !this.store.get(key).contains(data) )
                    this.store.get(key).add(data);
                else
                    return false;
            }
        }
        if (data.isDefault())
            this.defaultStore.add(key);

        if( data.isDrawing() )
            this.drawStore.add(Block.getStateId(data.getState()));

        return true;
    }

    public HashMap<String, Deque<BlockData>> getStore() {
        return store;
    }

    public List<Integer> getDrawStore() {
        return drawStore;
    }

    public void toggleDrawing( String key, BlockData block ) {
        if( !this.store.containsKey(key) )
            return;

        Deque<BlockData> data = this.store.get(key);
        if( block.isDefault() ) {
            data.getFirst().drawing = !data.getFirst().drawing;
            this.updateDrawStore(data.getFirst().drawing, Block.getStateId(block.getState()));
            return;
        }

        for ( BlockData d : data ) {
            if (d.getState() == block.getState()) {
                d.drawing = !d.drawing;
                this.updateDrawStore(d.drawing, Block.getStateId(block.getState()));
                break; // We're done. Lets not waste time
            }
        }
    }

    private void updateDrawStore(boolean addRemove, int stateId) {
        if( addRemove )
            this.drawStore.add(stateId);
        else
            this.drawStore.remove(this.drawStore.indexOf(stateId));
    }

    public boolean defaultContains(String key) {
        return this.defaultStore.contains(key);
    }

    /**
     * Helper for creating default config from ore dictionary names.
     * Given an ore name, tries to find an actual instance of such ore.
     * @param name OreDictionary name (eg. oreIron)
     * @param color a color for this ore
     * @param draw shall we draw it by default?
     * @return An ore registered with this dictionary name, null if none found
     */
    public static BlockData firstOreInDictionary( String name, int[] color, boolean draw )
    {
        NonNullList<ItemStack> ores = OreDictionary.getOres( name );
        if ( ores.isEmpty() || ores.get(0).isEmpty() )
            return null;

        ItemStack stack = ores.get( 0 );

        return new BlockData(
                stack.getItem().getRegistryName(),
                stack.getDisplayName(),
                new OutlineColor(color[0], color[1], color[2]),
                true,
                Block.getBlockFromItem(stack.getItem()).getDefaultState(),
                stack,
                draw
        );
    }


    /**
     * Used for debugging. Shouldn't be used in released version
     * TODO: add support for automatically disabling
     */
    public void printStore() {
        System.out.println("----==============================================----");
        System.out.println("-> Block Store");
        for (Map.Entry<String, Deque<BlockData>> data:
                this.store.entrySet()) {

            System.out.println(String.format("-> [%s]", data.getKey()));
            for (BlockData block : data.getValue() ) {
                System.out.println(String.format("---> [%b, %s, %s, %s]", block.isDefault(), block.getState().toString(), block.getOutline().getBlue(), block.getName()));
            }
        }
        System.out.println("-> Draw Store");
        for (int id : this.drawStore) {
            System.out.println(String.format("---> [%s]", String.valueOf(id)));
        }
        System.out.println("----==============================================----");
    }
}

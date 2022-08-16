package com.wolfyscript.bundleinv;

import com.google.common.base.Objects;
import com.wolfyscript.bundleinv.util.collection.IndexedSortedArraySet;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Clearable;

public class PlayerBundleStorage implements Clearable {

    private final PlayerEntity player;
    private final IndexedSortedArraySet<ItemStack> stacks;
    private final PlayerInventory inventory;
    private boolean open;

    private final int capacity;
    private int load;

    public PlayerBundleStorage(PlayerInventory inventory, PlayerEntity player) {
        this.inventory = inventory;
        this.player = player;
        this.capacity = 64 * 32; // 2048 (32 standard slots) space for all kinds of items
        this.stacks = IndexedSortedArraySet.create((mid, target) -> {
            int rawIdMid = Item.getRawId(mid.getItem());
            int rawIdTarget = Item.getRawId(target.getItem());
            if (rawIdMid == rawIdTarget) {
                if (!ItemStack.canCombine(mid, target)) {
                    return 1;
                }
                return 0;
            }
            return rawIdTarget > rawIdMid ? 1 : -1;
        });
        this.load = 0;
    }

    public int size() {
        return stacks.size();
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public IndexedSortedArraySet<ItemStack> getStacks() {
        return stacks;
    }

    public ItemStack removeStack(ItemStack stack, int amount) {
        int index = stacks.indexOf(stack);
        if (index < 0) return ItemStack.EMPTY;
        ItemStack target = stacks.get(index);
        if (target == null || target.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = target.split(amount);
        load -= getItemOccupancy(removed) * removed.getCount();
        if (target.isEmpty()) {
            stacks.remove(index);
        }
        inventory.markDirty();
        return removed;
    }

    /**
     * Removes the max possible amount of the item.<br>
     * The amount is the minimum of the actual items count and max count.
     *
     * @param stack The ItemStack to remove.
     * @return The removed ItemStack; or EMPTY when index is out of bounds or item is empty.
     */
    public ItemStack removeMaxStack(ItemStack stack) {
        return removeStack(stack, stack.getMaxCount());
    }

    public ItemStack removeStack(int index, int amount) {
        if (size() <= index || index < 0) return ItemStack.EMPTY;
        return removeStack(get(index), amount);
    }

    /**
     * Removes the max possible amount of the item, at that index in the Bundle.<br>
     * The amount is the minimum of the actual items count and max count.
     *
     * @param index The index of the item in the Bundle.
     * @return The removed ItemStack; or EMPTY when index is out of bounds or item is empty.
     */
    public ItemStack removeMaxStack(int index) {
        if (size() <= index || index < 0) return ItemStack.EMPTY;
        ItemStack toRemove = get(index);
        return removeStack(toRemove, toRemove.getMaxCount());
    }

    @Override
    public void clear() {
        stacks.clear();
    }

    /**
     * Gets the ItemStack at the specified index in the storage.
     *
     * @param index The index of the ItemStack.
     * @return The ItemStack at the index in the storage; otherwise {@link ItemStack#EMPTY} if item not available.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public ItemStack get(int index) {
        ItemStack itemStack = stacks.get(index);
        if (itemStack == null) {
            return ItemStack.EMPTY;
        }
        return itemStack;
    }

    public int addStack(ItemStack stack) {
        int count = stack.getCount();
        int occupancy = getItemOccupancy(stack);
        int maxCount = getRemainingCapacity() / occupancy;

        ItemStack existing = stacks.getIfContains(stack);
        if (existing != null) {
            if (this.canStackAddMore(existing, stack) && maxCount > 0) {
                int countToAdd = Math.min(count, maxCount);
                count -= countToAdd;
                load += occupancy * countToAdd;
                existing.increment(countToAdd);
                existing.setBobbingAnimationTime(5);
                inventory.markDirty();
            }
        }
        maxCount = getRemainingCapacity() / occupancy;
        if (count > 0) {
            int newCount = Math.min(count, maxCount);
            ItemStack itemStack = stack.copy();
            itemStack.setCount(newCount);
            stacks.add(itemStack);
            load += occupancy * newCount;
            count -= newCount;
            inventory.markDirty();
        }
        return count;
    }

    public int getRemainingCapacity() {
        return capacity - load;
    }

    private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
        //Make sure the items can stack as much as possible to remove clutter
        //This is not the standard inventory where you can move items around anyway, so big stack sizes are ok.
        return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack) && this.getRemainingCapacity() >= getItemOccupancy(existingStack);
    }

    public int getSwappableHotbarSlotFor(ItemStack stack) {
        int slot;
        for (int i = 0; i < 9; ++i) {
            slot = (inventory.selectedSlot + i) % 9;
            if (inventory.main.get(slot).isEmpty()) return slot;
        }
        // Find the best item to swap with
        int stackLoad = PlayerBundleStorage.getItemStackLoad(stack);
        int[] loadDiffs = new int[9];
        for (int i = 0; i < 9; ++i) {
            slot = (inventory.selectedSlot + i) % 9;
            ItemStack slotStack = inventory.main.get(slot);
            int load = PlayerBundleStorage.getItemStackLoad(slotStack);
            loadDiffs[i] = load - stackLoad;
        }
        IntHeapIndirectPriorityQueue loadQueue = new IntHeapIndirectPriorityQueue(loadDiffs);
        for (int i = 0; i < 9; i++) {
            loadQueue.enqueue(i);
        }
        // We use a priority queue to swap the item, that requires the smallest amount of space in the bundle inventory and isn't enchanted.
        for (int i = 0; i < loadQueue.size(); i++) {
            slot = (inventory.selectedSlot + loadQueue.dequeue()) % 9;
            if (!inventory.main.get(slot).hasEnchantments()) return slot;
        }
        return inventory.selectedSlot;
    }

    public static int getItemOccupancy(ItemStack stack) {
        if (stack.isOf(Items.BUNDLE)) {
            return 4 + (int) Math.floor(BundleItem.getAmountFilled(stack) * 64);
        } else {
            if ((stack.isOf(Items.BEEHIVE) || stack.isOf(Items.BEE_NEST)) && stack.hasNbt()) {
                NbtCompound nbtCompound = BlockItem.getBlockEntityNbt(stack);
                if (nbtCompound != null && !nbtCompound.getList("Bees", 10).isEmpty()) {
                    return 64;
                }
            }
            return 64 / stack.getMaxCount();
        }
    }

    public static int getItemStackLoad(ItemStack stack) {
        return getItemOccupancy(stack) * stack.getCount();
    }

    public NbtCompound writeNbt(NbtCompound compound) {
        compound.putBoolean("open", open);
        NbtList items = new NbtList();
        stacks.forEach(itemStack -> {
            NbtCompound entry = new NbtCompound();
            itemStack.writeNbt(entry);
            items.add(entry);
        });
        compound.put("items", items);
        return compound;
    }

    public void readNbt(NbtCompound compound) {
        clear();
        setOpen(compound.getBoolean("open"));
        NbtList items = compound.getList("items", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < items.size(); i++) {
            NbtCompound element = items.getCompound(i);
            ItemStack itemStack = ItemStack.fromNbt(element);
            if (itemStack.isEmpty()) continue;
            addStack(itemStack);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerBundleStorage that = (PlayerBundleStorage) o;
        return capacity == that.capacity && load == that.load && Objects.equal(player, that.player) && Objects.equal(stacks, that.stacks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(player, stacks, capacity, load);
    }
}

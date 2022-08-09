package com.wolfyscript.bundleinv;

import com.google.common.base.Objects;
import com.wolfyscript.bundleinv.util.collection.IndexedSortedArraySet;
import java.util.Iterator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Clearable;

public class PlayerBundleStorage implements Clearable {

    private final PlayerEntity player;
    private final IndexedSortedArraySet<ItemStack> stacks;
    private final PlayerInventory inventory;

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

    public ItemStack removeStack(int slot, int amount) {
        if (size() <= slot || slot < 0) return ItemStack.EMPTY;
        int index = 0;
        Iterator<ItemStack> iterator = stacks.iterator();
        while(iterator.hasNext()) {
            ItemStack current = iterator.next();
            if (index == slot) {
                ItemStack removed = current.split(amount);
                load -= getItemOccupancy(removed) * removed.getCount();
                if (current.isEmpty()) {
                    iterator.remove();
                }
                return removed;
            }
            index++;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeStack(int slot) {
        if (size() <= slot || slot < 0) return ItemStack.EMPTY;
        int index = 0;
        Iterator<ItemStack> iterator = stacks.iterator();
        while(iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            if (index == slot) {
                int count = itemStack.getCount();
                if (count > itemStack.getMaxCount()) {
                    int maxCount = itemStack.getMaxCount();
                    load -= getItemOccupancy(itemStack) * maxCount;
                    itemStack.setCount(count - maxCount);

                    ItemStack returnValue = itemStack.copy();
                    returnValue.setCount(maxCount);
                    return returnValue;
                }
                load -= getItemOccupancy(itemStack) * count;
                iterator.remove();
                return itemStack;
            }
            index++;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clear() {
        stacks.clear();
    }

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
        return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack) && this.getRemainingCapacity() > getItemOccupancy(existingStack);
    }

    private static int getItemOccupancy(ItemStack stack) {
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

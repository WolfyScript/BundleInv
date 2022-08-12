# BundleInv
This mod as the name suggests combines the idea of the Bundle and inventory expansion, to improve the inventory of Minecraft.

It adds a whole secondary Bundle Inventory.  
This Bundle Inventory acts very similar to the Bundle, as it has a max amount of inventory space, but no limit on Item types.  
So you can store many types of blocks and items in that inventory and not worry about slots running out too fast.

## Storage & Stacking
The stacks require the same space as in bundles.  
The inventory has a limited Bundle Slot Capacity and items take up those slots based on their max-stack amount.
* Items that stack to 64 take up 1 slot per item.
* Items that stack to 16 take up 4 slots per item.
* Items that do not stack take up 64 slots per item.

So basically the same as the Bundle, but with a lot more slots at once.

## Small Size
In order to keep the inventory manageable the GUI for the Bundle Inventory takes up as little space as possible and leaves space for the status effect icons on smaller screens.  
It automatically hides when opening the Recipe Book.  
<img src="./docs/assets/open_recipe_book_and_effects.gif" height=240>

## Functionality
It is designed to integrate with the primary inventory and supports basic functionalities.  
You can drag and drop items in and out of the secondary and into the primary inventory, and the other way around.  
For example you can Left-click to store one item, and Right-click to store whole cursor stack.

### Hover Guide
When hovering over the BundleInv with an Item on your cursor, it'll show you the existing items already in the inventory.    
Or a Bundle icon in case there is nothing in it yet.  
That way you know exactly how much you already have of the item without searching for it.  
<img src="./docs/assets/hover_guide.gif" height=240>

## Search
Search items in the Bundle Inventory using the search bar on top.  
This takes the display name and lore of the item into account, so you can look for your renamed items.
<img src="./docs/assets/search.gif" height=240>

## Planned & WIP Features

### Quick move
#### Secondary Inventory
`Shift-Click`: secondary -> primary  
`Ctrl-Shift-Click`: secondary -> hotbar / upper-inventory

#### Primary Inventory  
`Ctrl-Shift-Click`: primary -> secondary (Since shift-click would move it into the hotbar)

#### Upper-Inventory
`Ctrl-Shift-Click`: upper inventory -> secondary (Since shift-click would move it into primary)

#### Pick-Block
Pick-Block takes the max possible stack-amount of the picked block out of secondary inventory and puts it into the hotbar, when there is space.
If there is no space, but the item you are holding is the item you picked, then it tries to up that stack to the max-stack possible.
Otherwise, switches cursor to item in hotbar.

## Possible Future Features
- Pin items, that are always shown first in the list.
- Prioritise secondary inventory (or single slots?) when picking up items.
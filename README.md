**FishOnTracker** is a tool designed to scan all custom items across **FishOnMC** server to get instant net worth value at [FishOnTracker Website](https://5e8a.github.io/FishOnTracker/)
# ❗Requirements
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [YetAnotherConfigLib](https://modrinth.com/mod/yacl)
# 📖Usage
1. While on FishOnMC server start scanning by typing **/fot scan start**
2. Open all chests and virtual containers you wanna include
3. When done type **/fot scan stop**
4. Go to [FishOnTracker Website](https://5e8a.github.io/FishOnTracker/) and click **📋 Quick Paste** button <br>
# ✨Features
- Scans Chests and virtual containers after opening them
- Exports detected items to clipboard and JSON file located in *config/fishontracker/exports*
- Start/stop a scan with a key bind *(configurable, also found in Options > Controls > Key Binds > FishOnTracker)*
- Optional profile data - username, level and cash can be left out of the export in the config
- Prevents duplicate scans of the same container
- Blocks scans from unintended containers such as Auction House, Menu etc.
- Editable blacklists - adjust excluded items/containers in *config/fishontracker/blacklist_items.txt* and *config/fishontracker/blacklist_containers.txt* without recompiling (delete a file to restore defaults)
- Automatically exports username, level, and cash balance
- Supported Items:
  - **Armor** (Type, Rating, Rarity)
  - **Pets** (Type, Rating, Rarity, Equipped Items, Equipped Skins, Equipped Trails)
  - **Pet Skins & Pet Trails** (standalone, unequipped cosmetics)
  - **Rod Parts** (Reels, Poles, Lines, additional applied Tech)
  - **Bait Packages** (Type, Rarity)
  - **Baits** (Type)
  - **Chummers** (Rarity)
  - **Supercharges** (Type, Rarity)
  - **Rod Tech** (Blank Tech, Gear System Tech)
- Compatible containers:
  - Chests on crew islands
  - Personal Vaults
  - Presets Menu
- Fixes Cape Offset when FishOnExtras Armor rendering is off _(requires FishOnMC Extras, toggleable in config)_
- Hides Sodium Incompatible Pack Warning _(requires Sodium, toggleable in config)_
- Colored Pet Amulet Stars - draws a star matched to the amulet equipped on a pet instead of the default yellow one _(requires FishOnMC Extras, toggleable in config)_
- Leaves Fix - removes broken leaves when using Sodium _(requires Sodium, toggleable in config)_
- Short Rank Icons - smaller icons for ranks _(toggleable in config, off by default)_
- Tooltip Fix - extends the fancy item tooltip frame to match the text width _(toggleable in config)_

All toggles live in the mod config (Mod Menu), and the scan start/stop key bind is under **Options > Controls > Key Binds > FishOnTracker**.
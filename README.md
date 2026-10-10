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
- Prevents duplicate scans of the same container
- Blocks scans from unintended containers such as Auction House, Menu etc.
- Supported Items:
  - **Armor** (Type, Rating, Rarity)
  - **Pets** (Type, Rating, Rarity, Equipped Items, Equipped Skins)
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
- Fixes Cape Offset when FishOnExtras Armor rendering is off _(can be toggled in settings)_
- Hides Sodium Incompatible Packs Waring _(can be toggled in settings)_
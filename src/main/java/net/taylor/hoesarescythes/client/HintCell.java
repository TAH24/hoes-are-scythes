package net.taylor.hoesarescythes.client;

import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.network.chat.Component;

/** A Cloth Config list line that shows grey example text while empty, so newly added lines are easy to spot. */
final class HintCell extends StringListListEntry.StringListCell {

    HintCell(String value, StringListListEntry listEntry, Component hint) {
        super(value, listEntry);
        widget.setHint(hint);
    }
}

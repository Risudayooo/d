package com.risudayooo.discgun.disc;

import com.risudayooo.discgun.disc.ability.DnbAbility;
import com.risudayooo.discgun.disc.ability.DubstepAbility;
import com.risudayooo.discgun.disc.ability.HouseAbility;
import net.minecraft.util.Formatting;

/**
 * The Phase 1 placeholder disc roster: House / Dubstep / DnB (4章).
 *
 * <p>Each constant bundles an id (used in the data component + lang keys), a
 * display colour, and the {@link DiscAbility} that defines its combat feel.
 * Adding a disc = adding a constant + an ability class; nothing else changes.
 */
public enum DiscType {
	HOUSE("house", Formatting.AQUA, new HouseAbility()),
	DUBSTEP("dubstep", Formatting.LIGHT_PURPLE, new DubstepAbility()),
	DNB("dnb", Formatting.YELLOW, new DnbAbility());

	private final String id;
	private final Formatting color;
	private final DiscAbility ability;

	DiscType(String id, Formatting color, DiscAbility ability) {
		this.id = id;
		this.color = color;
		this.ability = ability;
	}

	public String id() {
		return id;
	}

	public Formatting color() {
		return color;
	}

	public DiscAbility ability() {
		return ability;
	}

	/** Translation key for the disc's display name. */
	public String translationKey() {
		return "disc.discgun." + id;
	}

	/** Resolve a disc by id, defaulting to {@link #HOUSE} (the neutral baseline). */
	public static DiscType byId(String id) {
		for (DiscType type : values()) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return HOUSE;
	}
}

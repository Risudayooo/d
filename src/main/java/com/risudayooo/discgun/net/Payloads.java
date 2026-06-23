package com.risudayooo.discgun.net;

import com.risudayooo.discgun.DiscGunMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client→server intent packets. All are empty signals — the server reads the
 * held gun + its disc and validates cooldowns authoritatively, so the client
 * never gets to dictate damage or position.
 */
public final class Payloads {
	private Payloads() {
	}

	public record Fire() implements CustomPayload {
		public static final Id<Fire> ID = new Id<>(DiscGunMod.id("fire"));
		public static final PacketCodec<RegistryByteBuf, Fire> CODEC = PacketCodec.unit(new Fire());

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record Action() implements CustomPayload {
		public static final Id<Action> ID = new Id<>(DiscGunMod.id("action"));
		public static final PacketCodec<RegistryByteBuf, Action> CODEC = PacketCodec.unit(new Action());

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record Blink() implements CustomPayload {
		public static final Id<Blink> ID = new Id<>(DiscGunMod.id("blink"));
		public static final PacketCodec<RegistryByteBuf, Blink> CODEC = PacketCodec.unit(new Blink());

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record Parry() implements CustomPayload {
		public static final Id<Parry> ID = new Id<>(DiscGunMod.id("parry"));
		public static final PacketCodec<RegistryByteBuf, Parry> CODEC = PacketCodec.unit(new Parry());

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record Reload() implements CustomPayload {
		public static final Id<Reload> ID = new Id<>(DiscGunMod.id("reload"));
		public static final PacketCodec<RegistryByteBuf, Reload> CODEC = PacketCodec.unit(new Reload());

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}

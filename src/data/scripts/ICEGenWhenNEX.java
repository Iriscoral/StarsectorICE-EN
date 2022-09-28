package data.scripts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exerelin.campaign.SectorManager;

public class ICEGenWhenNEX extends ICEGenNormal {

	public static void spawnICEFactionForNEX() {
		SectorManager.addLiveFactionId("sun_ice");
	}

	public static void removeICEFactionForNEX() {
		SectorManager.removeLiveFactionId("sun_ice");
	}

	public static void setNotTransfer() {
		/*
		if (!Nex_TransferMarket.NO_TRANSFER_FACTIONS.contains("sun_ice"))
			Nex_TransferMarket.NO_TRANSFER_FACTIONS.add("sun_ice");

		 */
	}

	public static boolean checkIfCorvus() {
		return SectorManager.getManager().isCorvusMode();
	}

	@Override
	public void generate(SectorAPI sector) {
		if (checkIfCorvus()) {
			super.generate(sector);
		}

		setNotTransfer();
	}
}
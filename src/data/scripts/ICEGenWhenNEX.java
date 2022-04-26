package data.scripts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exerelin.campaign.SectorManager;

public class ICEGenWhenNEX {

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

	public static void generateIfCorvus(SectorAPI sector) {
		if (checkIfCorvus()) {
			ICEGen.generateInNewGame(sector);
		}
	}
}
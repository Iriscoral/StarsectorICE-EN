package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.ArrayList;
import java.util.List;

public class ICEGenNormal implements SectorGeneratorPlugin {

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	@Override
	public void generate(SectorAPI sector) {
		generateInEOS(sector);
		relationAdj(sector);
	}

	private void generateInEOS(SectorAPI sector) {
		SharedData.getData().getPersonBountyEventData().addParticipatingFaction("sun_ici");

		StarSystemAPI system = sector.getStarSystem("Eos Exodus");
		if (system == null) {
			return;
		}

		system.addCustomEntity("sun_ice_entity_hack", strings.get("exiled_name"), "sun_ice_entity_hack", "neutral");

		spawnCitadel(system);
		spawnUlterius(system);
	}

	private void spawnCitadel(StarSystemAPI system) {
		CustomCampaignEntityAPI idoneusCitadel = system.addCustomEntity(SUN_ICE_Data.IdoneusCitadelId, "Idoneus Citadel", "sun_ice_idoneus_citadel", "sun_ici");
		idoneusCitadel.setCircularOrbit(system.getStar(), 76f, 16500f, 900f);
		idoneusCitadel.setCustomDescriptionId("sun_ice_idoneus_citadel");

		MarketAPI citadelMarket = Global.getFactory().createMarket("sun_ice_idoneus_citadel_market", "Idoneus Citadel", 6);
		citadelMarket.setFactionId("sun_ici");

		citadelMarket.addSubmarket("open_market");
		// citadelMarket.addSubmarket("generic_military");
		// citadelMarket.addSubmarket("black_market");
		citadelMarket.addSubmarket("storage");

		citadelMarket.addCondition("population_6");
		citadelMarket.addCondition("sun_ice_exotic_tech");
		citadelMarket.addCondition("stealth_minefields");

		citadelMarket.addIndustry("population");
		citadelMarket.addIndustry("spaceport");
		citadelMarket.addIndustry("lightindustry");
		citadelMarket.addIndustry("heavybatteries");
		citadelMarket.addIndustry("orbitalworks");
		citadelMarket.addIndustry("highcommand");
		citadelMarket.addIndustry("starfortress_high");
		citadelMarket.getIndustry("orbitalworks").setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));

		citadelMarket.getTariff().modifyFlat("sun_ice_idoneus_citadel_market", 0.35f);
		citadelMarket.setPrimaryEntity(idoneusCitadel);
		idoneusCitadel.setMarket(citadelMarket);

		Global.getSector().getEconomy().addMarket(citadelMarket, false);
	}

	private void spawnUlterius(StarSystemAPI system) {
		CustomCampaignEntityAPI idoneusUlterius = system.addCustomEntity(SUN_ICE_Data.IdoneusUlteriusId, "Ulterius", "sun_ice_idoneus_citadel", "sun_ici");
		idoneusUlterius.setCircularOrbit(system.getStar(), 80f, 15000f, 900f);
		idoneusUlterius.setCustomDescriptionId("sun_ice_idoneus_ulterius");

		MarketAPI ulteriusMarket = Global.getFactory().createMarket("sun_ice_idoneus_ulterius_market", "Ulterius", 5);
		ulteriusMarket.setFactionId("sun_ici");

		ulteriusMarket.addSubmarket("open_market");
		ulteriusMarket.addSubmarket("generic_military");
		// ulteriusMarket.addSubmarket("black_market");
		ulteriusMarket.addSubmarket("storage");

		ulteriusMarket.addCondition("population_5");
		ulteriusMarket.addCondition("sun_ice_exotic_tech");
		ulteriusMarket.addCondition("stealth_minefields");

		ulteriusMarket.addIndustry("population");
		ulteriusMarket.addIndustry("spaceport");
		ulteriusMarket.addIndustry("heavybatteries");
		ulteriusMarket.addIndustry("fuelprod");
		ulteriusMarket.addIndustry("militarybase");
		ulteriusMarket.addIndustry("starfortress_high");
		ulteriusMarket.getIndustry("fuelprod").setSpecialItem(new SpecialItemData(Items.SYNCHROTRON, null));

		ulteriusMarket.getTariff().modifyFlat("sun_ice_idoneus_ulterius_market", 0.35f);
		ulteriusMarket.setPrimaryEntity(idoneusUlterius);
		idoneusUlterius.setMarket(ulteriusMarket);

		Global.getSector().getEconomy().addMarket(ulteriusMarket, false);
	}

	private void relationAdj(SectorAPI sector) {
		FactionAPI ice = SUN_ICE_Data.getICE();
		FactionAPI ici = SUN_ICE_Data.getICI();

		List<FactionAPI> factions = new ArrayList<>(sector.getAllFactions());
		factions.remove(ici);

		for (FactionAPI faction : factions) {
			switch (faction.getRelationshipLevel(ici)) {
				case COOPERATIVE:
				case FRIENDLY:
				case WELCOMING:
				case FAVORABLE:
					ici.setRelationship(faction.getId(), RepLevel.NEUTRAL);
					break;
				case NEUTRAL:
					ici.setRelationship(faction.getId(), RepLevel.INHOSPITABLE);
					break;
				case SUSPICIOUS:
				case INHOSPITABLE:
					ici.setRelationship(faction.getId(), RepLevel.HOSTILE);
					break;
				default:
					break;
			}
		}

		ici.setRelationship("player", RepLevel.INHOSPITABLE);
		ici.setRelationship("independent", RepLevel.INHOSPITABLE);
		ici.setRelationship("pirates", RepLevel.HOSTILE);
		ici.setRelationship("luddic_church", RepLevel.HOSTILE);
		ici.setRelationship("luddic_path", RepLevel.VENGEFUL);
		ici.setRelationship("derelict", RepLevel.HOSTILE);
		ici.setRelationship("remnant", RepLevel.HOSTILE);
		ici.setRelationship("sun_ice", RepLevel.INHOSPITABLE);
		ici.setRelationship("cabal", RepLevel.VENGEFUL);

		factions.remove(ice);
		for (FactionAPI faction : factions) {
			switch (faction.getRelationshipLevel(ice)) {
				case NEUTRAL:
					ice.setRelationship(faction.getId(), RepLevel.SUSPICIOUS);
					break;
				case SUSPICIOUS:
					ice.setRelationship(faction.getId(), RepLevel.INHOSPITABLE);
					break;
				default:
					break;
			}
		}

		ice.setRelationship("player", RepLevel.SUSPICIOUS);
		ice.setRelationship("independent", RepLevel.SUSPICIOUS);
		ice.setRelationship("pirates", RepLevel.HOSTILE);
		ice.setRelationship("luddic_church", RepLevel.INHOSPITABLE);
		ice.setRelationship("luddic_path", RepLevel.HOSTILE);
		ice.setRelationship("derelict", RepLevel.HOSTILE);
		ice.setRelationship("remnant", RepLevel.HOSTILE);
		ice.setRelationship("sun_ici", RepLevel.INHOSPITABLE);
		ice.setRelationship("cabal", RepLevel.HOSTILE);
	}
}
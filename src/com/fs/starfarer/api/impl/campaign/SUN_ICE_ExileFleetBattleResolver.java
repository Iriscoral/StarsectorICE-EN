package com.fs.starfarer.api.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.tools.SUN_ICE_Data;

public class SUN_ICE_ExileFleetBattleResolver extends BattleAutoresolverPluginImpl {

	public SUN_ICE_ExileFleetBattleResolver(BattleAPI battle) {
		super(battle);
	}

	@Override
	public void resolve() {
		Global.getLogger(this.getClass()).info("ICE resolve called.");
		super.resolve();
	}

	@Override
	protected FleetAutoresolveData computeDataForFleet(CampaignFleetAPI fleet) {

		FleetAutoresolveData fleetData = super.computeDataForFleet(fleet);
		if (SUN_ICE_Data.getExileManager() == null) return fleetData;

		CampaignFleetAPI exiled = SUN_ICE_Data.getExileManager().getExiledFleet();
		if (exiled == null) return fleetData;

		CampaignFleetAPI combined = battle.getCombinedFor(exiled);
		if (combined != null && combined == fleet) {
			Global.getLogger(this.getClass()).info("ICE resolve mult.");
			fleetData.fightingStrength *= 2f;
		}

		return fleetData;
	}
}
package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;

public class SUN_ICE_CampaignListener extends BaseCampaignEventListener {

	public SUN_ICE_CampaignListener(boolean permaRegister) {
		super(permaRegister);
	}

	@Override
	public void reportPlayerReputationChange(String faction, float delta) {
		if (faction.contentEquals("sun_ice")) {
			FactionAPI player = Global.getSector().getPlayerFaction();
			if (player.isAtBest(faction, RepLevel.SUSPICIOUS)) {
				SUN_ICE_MissionManager.doomStages();
			}
		}
	}
}
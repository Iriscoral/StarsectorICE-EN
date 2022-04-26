package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;

public class SUN_ICE_ExileFleetInflater2 extends DefaultFleetInflater {

	public SUN_ICE_ExileFleetInflater2() {
		super(new DefaultFleetInflaterParams());

		setPersistent(true);
		updateQuality();
	}

	private void updateQuality() {
		p.quality = 0.5f + SUN_ICE_MissionManager.getStage().getCompleted() * 0.25f;
		p.averageSMods = (int)(0f + SUN_ICE_MissionManager.getSpecialTagCompletedCount() * 0.4f);
	}

	@Override
	public void inflate(CampaignFleetAPI fleet) {
		Global.getLogger(this.getClass()).info("ICE inflater triggered for " + fleet.getName());

		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.getShipName() == null) {
				Global.getLogger(this.getClass()).warn("WARNING, no name for " + member.getHullSpec().getHullName());
				member.setShipName(" ");
			}

			if (!member.getHullSpec().getManufacturer().contentEquals("ICS") && !member.isCivilian()) {
				Global.getLogger(this.getClass()).info("skipped " + member + ", because of manu " + member.getHullSpec().getManufacturer());
				member.getVariant().addTag(Items.TAG_NO_AUTOFIT);
			}
		}

		updateQuality();
		super.inflate(fleet);
	}

	@Override
	public Boolean getPersistent() {
		return true; // persistent
	}

	@Override
	public void setPersistent(Boolean persistent) {
		p.persistent = true; // persistent
	}

	@Override
	public boolean removeAfterInflating() {
		return false; // persistent
	}

	@Override
	public void setRemoveAfterInflating(boolean removeAfterInflating) {
		p.persistent = true; // persistent
	}
}
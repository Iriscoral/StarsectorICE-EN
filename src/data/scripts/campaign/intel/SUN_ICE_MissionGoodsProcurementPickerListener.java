package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.awt.Color;

public class SUN_ICE_MissionGoodsProcurementPickerListener implements CargoPickerListener {

	private final SUN_ICE_MissionGoodsProcurementIntel intel;
	private final InteractionDialogAPI dialog;
	private final InteractionDialogPlugin plugin;

	public SUN_ICE_MissionGoodsProcurementPickerListener(SUN_ICE_MissionGoodsProcurementIntel intel, InteractionDialogAPI dialog, InteractionDialogPlugin plugin) {
		this.intel = intel;
		this.dialog = dialog;
		this.plugin = plugin;
	}

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission4_ongoing_");

	@Override
	public void pickedCargo(CargoAPI cargo) {

		if (cargo.isEmpty()) {
			cancelledCargoSelection();
			return;
		}

		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			if (stack.getCommodityId() == null) continue;

			int shortage = intel.getShortage(stack.getCommodityId());
			int toDeliver = Math.min(shortage, (int)stack.getSize());

			intel.deliver(stack.getCommodityId(), toDeliver);
			Global.getSector().getPlayerFleet().getCargo().removeItems(CargoAPI.CargoItemType.RESOURCES, stack.getCommodityId(), toDeliver);
		}

		if (intel.isAllDelivered()) {
			dialog.getOptionPanel().addOption(strings.get("deliver"), "deliver");
			dialog.getOptionPanel().addOption(strings.get("deliver_extra"), "deliver_extra", Misc.getPositiveHighlightColor(), null);
			dialog.getOptionPanel().addOption(strings.get("wait"), "wait");
		} else {
			cancelledCargoSelection();
		}
	}

	@Override
	public void cancelledCargoSelection() {
		dialog.getOptionPanel().addOption(strings.get("check"), "check");
		dialog.getOptionPanel().addOption(strings.get("wait"), "wait");
	}

	@Override
	public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
		float pad = 3f;
		float opad = 10f;
		float lpad = 20f;
		Color highlight = Misc.getHighlightColor();
		Color negative = Misc.getNegativeHighlightColor();
		Color positive = Misc.getPositiveHighlightColor();

		for (String id : intel.getRequirements()) {
			CommoditySpecAPI commodity = Global.getSettings().getCommoditySpec(id);

 			TooltipMakerAPI imageTooltip = panel.beginImageWithText(commodity.getIconName(), 64f);
			imageTooltip.setParaOrbitronLarge();
			imageTooltip.addPara(commodity.getName(), opad);
			imageTooltip.setParaFontDefault();

			int required = intel.getRequired(commodity.getId());
			int provided = intel.getProvided(commodity.getId());
			int toProvide = (int)combined.getCommodityQuantity(id);
			int toFill = Math.min(provided + toProvide, required);
			Color[] hl = new Color[2];
			if (provided >= required) {
				hl[0] = highlight;
				hl[1] = highlight;
			} else if (toProvide > 0) {
				hl[0] = positive;
				hl[1] = highlight;
			} else {
				hl[0] = negative;
				hl[1] = highlight;
			}

			imageTooltip.addPara("%s / %s", opad, hl, String.valueOf(toFill), String.valueOf(required));
			panel.addImageWithText(0f);
		}
	}
}
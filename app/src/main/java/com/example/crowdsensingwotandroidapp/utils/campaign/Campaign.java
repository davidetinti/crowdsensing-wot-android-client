package com.example.crowdsensingwotandroidapp.utils.campaign;

import com.example.crowdsensingwotandroidapp.utils.campaign.area.CampaignArea;
import com.example.crowdsensingwotandroidapp.utils.campaign.area.CampaignCircleArea;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

public class Campaign implements Serializable {

	private final String id;
	private final String title;
	private final String description;
	private final String organization;
	private final Number idealSubmissionInterval;
	private final Number points;
	private final Boolean pullEnabled;
	private final Boolean pushAutoEnabled;
	private final Boolean pushInputEnabled;
	private final Number submissionRequired;
	private final String type;
	private final CampaignArea area;

	public Campaign(HashMap campaign) {
		id = (String) campaign.get("_id");
		title = (String) campaign.get("title");
		description = (String) campaign.get("description");
		organization = (String) campaign.get("organization");
		idealSubmissionInterval = (Number) campaign.get("ideal_submission_interval");
		points = (Number) campaign.get("points");
		pullEnabled = (Boolean) campaign.get("pull_enabled");
		pushAutoEnabled = (Boolean) campaign.get("push_auto_enabled");
		pushInputEnabled = (Boolean) campaign.get("push_input_enabled");
		submissionRequired = (Number) campaign.get("submission_required");
		type = (String) campaign.get("type");
		HashMap areaMap = (HashMap) campaign.get("area");
		if (areaMap != null) {
			switch ((String) Objects.requireNonNull(areaMap.get("type"))) {
				case "circle":
					area = new CampaignCircleArea(areaMap);
					break;
				default:
					area = null;
			}
		} else {
			area = null;
		}
	}

	public Campaign(Campaign campaign){
		this.id = campaign.id;
		this.title = campaign.title;
		this.description = campaign.description;
		this.organization = campaign.organization;
		this.idealSubmissionInterval = campaign.idealSubmissionInterval;
		this.points = campaign.points;
		this.pullEnabled = campaign.pullEnabled;
		this.pushAutoEnabled = campaign.pushAutoEnabled;
		this.pushInputEnabled = campaign.pushInputEnabled;
		this.submissionRequired = campaign.submissionRequired;
		this.type = campaign.type;
		this.area = campaign.area;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getOrganization() {
		return organization;
	}

	public Number getIdealSubmissionInterval() {
		return idealSubmissionInterval;
	}

	public Number getPoints() {
		return points;
	}

	public Boolean getPullEnabled() {
		return pullEnabled;
	}

	public Boolean getPushAutoEnabled() {
		return pushAutoEnabled;
	}

	public Boolean getPushInputEnabled() {
		return pushInputEnabled;
	}

	public Number getSubmissionRequired() {
		return submissionRequired;
	}

	public String getType() {
		return type;
	}

	public CampaignArea getArea() {
		return area;
	}
}

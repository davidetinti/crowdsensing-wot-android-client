package com.example.crowdsensingwotandroidapp.utils.campaign;

public class AppliedCampaign extends Campaign {

	private final SubmissionMode mode;
	private final Integer interval;

	public AppliedCampaign(Campaign campaign, SubmissionMode mode, Integer interval) {
		super(campaign);
		this.mode = mode;
		this.interval = interval;
	}

	public SubmissionMode getMode() {
		return mode;
	}

	public Integer getInterval() {
		if (interval == null)
			return getIdealSubmissionInterval().intValue();
		else
			return interval;
	}
}

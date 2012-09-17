package com.mamewo.podplayer0.tests;

import com.jayway.android.robotium.solo.Solo;

public class PodplayerExpActivityLandTest
	extends PodplayerExpActivityTest
{
	@Override
	public void setUp()
			throws Exception
	{
		super.setUp();
		solo_.setActivityOrientation(Solo.LANDSCAPE);
	}
}

package com.mamewo.podplayer0.tests;

//import com.jayway.android.robotium.solo.Solo;
import com.robotium.solo.Solo;

public class TestPodplayerExpActivityLand
	extends TestPodplayerExpActivity
{
	@Override
	public void setUp()
			throws Exception
	{
		super.setUp();
		solo_.setActivityOrientation(Solo.LANDSCAPE);
	}
}

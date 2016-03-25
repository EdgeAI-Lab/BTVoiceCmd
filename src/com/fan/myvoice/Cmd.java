package com.fan.myvoice;

public enum Cmd {
	
	前进,后退,向左转,向右转,左转弯,右转弯,向前滑行,向后滑行,向左转头,向右转头,摇头,抬起左臂,放下左臂,抬起右臂,放下右臂,向左摆动左手腕,左手腕向左摆,
	向右摆动左手腕,左手腕向右摆,摆动左手腕,向左摆动右手腕,右手腕向左摆,向右摆动右手腕,右手腕向右摆,摆动右手腕,摆动腰部,停止,加速,减速,noCmd;

	public static Cmd getCmd(String cmd){
		
		Cmd mCmd = noCmd;
		try {
			mCmd = valueOf(cmd);
		} catch (IllegalArgumentException  e) {
			return mCmd;
		}catch (NullPointerException  e) {
			return mCmd;
		}

		return mCmd;
	}
}

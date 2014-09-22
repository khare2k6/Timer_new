package com.ak.remotecontroller;

import com.ak.timer.TimerStateMachine.State;
import com.ak.timer.ICountDownListener;

public interface ITimerActionsListener extends ICountDownListener{

	public void onStateChange(State newState);
}

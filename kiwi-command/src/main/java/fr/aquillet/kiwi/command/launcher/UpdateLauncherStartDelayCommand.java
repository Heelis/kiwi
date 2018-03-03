package fr.aquillet.kiwi.command.launcher;

import lombok.Value;

import java.util.UUID;

@Value
public class UpdateLauncherStartDelayCommand {

	private UUID id;
	private int startDelay;
}

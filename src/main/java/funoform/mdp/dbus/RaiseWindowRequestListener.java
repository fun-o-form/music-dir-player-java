package funoform.mdp.dbus;

/**
 * Gets notifications when DBUS requests the player's window (if any) be
 * displayed in the foreground.
 * 
 * See:
 * https://specifications.freedesktop.org/mpris-spec/latest/Media_Player.html#Method:Raise
 */
public interface RaiseWindowRequestListener {
	public void raiseWindowRequested();
}

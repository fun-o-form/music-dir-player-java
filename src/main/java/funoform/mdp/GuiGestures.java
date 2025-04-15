package funoform.mdp;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * Recognizes mouse gestures, such as swipes, and issues the corresponding
 * commands.
 * 
 * Currently supported gestures: 1) swipe right = next track 2) swipe left =
 * previous track, 3) triple click = stop the track.
 * 
 * The way this class works is tricky. You give it the top level window
 * component. It then turns on the glass pane on that window and adds itself as
 * a mouse listener for the glass pane. It is important to understand that the
 * glass pane now gets all mouse actions. The buttons and controls under the
 * glass pane no longer get any mouse actions. If left that way, this would
 * break the entire app as your buttons wouldn't do anything any longer.
 * 
 * Every single mouse action therefore goes to this GuiGestures class. If it
 * detects a gesture, it handles the gesture. If the mouse action was not a
 * gesture, it forwards the mouse event to the component at that location under
 * the glass pane. This class is therefore a conduit for all mouse actions and
 * is responsible for sending any mouse actions it chooses not to handle to the
 * components under the glass pane.
 */
public class GuiGestures implements MouseListener, MouseWheelListener {

	private static final Logger sLogger = Logger.getLogger(GuiGestures.class.getName());
	private Controller mCtrl;
	private RootPaneContainer mWin;
	private Point mPressPoint = null;

	/**
	 * Creating this object will cause it to immediately start intercepting mouse
	 * events on the provided window.
	 * 
	 * @param ctrl   The controller to send commands to, such as previous track and
	 *               next track.
	 * @param window The window, likely a JFrame or JWindow, to which this class
	 *               will monitor for mouse gestures over. This needs to be your top
	 *               level window in order to allow gestures anywhere over your app.
	 *               Keep in mind, the provided window will be modified. It will
	 *               have its glass pane enabled and the GuiGestures class will
	 *               register to receive all mouse events so that none are sent
	 *               directly to the components on the window any longer.
	 */
	public GuiGestures(Controller ctrl, RootPaneContainer window) {
		mCtrl = ctrl;
		mWin = window;
		mWin.getGlassPane().setVisible(true);
//		mWin.getGlassPane().addMouseListener(this);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent evt) {
		// No gestures can occur on this mouse event. Forward everything just so the
		// glass pane doesn't prevent controls from getting this event on their own
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseEntered(MouseEvent evt) {
		// No gestures can occur on this mouse event. Forward everything just so the
		// glass pane doesn't prevent controls from getting this event on their own
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseExited(MouseEvent evt) {
		// No gestures can occur on this mouse event. Forward everything just so the
		// glass pane doesn't prevent controls from getting this event on their own
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		// mouse clicks are those where the mouse is both pressed and released

		// If you triple click (or more) the mouse in rapid succession, the music will
		// be stopped. Note that you must stop clicking the mouse briefly to reset the
		// counter
		if (evt.getClickCount() > 2) {
			// TODO: replace with pause/resume once supported
			mCtrl.stop();
		} else {
			// plain old single or double click. Send to underlying component
			redispatchMouseEvent(evt);
		}
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		// mouse presses are those where the button is pressed down but not necessarily
		// released
		redispatchMouseEvent(evt);
		if (MouseEvent.BUTTON1 == evt.getButton()) {
			mPressPoint = evt.getPoint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent evt) {
		// mouse presses are those where the button is pressed down but not necessarily
		// released
		if (MouseEvent.BUTTON1 == evt.getButton()) {
			Point releasePoint = evt.getPoint();
			if (handlePotentialDrag(mPressPoint, releasePoint)) {
				// We detected and handled a gesture. Don't forward the mouse event to the
				// component under the glass
				return;
			}
		}
		redispatchMouseEvent(evt);
	}

	/**
	 * Determines if a gesture took place and if so, handles it.
	 * 
	 * @param press
	 * @param release
	 * 
	 * @return True if a gesture was detected. False if no gesture was detected and
	 *         thus the mouse event should be forwarded to whatever control is under
	 *         the glass pane at that location.
	 */
	private boolean handlePotentialDrag(Point press, Point release) {
		// Always get the windows current size just in case it recently increased (e.g.
		// maximize) or decreased
		Dimension winSize = mWin.getGlassPane().getSize();

		// determine how far they dragged the mouse
		int deltaHorizontal = Math.abs(press.x - release.x);
		int deltaVertical = Math.abs(press.y - release.y);

		// We want to see gestures that cover 50% of the window area. Anything smaller
		// and we won't consider it for a gesture
		int gestureMinWidth = winSize.width / 2;
		int gestureMinHeight = winSize.height / 2;

		// Did the user swipe far enough to consider this a gesture?
		boolean hGesture = false;
		boolean vGesture = false;
		if (gestureMinWidth < deltaHorizontal) {
			hGesture = true;
		}
		if (gestureMinHeight < deltaVertical) {
			vGesture = true;
		}
		if (!(hGesture || vGesture)) {
			// Not big enough movement in either direction to be considered a gesture
			return false;
		}
		if (hGesture && vGesture) {
			// Great, they made big movements in both directions, something like a 45 degree
			// swipe. Don't bother going any further. It is too ambiguous
			return false;
		}

		// No one gestures in a straight line. There will always be some curvature but
		// to be a gesture it should be roughly straight. A roughly straight line is one
		// in which the start and end point don't vary more than 10% in opposite
		// direction, i.e. a swipe on the width must not have a ending height more than
		// 10% off the starting height. We only compare the start and end points. If
		// some jerk swipes in a V, well, that's considered a horizontal (width) swipe
		// since we only care about the end points and not the middle
		if (hGesture) {
			int verticalJitterLimit = winSize.height / 10;
			if (deltaVertical < verticalJitterLimit) {
				// Clean swipe left or right. Figure out which way they went and respond to it
				if (release.x > press.x) {
					// swipe right
					sLogger.log(Level.INFO, "User swipped right. Sending next track cmd");
					mCtrl.nextTrack();
					return true;
				} else {
					// swipe left
					sLogger.log(Level.INFO, "User swipped left. Sending previous track cmd");
					mCtrl.priorTrack();
					return true;
				}
			}
		} else if (vGesture) {
			// We currently don't do anything with vertical gestures. Just ignore them for
			// now
		}
		return false;
	}

	/**
	 * Call this method once we decide the mouse event was not a gesture and instead
	 * should be forwarded to whatever component is under the glass where this event
	 * took place.
	 * 
	 * This is what causes components like JButtons under the glass to get clicked.
	 * If you never call this, your application will be completely unresponsive to
	 * the mouse, aside from supported gestures.
	 * 
	 * @param evt The event to forward to the underlying component.
	 * 
	 */
	@SuppressWarnings("deprecation") // MouseEvent.getModifiers() is deprecated in Java 9 but the replacement
										// getModifiersEx() does not work the same way and in fact breaks this method if
										// used.
	private void redispatchMouseEvent(MouseEvent evt) {
		Point glassPanePoint = evt.getPoint();
		Container c = mWin.getContentPane();
		Component compAtLocation = SwingUtilities.getDeepestComponentAt(c, glassPanePoint.x, glassPanePoint.y);
		Point containerPoint = SwingUtilities.convertPoint(mWin.getGlassPane(), glassPanePoint, compAtLocation);

		if (null != compAtLocation) {
			compAtLocation.dispatchEvent(new MouseEvent(compAtLocation, evt.getID(), evt.getWhen(), evt.getModifiers(),
					containerPoint.x, containerPoint.y, evt.getClickCount(), evt.isPopupTrigger()));
		}
	}
}

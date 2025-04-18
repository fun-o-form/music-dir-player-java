package funoform.mdp.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollBar;
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
public class GestureDetector implements MouseListener, MouseWheelListener, MouseMotionListener {

	private static final Logger sLogger = Logger.getLogger(GestureDetector.class.getName());
	private IGestureListener mGestListener;
	private RootPaneContainer mWin;
	private Point mPressPoint = null;

	/**
	 * Creating this object will cause it to immediately start intercepting mouse
	 * events on the provided window.
	 * 
	 * @param window The window, likely a JFrame or JWindow, to which this class
	 *               will monitor for mouse gestures over. This needs to be your top
	 *               level window in order to allow gestures anywhere over your app.
	 *               Keep in mind, the provided window will be modified. It will
	 *               have its glass pane enabled and the GuiGestures class will
	 *               register to receive all mouse events so that none are sent
	 *               directly to the components on the window any longer.
	 * @param l      The thing that gets notified when a gesture has occurred.
	 */
	public GestureDetector(RootPaneContainer window, IGestureListener l) {
		mGestListener = l;
		mWin = window;
		mWin.getGlassPane().setVisible(true);

		// All we really want is the mouse listener so we can look for click and drag
		// gestures. Unfortunately, once you register a mouseListener, your window will
		// stop responding to mouse wheel and mouse motion events. So you MUST register
		// as a listener for all 3 and forward the events you don't want. Super
		// annoying.
		mWin.getGlassPane().addMouseListener(this);
		mWin.getGlassPane().addMouseWheelListener(this);
		mWin.getGlassPane().addMouseMotionListener(this);
	}

	/**
	 * Mouse clicks are those where the mouse is both pressed and released
	 */
	@Override
	public void mouseClicked(MouseEvent evt) {
		// If you triple click (or more) the mouse in rapid succession, take an action.
		// Note that you must stop clicking the mouse briefly to reset the counter
		if (evt.getClickCount() > 2) {
			mGestListener.gestureDetected(IGestureListener.Gesture.TRIPLE_TAP);
		} else {
			// plain old single or double click. Send to underlying component
			redispatchMouseEvent(evt);
		}
	}

	/**
	 * Mouse presses are those where the button is pressed down but not necessarily
	 * released
	 */
	@Override
	public void mousePressed(MouseEvent evt) {
		redispatchMouseEvent(evt);

		if (MouseEvent.BUTTON1 == evt.getButton()) {
			// perhaps the beginning of a gesture
			mPressPoint = evt.getPoint();
		}
	}

	/**
	 * Mouse presses are those where the button is pressed down but not necessarily
	 * released
	 */
	@Override
	public void mouseReleased(MouseEvent evt) {
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
					sLogger.log(Level.INFO, "User swipped right");
					mGestListener.gestureDetected(IGestureListener.Gesture.SWIPE_RIGHT);
					return true;
				} else {
					// swipe left
					sLogger.log(Level.INFO, "User swipped left");
					mGestListener.gestureDetected(IGestureListener.Gesture.SWIPE_LEFT);
					return true;
				}
			}
		} else if (vGesture) {
			// 20% because we assume portrait mode, so be more generous in the skinnier
			// dimension
			int horizontalJitterLimit = winSize.width / 20;
			if (deltaHorizontal < horizontalJitterLimit) {
				// Clean swipe up or down. Figure out which way they went and respond to it
				if (release.y > press.y) {
					// swipe down
					sLogger.log(Level.INFO, "User swipped down");
					mGestListener.gestureDetected(IGestureListener.Gesture.SWIPE_DOWN);
					return true;
				} else {
					// swipe up
					sLogger.log(Level.INFO, "User swipped up");
					mGestListener.gestureDetected(IGestureListener.Gesture.SWIPE_UP);
					return true;
				}
			}
		}
		return false;
	}

	public interface IGestureListener {
		public enum Gesture {
			SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, TRIPLE_TAP
		}

		public void gestureDetected(Gesture g);
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

	@SuppressWarnings("deprecation")
	private void redispatchMouseEvent(MouseWheelEvent evt) {
		Point glassPanePoint = evt.getPoint();
		Container c = mWin.getContentPane();
		Component compAtLocation = SwingUtilities.getDeepestComponentAt(c, glassPanePoint.x, glassPanePoint.y);
		Point containerPoint = SwingUtilities.convertPoint(mWin.getGlassPane(), glassPanePoint, compAtLocation);

		if (null != compAtLocation) {
			compAtLocation.dispatchEvent(new MouseWheelEvent(compAtLocation, evt.getID(), evt.getWhen(),
					evt.getModifiers(), containerPoint.x, containerPoint.y, evt.getClickCount(), evt.isPopupTrigger(),
					evt.getScrollType(), evt.getScrollAmount(), evt.getWheelRotation()));
		}
	}

	private boolean isMouseOnScrollBar(MouseEvent evt) {
		Point glassPanePoint = evt.getPoint();
		Container c = mWin.getContentPane();
		Component compAtLocation = SwingUtilities.getDeepestComponentAt(c, glassPanePoint.x, glassPanePoint.y);
		if (compAtLocation instanceof JScrollBar) {
			return true;
		}
		return false;
	}

	@Override
	public void mouseDragged(MouseEvent evt) {
//		if (isMouseOnScrollBar(evt)) {
			redispatchMouseEvent(evt);
//		}
	}

	////////////////////////////////////////////////////////////////////////////
	// None of the methods below lead to mouse gestures. Yet we have to listen for
	// these events so we can forward them to the component under the glass pane.
	// Not listening for these events would cause them to vanish into the void and
	// you wouldn't be able to scroll your mouse wheel on a scroll pane for example.
	////////////////////////////////////////////////////////////////////////////

	@Override
	public void mouseWheelMoved(MouseWheelEvent evt) {
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseEntered(MouseEvent evt) {
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseExited(MouseEvent evt) {
		redispatchMouseEvent(evt);
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		redispatchMouseEvent(evt);
	}
}

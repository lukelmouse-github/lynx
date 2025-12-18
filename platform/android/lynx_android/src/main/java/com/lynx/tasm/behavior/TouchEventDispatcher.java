// Copyright 2019 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.lynx.tasm.behavior;

import static com.lynx.tasm.event.LynxTouchEvent.EVENT_CLICK;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_LONG_PRESS;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_TAP;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_TOUCH_CANCEL;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_TOUCH_END;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_TOUCH_MOVE;
import static com.lynx.tasm.event.LynxTouchEvent.EVENT_TOUCH_START;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import com.lynx.devtoolwrapper.CDPResultCallback;
import com.lynx.devtoolwrapper.LogBoxLogLevel;
import com.lynx.devtoolwrapper.LynxBaseInspectorOwner;
import com.lynx.react.bridge.JavaOnlyArray;
import com.lynx.react.bridge.JavaOnlyMap;
import com.lynx.react.bridge.ReadableArray;
import com.lynx.tasm.EventEmitter;
import com.lynx.tasm.LynxEnv;
import com.lynx.tasm.LynxView;
import com.lynx.tasm.base.LLog;
import com.lynx.tasm.behavior.event.EventTarget;
import com.lynx.tasm.behavior.event.EventTargetBase;
import com.lynx.tasm.behavior.ui.LynxBaseUI;
import com.lynx.tasm.behavior.ui.UIBody;
import com.lynx.tasm.behavior.ui.UIGroup;
import com.lynx.tasm.behavior.ui.utils.LynxUIHelper;
import com.lynx.tasm.event.LynxEventDetail;
import com.lynx.tasm.event.LynxEventDetail.EVENT_TYPE;
import com.lynx.tasm.event.LynxTouchEvent;
import com.lynx.tasm.event.LynxTouchEvent.Point;
import com.lynx.tasm.gesture.arena.GestureArenaManager;
import com.lynx.tasm.gesture.handler.GestureConstants;
import com.lynx.tasm.utils.PixelUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class TouchEventDispatcher {
  private static class EventTargetDetail {
    private final EventTarget mActiveUI;
    private final PointF mDownPoint;
    private PointF mPrePoint;

    public EventTargetDetail(EventTarget activeUI, float x, float y) {
      mActiveUI = activeUI;
      mDownPoint = new PointF(x, y);
      mPrePoint = new PointF(x, y);
    }

    public EventTarget getUI() {
      return mActiveUI;
    }

    public PointF getDownPoint() {
      return mDownPoint;
    }

    public PointF getPrePoint() {
      return mPrePoint;
    }

    public void setPrePoint(PointF prePoint) {
      mPrePoint = prePoint;
    }
  }

  private LynxUIOwner mUIOwner;
  private GestureRecognizer mDetector;
  private EventTarget mActiveUI;
  // pointId -> Target, a pointId corresponds to a finger.
  private HashMap<Integer, EventTargetDetail> mActiveUIMap;
  private HashMap<Integer, EventTargetBase> mActiveTargetMap;
  private LinkedList<EventTarget> mActiveUIList;
  private EventTarget mFocusedUI;
  private LinkedList<EventTarget> mActiveClickList;
  // Manages the gesture arenas for handling touch events
  private GestureArenaManager mGestureArenaManager;

  private EventTarget.EnableStatus mConsumeSlideEvent = EventTarget.EnableStatus.Undefined;
  private boolean mCanConsumeSlideEvent = false;
  private PointF mDownPoint;
  private float mTapSlop;
  public static final String mTapSlopDefault = "50px";
  public static final float mTapSlopFloatDefault = 50;

  private boolean mTouchMoved;
  private boolean mTouchMoving;
  private float mMoveSlop;
  private boolean mShouldCheckMove;
  private boolean mGestureRecognized;
  private boolean mTouchOutSide;
  private PointF mPrePoint;
  private final HashSet<Integer> mGestureRecognizedUISet;
  private final HashSet<Integer> mPropsChangedUISet;
  private boolean mHasTouchPseudo;
  private boolean mEnablePlatformGesture;
  private boolean mHasMultiTouch;
  private boolean mEnableMultiTouch;
  private long mTimestamp = 0;

  private Point mTargetPoint;
  // record first index of multi touch event
  private LynxTouchEvent mFirstLynxTouchEvent;
  private EventTarget mPreTarget;
  private String mPreTargetInlineCSSText;
  private Point mFirstFingerDownPoint;
  private boolean mIsPlatformGestureActive = false;

  private static final String TAG = "LynxTouchEventDispatcher";

  public TouchEventDispatcher(@NonNull LynxUIOwner owner) {
    mUIOwner = owner;
    Listener mListener = new Listener();
    mDetector = new GestureRecognizer(
        mUIOwner.getContext(), mListener, new Handler(Looper.getMainLooper()));
    mActiveUIList = new LinkedList<>();
    mActiveClickList = new LinkedList<>();
    mDownPoint = new PointF(Float.MIN_VALUE, Float.MIN_VALUE);
    mFirstFingerDownPoint = new LynxTouchEvent.Point();
    // In template_binary_reader.cc, if template's page config doesn't contain tapSlop, let
    // tapSlop's the default value be 50px. Otherwise, tapSlop's value is set by the FE developer.
    // Normally, TouchEventDispatcher's setTapSlop function will be called, and mTapSlop will be
    // PixelUtils.dipToPx(tapSlop's value), which is greater than 0 . However, when LynxOverlayView
    // creates TouchEventDispatcher, the TouchEventDispatcher's setTapSlop function is not called.
    // Thus, mTapSlop in LynxOverlayView's TouchEventDispatcher will be 0, which means that the tap
    // gesture is recognize only if there is no ACTION_MOVE between the ACTION_DOWN and ACTION_UP.
    // But on some android phones, there will be ACTION_MOVE between ACTION_DWON and ACTION_UP even
    // if there is no movement between the finger down and up. Which leads to the bug that on these
    // phones, the tap event cannot be triggered in the overlay. To solve this bug, let mTapSlop's
    // default value be PixelUtils.dipToPx(50). And another fix will be provided. With the future
    // fix, when LynxOverlayView creates TouchEventDispatcher, the TouchEventDispatcher's setTapSlop
    // and other set-methods will be called. And E2E test case will be added in future.
    // TODO(songshourui.null): Add E2E test case.
    mTapSlop = PixelUtils.dipToPx(mTapSlopFloatDefault);
    mMoveSlop = 0;
    mShouldCheckMove = true;
    mGestureRecognizedUISet = new HashSet<>();
    mPropsChangedUISet = new HashSet<>();
    mHasTouchPseudo = false;
    mHasMultiTouch = false;
    mEnableMultiTouch = false;
    mActiveUIMap = new HashMap<>();
    mActiveTargetMap = new HashMap<>();
    mGestureArenaManager = owner.getGestureArenaManager();
  }

  /**
   * Support Context replacement
   * @param context
   */
  public void attachContext(Context context) {
    if (mDetector != null) {
      mDetector.updateTouchSlop(context);
    }
  }

  // TODO(hexionghui): Delete this, use the same method with paramenter instead.
  public void onGestureRecognized() {
    mGestureRecognized = true;
    if (!mActiveUIList.isEmpty()) {
      // if gesture is recognized, tap will not be triggered, so remove the active state of all
      // active UI.
      deactivatePseudoState(LynxTouchEvent.kPseudoStateActive);
    }
  }

  public void setGestureArenaManager(GestureArenaManager manager) {
    mGestureArenaManager = manager;
  }

  public void onGestureRecognized(LynxBaseUI ui) {
    if (mGestureRecognizedUISet != null) {
      mGestureRecognizedUISet.add(ui.getSign());
    }
  }

  public void onGestureRecognized(int sign) {
    if (mGestureRecognizedUISet != null) {
      mGestureRecognizedUISet.add(sign);
    }
  }

  public void onPropsChanged(LynxBaseUI ui) {
    if (mPropsChangedUISet != null) {
      mPropsChangedUISet.add(ui.getSign());
    }
  }

  public boolean isTouchMoving() {
    return mTouchMoving;
  }

  public void setHasTouchPseudo(boolean value) {
    // When disable fiber arch, setHasTouchPseudo will be exec twice.
    // Normally, it will exec setHasTouchPseudo in onPageConfigDecoded first.
    // In case not following this order in the future and exec setHasTouchPseudo in updateEventInfo
    // first, let mHasTouchPseudo = mHasTouchPseudo || value;
    mHasTouchPseudo = mHasTouchPseudo || value;
  }

  public void setEnablePlatformGesture(boolean value) {
    mEnablePlatformGesture = value;
  }

  public void setEnableMultiTouch(boolean value) {
    mEnableMultiTouch = value;
  }

  private boolean requestNativeDisallowIntercept(boolean flag) {
    if (mUIOwner.getRootUI() == null || mUIOwner.getRootUI().getView().getParent() == null) {
      LLog.e(TAG, "requestNativeDisallowIntercept failed, root ui or root ui'parent is null.");
      return false;
    }
    mUIOwner.getRootUI().getView().getParent().requestDisallowInterceptTouchEvent(flag);
    return flag;
  }

  public boolean consumeSlideEvent(MotionEvent ev) {
    switch (ev.getAction()) {
      case MotionEvent.ACTION_DOWN: {
        // When the finger is pressed, set mConsumeSlideEvent to Undefined.
        mConsumeSlideEvent = EventTarget.EnableStatus.Undefined;
        mCanConsumeSlideEvent = false;
        if (mActiveUI != null) {
          EventTarget target = mActiveUI;
          while (target != null && target.parent() != target) {
            // Only when there is a node on the responder chain that has consume-slide-event set,
            // can the detection logic of consumeSlideEvent continue to be executed.
            if (target.hasConsumeSlideEventAngles()) {
              mCanConsumeSlideEvent = true;
            }
            target = target.parent();
          }
        }

        if (mCanConsumeSlideEvent) {
          // When executing consumeSlideEvent, if MotionEvent.ACTION_DOWN is encountered,
          // immediately execute requestNativeDisallowIntercept(true) and return false. According to
          // the previous logic, if break is encountered at this point, the function will return
          // false and also call requestNativeDisallowIntercept(false). This may cause the ancestors
          // of LynxView to immediately consume the event, resulting in a bad case for
          // consume-slide-event.
          requestNativeDisallowIntercept(mCanConsumeSlideEvent);
        }
        return false;
      }
      case MotionEvent.ACTION_MOVE: {
        if (!mCanConsumeSlideEvent) {
          return false;
        }

        // Calculate the distance and angle of finger movement. To avoid the dead zone in angle
        // calculation during the initial stage of finger movement, when distanceX and distanceY are
        // small, return true first to make LynxView consume the event. When the finger moves beyond
        // a certain threshold, calculate the angle to determine whether to consume the slide event.
        float distanceX = ev.getX() - mDownPoint.x;
        float distanceY = ev.getY() - mDownPoint.y;
        float threshold = 10;
        if (Math.abs(distanceX) <= PixelUtils.dipToPx(threshold)
            && Math.abs(distanceY) <= PixelUtils.dipToPx(threshold)) {
          requestNativeDisallowIntercept(true);
          return false;
        }
        // If mConsumeSlideEvent != Undefined, then judge whether mConsumeSlideEvent == Enable. If
        // it is Enable, return true to consume subsequent events. Otherwise, return false to not
        // consume subsequent events.
        if (mConsumeSlideEvent != EventTarget.EnableStatus.Undefined) {
          break;
        }
        // When the finger moves, set the default value of mConsumeSlideEvent to Disable, then
        // calculate the sliding angle. If it needs to consume the angle, set mConsumeSlideEvent to
        // Enable. Otherwise, let mConsumeSlideEvent remain Disable.
        mConsumeSlideEvent = EventTarget.EnableStatus.Disable;

        // Use atan2(y, x) * 180 / PI to calculate the angle.
        float semicircleAngle = 180;
        double angle = Math.atan2(distanceY, distanceX) * semicircleAngle / Math.PI;
        EventTarget target = mActiveUI;
        while (target != null && target.parent() != target) {
          if (target.consumeSlideEvent((float) angle)) {
            mConsumeSlideEvent = EventTarget.EnableStatus.Enable;
            break;
          }
          target = target.parent();
        }
        break;
      }
      default:
        break;
    }
    boolean res = (mConsumeSlideEvent == EventTarget.EnableStatus.Enable);
    return requestNativeDisallowIntercept(res);
  }

  public boolean blockNativeEvent(MotionEvent ev) {
    if (mActiveUI == null) {
      return false;
    }
    EventTarget target = mActiveUI;
    while (target != null && target.parent() != target) {
      if (target.blockNativeEvent(ev)) {
        return true;
      }
      target = target.parent();
    }
    return false;
  }

  public boolean eventThrough() {
    if (mActiveUI == null) {
      return false;
    }
    return mActiveUI.eventThrough(mFirstFingerDownPoint.getX(), mFirstFingerDownPoint.getY());
  }

  public void setTapSlop(float tapSlop) {
    mTapSlop = tapSlop;
  }

  private int checkCanRespondTapOrClick(EventTarget ui, HashSet set) {
    if (ui == null) {
      return 0;
    }
    if (set == null || set.isEmpty()) {
      return -1;
    }
    int res = -1;
    EventTarget parent = ui;
    while (parent != null && parent.parent() != parent) {
      if (set.contains(parent.getSign())) {
        res = parent.getSign();
        break;
      }
      parent = parent.parent();
    }
    return res;
  }

  // Specify whether the tap/click event can be triggered when the ui slides.
  // At this time, the ui slide is triggered by a gesture.
  private int canRespondTapOrClick(EventTarget ui) {
    return checkCanRespondTapOrClick(ui, mGestureRecognizedUISet);
  }

  // Specify whether the tap/click event can be triggered when the ui slides.
  // At this time, the ui slide is triggered by the front-end modifying the
  // animation or layout attributes.
  private int canRespondTapOrClickWhenUISlideWithProps(EventTarget ui) {
    return checkCanRespondTapOrClick(ui, mPropsChangedUISet);
  }

  private void initTouchEnv(MotionEvent ev) {
    mTouchMoved = false;
    mTouchMoving = false;
    mShouldCheckMove = true;
    mDownPoint = new PointF(ev.getX(), ev.getY());
    mGestureRecognized = false;
    mPrePoint = new PointF(ev.getX(), ev.getY());
    mGestureRecognizedUISet.clear();
    mPropsChangedUISet.clear();
    mHasMultiTouch = false;
    mActiveUIMap.clear();
    mActiveTargetMap.clear();
  }

  private void initClickEnv() {
    mActiveClickList.clear();
    if (mActiveUI == null) {
      return;
    }
    EventTarget tempUi = mActiveUI;
    while (tempUi != null) {
      mActiveClickList.push(tempUi);
      tempUi = tempUi.parent();
    }
    while (!mActiveClickList.isEmpty()
        && (mActiveClickList.getLast().getEvents() == null
            || !mActiveClickList.getLast().getEvents().containsKey(EVENT_CLICK))) {
      mActiveClickList.removeLast();
    }
    for (EventTarget target : mActiveClickList) {
      target.onResponseChain();
    }
    if (mActiveClickList.isEmpty()) {
      mTouchOutSide = true;
    } else {
      mTouchOutSide = false;
    }
  }

  private boolean onTouchMove(MotionEvent ev, int index) {
    boolean res = false;
    boolean firstTouchMoved = false;
    EventTargetDetail detail = mActiveUIMap.get(ev.getPointerId(index));
    if (detail != null) {
      PointF prePoint = detail.getPrePoint();
      PointF downPoint = detail.getDownPoint();
      // TODO(hexionghui): Fix the problem: In single-finger mode, the pressing position of the
      // first finger and the current position of the last finger are always used to determine
      // whether to move.
      if (!mEnableMultiTouch) {
        downPoint = mDownPoint;
      }
      // TODO(hexionghui): Align with the tapslop logic on the iOS side: xDiff^2 + yDiff^2 >
      // tapslop^2
      if (prePoint.x != ev.getX(index) || prePoint.y != ev.getY(index)) {
        if (Math.abs(downPoint.x - ev.getX(index)) > mTapSlop
            || Math.abs(downPoint.y - ev.getY(index)) > mTapSlop) {
          mTouchMoved = true;
          if (ev.getPointerId(index) == 0) {
            firstTouchMoved = true;
          }
        }
        prePoint.x = ev.getX(index);
        prePoint.y = ev.getY(index);
        mPrePoint = prePoint;
        res = true;
      }

      detail.setPrePoint(prePoint);
    }

    if (res) {
      // For better performance, the following code is executed only when the touch position
      // changes. Because the response chain does not change when the touch position does not
      // change, thus, there is no need to exec the following code.
      EventTarget target = findUI(ev, index, mUIOwner.getRootUI());
      mTouchOutSide = mGestureRecognized
          || (!mActiveClickList.isEmpty() && canRespondTapOrClick(mActiveClickList.getLast()) != -1)
          || mTouchOutSide || eventOutSideActiveList(ev, target);
      // Only when the first finger is moved, :active will be disabled.
      if (!mActiveUIList.isEmpty() && (!mEnableMultiTouch && mTouchMoved)
          || (mEnableMultiTouch && firstTouchMoved) || canRespondTapOrClick(mActiveUI) != -1
          || canRespondTapOrClickWhenUISlideWithProps(mActiveUI) != -1) {
        // If touchMoved, tap will not be triggered, so remove the active state of all active UI.
        deactivatePseudoState(LynxTouchEvent.kPseudoStateActive);
      }
    }

    return res;
  }

  public void fireClick(MotionEvent e) {
    // For the click event, it only support single finger.
    if (mActiveClickList.isEmpty() || mActiveClickList.getLast() == null) {
      return;
    }
    int slideTargetSign = canRespondTapOrClick(mActiveClickList.getLast());
    int propsTargetSign = canRespondTapOrClickWhenUISlideWithProps(mActiveClickList.getLast());
    if ((!mEnableMultiTouch || !mHasMultiTouch) && !mTouchOutSide && !mGestureRecognized
        && !mActiveClickList.isEmpty() && mActiveClickList.getLast() != null
        && slideTargetSign == -1 && propsTargetSign == -1) {
      dispatchEvent(mActiveClickList.getLast(), EVENT_CLICK, e);
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        e.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().fireClick(e);
      }
    }
  }

  public void fireTap(MotionEvent e) {
    // For the tap event, it only support single finger.
    int slideTargetSign = canRespondTapOrClick(mActiveUI);
    int propsTargetSign = canRespondTapOrClickWhenUISlideWithProps(mActiveUI);
    if ((!mEnableMultiTouch || !mHasMultiTouch) && !mGestureRecognized && !mTouchMoved
        && slideTargetSign == -1 && propsTargetSign == -1) {
      if (LynxEnv.inst().isHighlightTouchEnabled()) {
        showMessageOnConsole(
            TAG + ": fire tap for target " + mActiveUI.getSign(), LogBoxLogLevel.Info.ordinal());
      }
      dispatchEvent(mActiveUI, EVENT_TAP, e);
    } else {
      if (LynxEnv.inst().isHighlightTouchEnabled()) {
        showMessageOnConsole(TAG + ": tap failed due to [gesture] " + mGestureRecognized
                + ", [move] " + mTouchMoved + ", [slide] " + slideTargetSign + ", [props] "
                + propsTargetSign,
            LogBoxLogLevel.Warn.ordinal());
      }
      LLog.i(TAG,
          "tap failed:" + mGestureRecognized + " " + mTouchMoved + " " + slideTargetSign + " "
              + propsTargetSign);
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        e.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().fireTap(e);
      }
    }
  }

  public void fireLongpress(MotionEvent e) {
    int slideTargetSign = canRespondTapOrClick(mActiveUI);
    int propsTargetSign = canRespondTapOrClickWhenUISlideWithProps(mActiveUI);
    if ((!mEnableMultiTouch || !mHasMultiTouch) && mActiveUI != null && slideTargetSign == -1
        && propsTargetSign == -1) {
      dispatchEvent(mActiveUI, EVENT_LONG_PRESS, e);
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        e.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().fireLongpress(e);
      }
    }
  }

  private void resetEnv() {
    for (EventTarget target : mActiveClickList) {
      target.offResponseChain();
    }
    mActiveUIList.clear();
    mActiveClickList.clear();
    mGestureRecognizedUISet.clear();
    mPropsChangedUISet.clear();
    mTouchMoved = false;
    mTouchMoving = false;
    mShouldCheckMove = true;
    mHasMultiTouch = false;
    mActiveUIMap.clear();
    mActiveTargetMap.clear();
    mPreTarget = mActiveUI;
  }

  public void destroy() {}
  // When ActionDown, generate event target response chain. And traversed the event target response
  // chain to make the target's touch state pseudo-class take effect.
  private void onActionDown(MotionEvent ev) {
    // TODO(songshourui.null): for fiber Arch, need to enable mHasTouchPseudo by default
    if (eventEmitter() == null) {
      return;
    }
    EventTarget target = mActiveUI;
    while (target != null) {
      mActiveUIList.push(target);
      if (!target.enableTouchPseudoPropagation()) {
        break;
      }
      target = target.parent();
    }
    for (int i = 0; i < mActiveUIList.size(); ++i) {
      EventTarget ui = mActiveUIList.get(i);
      if (mHasTouchPseudo) {
        // Only execute this function if mHasTouchPseudo is true
        eventEmitter().onPseudoStatusChanged(
            ui.getSign(), LynxTouchEvent.kPseudoStateNone, LynxTouchEvent.kPseudoStateActive);
      }
      ui.onPseudoStatusChanged(LynxTouchEvent.kPseudoStateNone, LynxTouchEvent.kPseudoStateActive);
    }
  }

  // When ActionMove, the touched event target may change. Disable the touch pseudo class for
  // targets not on the response chain.
  void onActionMove(MotionEvent ev, EventTarget target) {
    if (mUIOwner == null || mUIOwner.getRootUI() == null || mActiveUIList.isEmpty()
        || eventEmitter() == null) {
      return;
    }
    EventTarget newUI = target;
    LinkedList<EventTarget> list = new LinkedList<>();
    while (newUI != null) {
      list.push(newUI);
      if (!newUI.enableTouchPseudoPropagation()) {
        break;
      }
      newUI = newUI.parent();
    }

    int index = -1;
    for (int i = 0; i < mActiveUIList.size() && i < list.size(); ++i) {
      EventTarget preTarget = mActiveUIList.get(i);
      EventTarget nowTarget = list.get(i);
      if (preTarget.getSign() != nowTarget.getSign()) {
        break;
      }
      index = i;
    }

    for (int i = mActiveUIList.size() - 1; i >= index + 1; --i) {
      EventTarget ui = mActiveUIList.get(i);
      if (mHasTouchPseudo) {
        // Only execute this function if mHasTouchPseudo is true
        eventEmitter().onPseudoStatusChanged(
            ui.getSign(), LynxTouchEvent.kPseudoStateActive, LynxTouchEvent.kPseudoStateNone);
      }
      ui.onPseudoStatusChanged(LynxTouchEvent.kPseudoStateActive, LynxTouchEvent.kPseudoStateNone);
      mActiveUIList.remove(i);
    }
  }

  // When ActionUp, the touched event target may change. Disable the touch pseudo class for all
  // targets.
  private void onActionUpOrCancel(MotionEvent ev) {
    deactivatePseudoState(LynxTouchEvent.kPseudoStateAll);
  }

  private void deactivatePseudoState(int state) {
    if (eventEmitter() == null) {
      return;
    }
    for (EventTarget ui : mActiveUIList) {
      if (mHasTouchPseudo) {
        // Only execute this function if mHasTouchPseudo is true
        eventEmitter().onPseudoStatusChanged(
            ui.getSign(), ui.getPseudoStatus(), ui.getPseudoStatus() & ~state);
      }
      ui.onPseudoStatusChanged(ui.getPseudoStatus(), ui.getPseudoStatus() & ~state);
    }
  }

  /**
   * 分发多点触摸事件到JavaScript - 事件传递的关键桥梁
   *
   * 方法功能：将多点触摸事件封装为LynxTouchEvent，通过EventEmitter发送到JavaScript
   * 这是事件从Java层传递到C++/JavaScript层的核心方法之一
   *
   * 事件传递链路：
   * dispatchEvent → eventEmitter().sendMultiTouchEvent → LynxEngineProxy.sendMultiTouchEvent →
   * nativeSendMultiTouchEvent (JNI) → C++事件系统 → JavaScript事件监听器
   *
   * 关键操作：
   * 1. 创建LynxTouchEvent对象，包含事件类型、坐标、时间戳等信息
   * 2. 设置活动目标映射（mActiveTargetMap），包含所有触摸点对应的UI
   * 3. 分发冒泡触摸事件到手势竞技场（用于手势冲突处理）
   * 4. 通过EventEmitter发送事件到JavaScript
   *
   * @param eventName 事件名称（如EVENT_TOUCH_START、EVENT_TOUCH_MOVE等）
   * @param ev Android MotionEvent对象
   * @param map JavaOnlyMap对象，包含所有触摸点的详细信息
   *           结构：{sign: [[pointerId, clientX, clientY, pageX, pageY, viewX, viewY], ...]}
   */
  // dispatch event for touch* .
  // TODO(hexionghui): Merge two dispatchEvent interfaces into one.
  private void dispatchEvent(String eventName, MotionEvent ev, JavaOnlyMap map) {
    // 初始化第一个Lynx触摸事件对象
    // 包含事件类型、目标UI签名、坐标信息等
    mFirstLynxTouchEvent = initialFirstLynxTouchEvent(mActiveUI, eventName, ev);
    mFirstLynxTouchEvent.setMotionEvent(ev);

    // 创建多点触摸事件对象
    LynxTouchEvent event = new LynxTouchEvent(eventName, map);
    event.setMotionEvent(ev);
    // 设置活动目标映射，包含所有触摸点对应的UI
    event.setActiveTargetMap(mActiveTargetMap);
    // 设置主要目标UI（第一个手指按下的UI）
    event.setTarget(mActiveUI);
    // 设置事件时间戳
    event.setTimestamp(mTimestamp);

    // 如果启用手势竞技场，分发冒泡触摸事件
    // 手势竞技场用于处理手势的竞争和冲突解决
    if (mGestureArenaManager != null) {
      mGestureArenaManager.dispatchBubbleTouchEvent(eventName, mFirstLynxTouchEvent);
    }

    // 关键调用：通过EventEmitter发送多点触摸事件到JavaScript
    // eventEmitter()返回LynxEventEmitter实例，负责事件的中转
    eventEmitter().sendMultiTouchEvent(event);
  }

  /**
   * 分发单点触摸事件到JavaScript - 用于tap、click、longpress等手势事件
   *
   * 方法功能：将单点触摸事件封装为LynxTouchEvent，通过EventEmitter发送到JavaScript
   * 这是手势事件（点击、长按等）传递到JavaScript的核心方法
   *
   * 坐标系统：
   * 1. 客户端坐标（clientPoint）：相对于屏幕的坐标
   * 2. 页面坐标（pagePoint）：相对于LynxView的坐标
   * 3. 视图坐标（viewPoint）：相对于目标UI的本地坐标
   *
   * 事件类型：
   * - EVENT_TOUCH_START: 触摸开始
   * - EVENT_TOUCH_MOVE: 触摸移动
   * - EVENT_TOUCH_END: 触摸结束
   * - EVENT_TOUCH_CANCEL: 触摸取消
   * - EVENT_TAP: 轻触
   * - EVENT_CLICK: 点击
   * - EVENT_LONG_PRESS: 长按
   *
   * @param target 目标UI元素
   * @param eventName 事件名称
   * @param ev Android MotionEvent对象
   */
  // dispatch event for tap, click, longpress.
  private void dispatchEvent(EventTarget target, String eventName, MotionEvent ev) {
    // 坐标转换：将页面坐标转换为目标UI的本地坐标
    mTargetPoint = convertToViewPoint(mActiveUI, new Point(ev.getX(0), ev.getY(0)));

    // 页面坐标：相对于LynxView的坐标
    LynxTouchEvent.Point pagePoint = new LynxTouchEvent.Point(ev.getX(0), ev.getY(0));

    // 客户端坐标：相对于屏幕的坐标
    // 通过LynxUIHelper.convertPointFromUIToScreen进行坐标转换
    PointF point = LynxUIHelper.convertPointFromUIToScreen(
        mUIOwner.getRootUI(), new PointF(pagePoint.getX(), pagePoint.getY()));
    LynxTouchEvent.Point clientPoint = new Point(point.x, point.y);

    // 创建LynxTouchEvent对象，包含三种坐标系统和目标UI签名
    mFirstLynxTouchEvent =
        new LynxTouchEvent(target.getSign(), eventName, clientPoint, pagePoint, mTargetPoint);
    mFirstLynxTouchEvent.setMotionEvent(ev);
    mFirstLynxTouchEvent.setTarget(mActiveUI);
    mFirstLynxTouchEvent.setTimestamp(mTimestamp);

    // 如果是触摸开始事件，进行命中目标检查
    if (EVENT_TOUCH_START.equals(eventName)) {
      // 检查命中目标（用于开发工具调试）
      inspectHitTarget();
      // 如果启用了触摸高亮，在控制台显示命中信息
      if (LynxEnv.inst().isHighlightTouchEnabled()) {
        showMessageOnConsole(TAG + ": hit the target with sign = " + target.getSign(),
            LogBoxLogLevel.Info.ordinal());
      }
    }

    // 检查EventEmitter是否可用
    if (eventEmitter() == null) {
      LLog.i(TAG, "dispatchEvent failed since eventEmitter() null");
      return;
    }

    // 如果启用手势竞技场，分发冒泡触摸事件
    // 冒泡事件用于手势竞技场中的手势识别和冲突解决
    if (mGestureArenaManager != null) {
      mGestureArenaManager.dispatchBubbleTouchEvent(eventName, mFirstLynxTouchEvent);
    }

    // 关键调用：通过EventEmitter发送单点触摸事件到JavaScript
    // 最终会调用LynxEngineProxy.sendTouchEvent → nativeSendTouchEvent
    eventEmitter().sendTouchEvent(mFirstLynxTouchEvent);
  }

  /**
   * Used to output logs to the console of DevTool. This function is effective only when DevTool is
   * connected.
   * @param msg Information related to event processing.
   * @param level The log level.
   */
  // TODO(hexionghui): Postpone string concatenation and encapsulate the method in LynxDevtool.
  private void showMessageOnConsole(String msg, int level) {
    if (mUIOwner.getRootUI() == null || mUIOwner.getRootUI().getBodyView() == null) {
      return;
    }
    LynxBaseInspectorOwner inspectorOwner =
        ((LynxView) mUIOwner.getRootUI().getBodyView()).getBaseInspectorOwner();
    if (inspectorOwner == null) {
      return;
    }
    inspectorOwner.showMessageOnConsole(msg, level);
  }

  /**
   * Used to highlight the nodes that are actually touched. This function takes effect only when
   * LynxDevtool and HighlightTouch are turned on.
   */
  private void inspectHitTarget() {
    if (!LynxEnv.inst().isHighlightTouchEnabled() || mActiveUI == null
        || mUIOwner.getRootUI() == null || mUIOwner.getRootUI().getBodyView() == null) {
      return;
    }
    LynxBaseInspectorOwner inspectorOwner =
        ((LynxView) mUIOwner.getRootUI().getBodyView()).getBaseInspectorOwner();
    if (inspectorOwner == null) {
      return;
    }
    try {
      JSONObject json = new JSONObject();
      json.put("id", 1);
      json.put("method", "DOM.setAttributesAsText");
      if (mPreTarget != null && mPreTargetInlineCSSText != null) {
        JSONObject params = new JSONObject();
        params.put("nodeId", mPreTarget.getSign());
        params.put("text", String.format("style=\"%s\"", mPreTargetInlineCSSText));
        params.put("name", "style");
        json.put("params", params);
        inspectorOwner.invokeCDPFromSDK(json.toString(), new CDPResultCallback() {
          @Override
          public void onResult(String result) {
            LLog.i(TAG, "DOM.setAttributesAsText:" + result);
          }
        });
      }

      json.put("method", "CSS.getInlineStylesForNode");
      JSONObject params = new JSONObject();
      params.put("nodeId", mActiveUI.getSign());
      json.put("params", params);
      inspectorOwner.invokeCDPFromSDK(json.toString(), new CDPResultCallback() {
        @Override
        public void onResult(String result) {
          try {
            setAttributeByInvokeCDP(json, params, inspectorOwner, result);
          } catch (JSONException e) {
            LLog.e(TAG, "setAttributeByInvokeCDP error:" + e.toString());
          }
        }
      });

    } catch (Exception e) {
      LLog.e(TAG, "inspectHitTarget json generate error");
    }
  }

  private void setAttributeByInvokeCDP(JSONObject json, JSONObject params,
      LynxBaseInspectorOwner inspectorOwner, String inlineStyle) throws JSONException {
    String regex = "\"cssText\"\\s*:\\s*\"([^\"]*)\"";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(inlineStyle);
    if (matcher.find()) {
      mPreTargetInlineCSSText = matcher.group(1);
    }

    String cssText = (mPreTargetInlineCSSText != null ? mPreTargetInlineCSSText : "")
        + "background-color:#9CC4E6;border-width:2px;border-color:red;";
    json.put("method", "DOM.setAttributesAsText");
    params.put("text", String.format("style=\"%s\"", cssText));
    params.put("name", "style");
    json.put("params", params);
    inspectorOwner.invokeCDPFromSDK(json.toString(), new CDPResultCallback() {
      @Override
      public void onResult(String result) {
        LLog.i(TAG, "DOM.setAttributesAsText:" + result);
      }
    });
  }

  private boolean shouldTriggerMove(MotionEvent ev) {
    if (mShouldCheckMove) {
      float distanceX = ev.getX(0) - mDownPoint.x;
      float distanceY = ev.getY(0) - mDownPoint.y;
      if (Math.abs(distanceX) > mMoveSlop || Math.abs(distanceY) > mMoveSlop) {
        mShouldCheckMove = false;
        return true;
      }
      return false;
    }
    return true;
  }

  /**
   * 处理第一个手指按下事件 - 触摸序列的起点
   *
   * 方法功能：初始化触摸环境，执行命中测试，分发TOUCH_START事件
   * 这是触摸事件处理的核心入口，负责：
   * 1. 命中测试（hitTest）找到被触摸的UI元素
   * 2. 坐标转换（页面坐标 → 视图坐标）
   * 3. 事件穿透检查
   * 4. 初始化触摸环境数据结构
   * 5. 分发TOUCH_START事件到JavaScript
   * 6. 激活伪类状态（:active）
   * 7. 处理子LynxPageUI的事件传递
   *
   * 关键操作：
   * - findUI: 通过hitTest找到触摸点下的UI元素
   * - convertToViewPoint: 将页面坐标转换为UI元素的本地坐标
   * - eventThrough: 检查UI是否允许事件穿透
   * - dispatchEvent: 分发事件到JavaScript
   * - onActionDown: 激活:active伪类状态
   *
   * @param ev MotionEvent对象
   * @param rootUi 根UI组，用于命中测试
   * @return true表示事件被处理，false表示事件穿透
   */
  public boolean handleFirstTouchDown(MotionEvent ev, UIGroup rootUi) {
    // 重置第一个手指按下点坐标
    mFirstFingerDownPoint.setX(0);
    mFirstFingerDownPoint.setY(0);

    // ============ 第1步：命中测试（hitTest） ============
    // 通过findUI方法在UI树中查找触摸点下的目标UI元素
    // findUI会调用rootUi.hitTest(x, y)进行递归查找
    mActiveUI = findUI(ev, 0, rootUi);

    // ============ 第2步：坐标转换 ============
    // 创建页面坐标点（相对于LynxView的坐标）
    LynxTouchEvent.Point pagePoint = new Point(ev.getX(), ev.getY());
    mFirstFingerDownPoint = pagePoint;

    // 如果目标UI是LynxBaseUI，将页面坐标转换为UI的本地坐标
    // 例如：如果UI在(100,100)位置，触摸点在(150,150)，则本地坐标为(50,50)
    if (mActiveUI instanceof LynxBaseUI) {
      mFirstFingerDownPoint = convertToViewPoint(mActiveUI, pagePoint);
    }

    // ============ 第3步：事件穿透检查 ============
    // 检查UI是否配置了enableEventThrough属性
    // 如果允许事件穿透，返回false，事件不会由Lynx处理
    if (mActiveUI != null
        && mActiveUI.eventThrough(mFirstFingerDownPoint.getX(), mFirstFingerDownPoint.getY())) {
      return false;
    }

    // ============ 第4步：初始化触摸环境 ============
    // 初始化触摸相关的环境变量和数据结构
    initTouchEnv(ev);
    // 初始化点击环境（用于后续的click/tap事件处理）
    initClickEnv();

    // ============ 第5步：更新活动UI映射 ============
    // 将第一个手指（pointerId=0）映射到目标UI
    // EventTargetDetail包含UI引用和按下点坐标
    mActiveUIMap.put(ev.getPointerId(0), new EventTargetDetail(mActiveUI, ev.getX(0), ev.getY(0)));
    // 将UI签名映射到UI对象，用于后续事件分发
    mActiveTargetMap.put(mActiveUI.getSign(), mActiveUI);

    // ============ 第6步：手势竞技场设置 ============
    // 如果启用手势竞技场，将激活的UI设置到竞技场
    // 手势竞技场用于处理复杂的手势竞争（如滑动冲突）
    if (mGestureArenaManager != null) {
      mGestureArenaManager.setActiveUIToArenaAtDownEvent(mActiveUI);
    }

    // ============ 第7步：配置长按超时 ============
    // 获取系统默认的长按超时时间，或使用LynxContext自定义的超时时间
    int longPressDuration = ViewConfiguration.getLongPressTimeout();
    if (mUIOwner.getContext().getLongPressDuration() >= 0) {
      longPressDuration = mUIOwner.getContext().getLongPressDuration();
    }
    // 设置手势识别器的长按超时时间
    mDetector.setLongPressTimeout(longPressDuration);

    // ============ 第8步：分发TOUCH_START事件到JavaScript ============
    // 这是事件传递到JavaScript的关键步骤
    if (mEnableMultiTouch) {
      // 多点触摸模式：使用JavaOnlyMap传递所有触摸点信息
      JavaOnlyMap map = new JavaOnlyMap();
      addMap(map, ev, 0);  // 将第一个手指的信息添加到map
      dispatchEvent(EVENT_TOUCH_START, ev, map);  // 分发多点触摸事件
    } else {
      // 单点触摸模式：直接分发事件到目标UI
      dispatchEvent(mActiveUI, EVENT_TOUCH_START, ev);  // 分发单点触摸事件
    }

    // ============ 第9步：激活伪类状态 ============
    // TODO(hexionghui): :active逻辑应该只支持单指。但在Android端，同时触摸两个手指会触发两次onTouchEvent，
    // 导致Android端的:active在两个手指同时触摸时也会生效。
    // 激活:active伪类状态，改变UI的视觉样式
    onActionDown(ev);

    // ============ 第10步：处理子LynxPageUI事件传递 ============
    // 如果当前UI包含子LynxPageUI，需要将事件传递给子页面的事件分发器
    // 这支持嵌套的Lynx页面结构
    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        // 调整事件坐标到子页面的坐标系
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        // 递归调用子页面的事件分发器
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleFirstTouchDown(
            ev, childLynxPageUI);
      }
    }

    // 返回true表示事件被成功处理
    return true;
  }

  public void handleOtherTouchDown(MotionEvent ev, UIGroup rootUi) {
    mHasMultiTouch = true;
    // mActiveUIMap should always be updated rather than controlled by mEnableMultiTouch to
    // prevent NPE errors in onTouchMove.
    int index = ev.getActionIndex();
    EventTarget active_target = findUI(ev, index, rootUi);
    mActiveUIMap.put(ev.getPointerId(index),
        new EventTargetDetail(active_target, ev.getX(index), ev.getY(index)));
    mActiveTargetMap.put(active_target.getSign(), active_target);
    if (mEnableMultiTouch) {
      JavaOnlyMap map = new JavaOnlyMap();
      addMap(map, ev, index);
      dispatchEvent(EVENT_TOUCH_START, ev, map);
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleOtherTouchDown(
            ev, childLynxPageUI);
      }
    }
  }

  public void handleTouchMove(MotionEvent ev) {
    if (shouldTriggerMove(ev)) {
      mTouchMoving = true;
      if (mEnableMultiTouch) {
        JavaOnlyMap map = new JavaOnlyMap();
        for (int index = 0; index < ev.getPointerCount(); ++index) {
          if (onTouchMove(ev, index)) {
            addMap(map, ev, index);
          }
        }
        dispatchEvent(EVENT_TOUCH_MOVE, ev, map);
      } else {
        if (onTouchMove(ev, 0)) {
          dispatchEvent(mActiveUI, EVENT_TOUCH_MOVE, ev);
        }
      }
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleTouchMove(ev);
      }
    }
  }

  public void handleOtherTouchUp(MotionEvent ev) {
    int index = ev.getActionIndex();
    if (mEnableMultiTouch) {
      if (ev.getPointerId(index) == 0) {
        onActionUpOrCancel(ev);
      }
      JavaOnlyMap map = new JavaOnlyMap();
      addMap(map, ev, index);
      dispatchEvent(EVENT_TOUCH_END, ev, map);
    }
    mActiveUIMap.remove(ev.getPointerId(index));

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleOtherTouchUp(ev);
      }
    }
  }

  public void handleFirstTouchUp(MotionEvent ev) {
    if (mActiveUI != null && !mActiveUI.ignoreFocus() && !mGestureRecognized
        && canRespondTapOrClick(mActiveUI) == -1
        && canRespondTapOrClickWhenUISlideWithProps(mActiveUI) == -1) {
      EventTarget prevActiveUI = mFocusedUI;
      mFocusedUI = mActiveUI;
      if (mActiveUI != prevActiveUI) {
        if (mActiveUI != null && mActiveUI.isFocusable()) {
          mActiveUI.onFocusChanged(true, prevActiveUI != null && prevActiveUI.isFocusable());
        }
        if (prevActiveUI != null && prevActiveUI.isFocusable()) {
          prevActiveUI.onFocusChanged(false, mActiveUI != null && mActiveUI.isFocusable());
        }
      }
    }

    if (mEnableMultiTouch) {
      JavaOnlyMap map = new JavaOnlyMap();
      addMap(map, ev, 0);
      dispatchEvent(EVENT_TOUCH_END, ev, map);
    } else {
      dispatchEvent(mActiveUI, EVENT_TOUCH_END, ev);
    }

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleFirstTouchUp(ev);
      }
    }
  }

  public void handleTouchCancel(MotionEvent ev) {
    if (mEnableMultiTouch) {
      JavaOnlyMap map = new JavaOnlyMap();
      for (EventTargetDetail detail : mActiveUIMap.values()) {
        addMap(map, ev, 0);
      }
      dispatchEvent(EVENT_TOUCH_CANCEL, ev, map);
    } else {
      dispatchEvent(mActiveUI, EVENT_TOUCH_CANCEL, ev);
    }

    onActionUpOrCancel(ev);
    resetEnv();

    if (mActiveUI != null && mActiveUI.getChildrenLynxPageUI() != null) {
      UIBody childLynxPageUI = (UIBody) mActiveUI.getChildrenLynxPageUI().get(
          String.valueOf(System.identityHashCode(mActiveUI)));
      if (childLynxPageUI != null && childLynxPageUI.getLynxContext() != null
          && childLynxPageUI.getLynxContext().getTouchEventDispatcher() != null) {
        ev.setLocation(
            mFirstLynxTouchEvent.getViewPoint().getX(), mFirstLynxTouchEvent.getViewPoint().getY());
        childLynxPageUI.getLynxContext().getTouchEventDispatcher().handleTouchCancel(ev);
      }
    }
  }

  public void onPlatformGestureStatusChanged(int status) {
    mIsPlatformGestureActive = status == GestureConstants.LYNX_STATE_ACTIVE;
  }

  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mEnablePlatformGesture) {
      return mIsPlatformGestureActive;
    }
    return false;
  }

  /**
   * TouchEventDispatcher核心事件处理方法 - Lynx事件系统的Java层核心
   *
   * 方法功能：处理触摸事件的完整生命周期，包括单点触摸、多点触摸、手势识别等
   * 这是Lynx事件分发链路的Java层核心，负责：
   * 1. 事件状态管理（按下、移动、抬起、取消）
   * 2. 命中测试（hitTest）找到目标UI元素
   * 3. 事件分发到JavaScript（通过EventEmitter）
   * 4. 手势识别（通过GestureRecognizer）
   * 5. 手势竞技场管理（GestureArenaManager）
   *
   * 完整的事件分发链路：
   * Android系统 → LynxView.dispatchTouchEvent → LynxTemplateRender.dispatchTouchEvent →
   * LynxUIRenderer.onTouchEvent → TouchEventDispatcher.onTouchEvent →
   * 1. handleFirstTouchDown/handleTouchMove/handleFirstTouchUp等具体处理方法
   * 2. dispatchEvent → EventEmitter.sendTouchEvent → LynxEngineProxy.sendTouchEvent →
   *    nativeSendTouchEvent (JNI调用) → C++事件系统 → JavaScript事件监听器
   *
   * 关键数据结构：
   * - mActiveUI: 当前激活的UI元素（第一个手指按下的目标）
   * - mActiveUIMap: 手指ID到UI元素的映射（支持多点触摸）
   * - mActiveTargetMap: UI签名到UI元素的映射
   * - mFirstFingerDownPoint: 第一个手指按下的坐标（用于事件穿透检查）
   * - mFirstLynxTouchEvent: 第一个触摸事件对象
   *
   * @param ev Android MotionEvent对象
   * @param rootUi 根UI组，用于命中测试
   * @return true表示事件被消费，false表示事件未被消费（事件穿透）
   */
  public boolean onTouchEvent(MotionEvent ev, UIGroup rootUi) {
    // 记录事件时间戳，用于事件排序和时序管理
    mTimestamp = System.currentTimeMillis();

    // ============ 第1步：事件类型分发 ============

    // ACTION_DOWN事件：第一个手指按下，开始新的触摸序列
    if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
      mIsPlatformGestureActive = false;  // 重置平台手势激活状态

      // 处理第一个手指按下事件
      // handleFirstTouchDown会执行：命中测试、初始化环境、分发TOUCH_START事件
      if (!handleFirstTouchDown(ev, rootUi)) {
        // 如果handleFirstTouchDown返回false，表示事件穿透（eventThrough）
        // 事件不会被Lynx消费，传递给下层View处理
        LLog.i(TAG, "hit event through");
        return false;
      }
    }
    // ACTION_POINTER_DOWN事件：其他手指按下（多点触摸）
    else if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
      // 处理其他手指按下事件
      // 会更新mActiveUIMap，为新的手指分配目标UI
      handleOtherTouchDown(ev, rootUi);
    }
    // 其他事件：移动、抬起、取消
    else {
      // 只有在有激活的UI且触摸映射不为空时才处理
      // 这确保事件序列的完整性（必须有DOWN事件才能处理后续事件）
      if (mActiveUI != null && !mActiveUIMap.isEmpty()) {
        // 检查事件穿透：如果当前激活的UI允许事件穿透，直接返回false
        // eventThrough方法检查UI是否配置了enableEventThrough属性
        if (mActiveUI.eventThrough(mFirstFingerDownPoint.getX(), mFirstFingerDownPoint.getY())) {
          LLog.i(TAG, "hit event through");
          return false;
        }

        // 根据事件类型分发到具体的处理方法
        switch (ev.getActionMasked()) {
          case MotionEvent.ACTION_MOVE:
            // 处理移动事件：更新手指位置，分发TOUCH_MOVE事件
            handleTouchMove(ev);
            break;
          case MotionEvent.ACTION_POINTER_UP:
            // 处理其他手指抬起事件：从映射中移除，分发TOUCH_END事件
            handleOtherTouchUp(ev);
            break;
          case MotionEvent.ACTION_UP:
            // 处理第一个手指抬起事件：结束触摸序列
            handleFirstTouchUp(ev);

            // TODO(hexionghui): 修复问题：在单指模式下，只有当最后一个手指抬起时，:active状态才会被禁用
            // 更新伪类状态（:active等）
            onActionUpOrCancel(ev);

            // 触发点击事件（click）
            fireClick(ev);

            // TODO(hexionghui): tap事件应该由第一个手指抬起触发，而不是最后一个手指
            // 触发轻触事件（tap）
            fireTap(ev);

            // 重置环境，准备下一个触摸序列
            resetEnv();
            break;
          case MotionEvent.ACTION_CANCEL:
            // 处理取消事件：分发TOUCH_CANCEL事件，重置环境
            handleTouchCancel(ev);
            break;
          default:
            break;
        }
      }
    }

    // ============ 第2步：最终的事件穿透检查 ============

    // 再次检查事件穿透，确保在事件处理过程中状态没有变化
    // 这是防御性编程，防止在事件处理过程中UI状态发生变化
    if (mActiveUI != null
        && mActiveUI.eventThrough(mFirstFingerDownPoint.getX(), mFirstFingerDownPoint.getY())) {
      LLog.i(TAG, "hit event through");
      return false;
    }

    // ============ 第3步：事件分发到UI元素和手势识别器 ============

    // 如果存在激活的UI，将事件分发给UI自身的dispatchTouch方法
    // 这允许UI元素处理自定义的触摸逻辑
    if (mActiveUI != null) {
      mActiveUI.dispatchTouch(ev);
    }

    // 将事件传递给手势识别器（GestureRecognizer）
    // mDetector负责识别点击、长按、滑动等手势
    // 手势识别结果会通过GestureRecognizer.SimpleOnGestureListener回调
    mDetector.onTouchEvent(ev);

    // ============ 第4步：手势竞技场事件分发 ============

    // 如果存在手势竞技场管理器，将事件分发给手势竞技场
    // 手势竞技场用于处理复杂的手势竞争和冲突解决
    // 例如：ScrollView和ViewPager的滑动冲突
    if (mGestureArenaManager != null) {
      mGestureArenaManager.dispatchTouchEventToArena(ev, mFirstLynxTouchEvent);
    }

    // ============ 第5步：返回事件消费结果 ============

    // 返回true表示事件被Lynx消费，不会传递给其他View
    // 注意：如果前面返回了false（事件穿透），不会执行到这里
    return true;
  }

  private void addMap(JavaOnlyMap map, MotionEvent ev, int index) {
    EventTargetDetail detail = mActiveUIMap.get(ev.getPointerId(index));
    if (detail == null) {
      return;
    }

    EventTarget activeUI = detail.getUI();
    String sign = String.valueOf(activeUI.getSign());
    ReadableArray events = map.getArray(sign);
    JavaOnlyArray event = new JavaOnlyArray();

    LynxTouchEvent.Point pagePoint = new Point(ev.getX(index), ev.getY(index));
    LynxTouchEvent.Point targetPoint = pagePoint;
    if (activeUI instanceof LynxBaseUI) {
      targetPoint = convertToViewPoint(activeUI, pagePoint);
    }
    PointF point = LynxUIHelper.convertPointFromUIToScreen(
        mUIOwner.getRootUI(), new PointF(pagePoint.getX(), pagePoint.getY()));
    LynxTouchEvent.Point clientPoint = new Point(point.x, point.y);

    event.add(ev.getPointerId(index));
    event.add(clientPoint.getX());
    event.add(clientPoint.getY());
    event.add(pagePoint.getX());
    event.add(pagePoint.getY());
    event.add(targetPoint.getX());
    event.add(targetPoint.getY());
    if (events != null) {
      events.asArrayList().add(event);
    } else {
      JavaOnlyArray newEvents = new JavaOnlyArray();
      newEvents.add(event);
      map.putArray(sign, newEvents);
    }
  }

  public void setFocusedUI(LynxBaseUI focusedUI) {
    mFocusedUI = focusedUI;
  }

  private LynxTouchEvent.Point convertToViewPoint(
      EventTarget activeUI, LynxTouchEvent.Point pagePoint) {
    if (activeUI instanceof LynxBaseUI) {
      LynxBaseUI ui = (LynxBaseUI) activeUI;
      RectF viewRect = LynxUIHelper.convertRectFromUIToRootUI(
          ui, new RectF(0, 0, ui.getWidth(), ui.getHeight()));
      LynxTouchEvent.Point viewPoint =
          new Point(pagePoint.getX() - viewRect.left, pagePoint.getY() - viewRect.top);
      return viewPoint;
    }
    return pagePoint;
  }

  private LynxTouchEvent initialFirstLynxTouchEvent(
      EventTarget activeUI, String type, MotionEvent ev) {
    LynxTouchEvent.Point pagePoint = new LynxTouchEvent.Point(ev.getX(), ev.getY());
    PointF point = LynxUIHelper.convertPointFromUIToScreen(
        mUIOwner.getRootUI(), new PointF(pagePoint.getX(), pagePoint.getY()));
    LynxTouchEvent.Point clientPoint = new Point(point.x, point.y);
    return new LynxTouchEvent(activeUI.getSign(), type, clientPoint, pagePoint, mTargetPoint);
  }

  private EventEmitter eventEmitter() {
    return mUIOwner.getContext().getEventEmitter();
  }

  private EventTarget findUI(MotionEvent ev, int pointerIndex, UIGroup rootUi) {
    if (rootUi == null) {
      rootUi = mUIOwner.getRootUI();
    }
    return rootUi.hitTest(ev.getX(pointerIndex), ev.getY(pointerIndex));
  }

  private boolean eventOutSideActiveList(MotionEvent ev, EventTarget target) {
    if (mUIOwner == null || mUIOwner.getRootUI() == null) {
      return true;
    }
    EventTarget newUI = target;
    LinkedList<EventTarget> list = new LinkedList<>();
    while (newUI != null) {
      list.push(newUI);
      newUI = newUI.parent();
    }
    if (list.size() < mActiveClickList.size()) {
      return true;
    }
    for (int i = 0; i < mActiveClickList.size(); ++i) {
      EventTarget ui = mActiveClickList.get(i);
      if (ui == null || ui != list.get(i)) {
        return true;
      }
    }
    return false;
  }

  private class Listener extends GestureRecognizer.SimpleOnGestureListener {
    @Override
    public void onLongPress(MotionEvent e) {
      fireLongpress(e);
      super.onLongPress(e);
    }
  }

  public void reset() {
    mActiveUI = null;
    mFocusedUI = null;
    mActiveClickList.clear();
  }
}

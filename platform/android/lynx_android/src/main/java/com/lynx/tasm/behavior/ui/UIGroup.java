// Copyright 2019 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.lynx.tasm.behavior.ui;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;

import com.lynx.base.log.ALog;
import com.lynx.tasm.base.LLog;
import com.lynx.tasm.behavior.LynxContext;
import com.lynx.tasm.behavior.event.EventTarget;
import com.lynx.tasm.behavior.ui.image.FlattenUIImage;
import com.lynx.tasm.behavior.ui.list.UIList;
import com.lynx.tasm.behavior.ui.text.FlattenUIText;
import com.lynx.tasm.behavior.ui.utils.BackgroundDrawable;
import com.lynx.tasm.behavior.ui.view.AndroidView;
import com.lynx.tasm.rendernode.compat.RenderNodeCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * TODO 这个类是干什么的呢???为什么他里面才有hook的逻辑呢??放到其他的View里面不行呢??
 * @param <T>
 */
public abstract class UIGroup<T extends ViewGroup>
    extends LynxUI<T> implements UIParent, IDrawChildHook {
  private static final String TAG = "UIGroup";

  private int mCurrentDrawIndex = 0;
  private LynxBaseUI mCurrentDrawUI = mDrawHead;
  private Rect mOverflowClipRect = new Rect();
  private static WeakHashMap<View, Integer> mZIndexHash = new WeakHashMap<>();
  private ViewGroupDrawingOrderHelper mDrawingOrderHelper;
  private boolean mIsInsertViewCalled = false;

  public boolean isInsertViewCalled() {
    return mIsInsertViewCalled;
  }

  public boolean enableAutoClipRadius() {
    return false;
  }

  public UIGroup(final LynxContext context) {
    this(context, null);
  }

  public UIGroup(final LynxContext context, Object param) {
    super(context, param);
  }

  @Override
  public void initialize() {
    super.initialize();
    mDrawingOrderHelper = new ViewGroupDrawingOrderHelper(getView());
    if (mView instanceof IDrawChildHookBinding) {
      ((IDrawChildHookBinding) mView).bindDrawChildHook(this);
    }
  }

  @Override
  public void markDetachWithViewRecursively(boolean detached) {
    if (detached) {
      mViewInfo = new ViewInfo(this, mView);
      mViewInfo.markNeedGenerateMeaningfulPaintingArea(needGenerateMeaningfulPaintingArea());
      if (mView instanceof IDrawChildHookBinding) {
        ((IDrawChildHookBinding) mView).bindDrawChildHook(mViewInfo);
      }
    }
    super.markDetachWithViewRecursively(detached);
  }

  // The following code shares structural similarities with IDrawChildHook implementations
  // Potential code reuse opportunities exist, but deferred to:
  // 1. Prevent coupling with existing logic
  // 2. Simplify code review process
  // Code optimization will be implemented via the following patch.
  @Override
  public void beforeProcessViewInfo(ViewInfo info) {
    super.beforeProcessViewInfo(info);
    if (mDrawingOrderHelper != null) {
      mDrawingOrderHelper.prepareChildDrawingOrder();
      info.setDrawingOrder(mDrawingOrderHelper.getDrawingOrderIndices());
    }
    info.setHasOverlappingRendering(hasOverlappingRendering());
  }

  @Override
  public void beforeDispatchProcessViewInfo(ViewInfo info) {
    super.beforeDispatchProcessViewInfo(info);
    mCurrentDrawUI = mDrawHead;
    mCurrentDrawIndex = 0;
    info.clearSubDrawInfo();
    boolean clipRadius = getClipToRadius()
        || (mContext.getDefaultOverflowVisible() && mOverflow == OVERFLOW_HIDDEN
            && enableAutoClipRadius());
    info.setClipToRadius(clipRadius);
    if (clipRadius) {
      BackgroundDrawable drawable =
          getLynxBackground() != null ? getLynxBackground().getDrawable() : null;
      Path path = drawable != null ? drawable.getInnerClipPathForBorderRadius() : null;
      boolean hasShear = getSkewX() != 0 || getSkewY() != 0;
      if (path != null) {
        info.setClipPathInBeforeDispatchDraw(path);
        info.setClipRectInBeforeDispatchDraw(null);
      } else if (hasShear) {
        info.setClipPathInBeforeDispatchDraw(null);
        info.setClipRectInBeforeDispatchDraw(getClipBounds());
      } else {
        info.setClipPathInBeforeDispatchDraw(null);
        info.setClipRectInBeforeDispatchDraw(null);
      }
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
        && getOverflow() != OVERFLOW_XY) {
      // setClipBounds can not be used prior to API 18, force clip here
      int w = getWidth(), h = getHeight();
      int x = 0, y = 0;
      DisplayMetrics dm = mContext.getScreenMetrics();
      if ((getOverflow() & OVERFLOW_X) != 0) {
        x -= dm.widthPixels;
        w += 2 * dm.widthPixels;
      }
      if ((getOverflow() & OVERFLOW_Y) != 0) {
        y -= dm.heightPixels;
        h += 2 * dm.heightPixels;
      }
      mOverflowClipRect.set(x, y, x + w, y + h);
      info.setOverflowClipRect(mOverflowClipRect);
    } else {
      info.setOverflowClipRect(null);
    }
  }

  @Override
  public void beforeProcessChildViewInfo(ViewInfo info, View child, long drawingTime) {
    super.beforeProcessChildViewInfo(info, child, drawingTime);
    for (LynxBaseUI ui = mCurrentDrawUI; ui != null; ui = ui.mNextDrawUI, mCurrentDrawIndex++) {
      if (!ui.isFlatten()) {
        if (((LynxUI) ui).getView() == child) {
          mCurrentDrawUI = ui.mNextDrawUI;
          ViewInfo.SubDrawInfo subInfo = new ViewInfo.SubDrawInfo(true, ui.getBound(), null, null,
              ((LynxUI<?>) ui).mViewInfo, ((LynxUI<?>) ui).getView());
          subInfo.markNeedGenerateMeaningfulPaintingArea(ui.needGenerateMeaningfulPaintingArea());
          info.addSubDrawInfo(mCurrentDrawIndex, subInfo);
          subInfo.recordSubView(ui, child);
          mCurrentDrawIndex++;
          break;
        }
      } else if (ui.isFlatten()) {
        ViewInfo.SubDrawInfo subDrawInfo =
            new ViewInfo.SubDrawInfo(false, ui.getBound(), null, ui.getLynxBackground());
        subDrawInfo.setFlattenUIInfo(ui.getLeft(), ui.getTop(), ui.getWidth(), ui.getHeight(),
            ui.getPaddingLeft(), ui.getPaddingTop(), ui.getPaddingRight(), ui.getPaddingBottom(),
            ((LynxFlattenUI) ui).getAlpha());
        subDrawInfo.markNeedGenerateMeaningfulPaintingArea(ui.needGenerateMeaningfulPaintingArea());
        tryAddInfoForSubDraw(subDrawInfo, ui);
        info.addSubDrawInfo(mCurrentDrawIndex, subDrawInfo);
      }
    }
  }

  @Override
  public void afterDispatchProcessViewInfo(ViewInfo info) {
    super.afterDispatchProcessViewInfo(info);
    LynxBaseUI ui;
    for (ui = mCurrentDrawUI; ui != null; ui = ui.mNextDrawUI, mCurrentDrawIndex++) {
      if (ui.isFlatten() && !(ui instanceof UIShadowProxy)) {
        ViewInfo.SubDrawInfo subDrawInfo =
            new ViewInfo.SubDrawInfo(false, ui.getBound(), null, ui.getLynxBackground());
        subDrawInfo.setFlattenUIInfo(ui.getLeft(), ui.getTop(), ui.getWidth(), ui.getHeight(),
            ui.getPaddingLeft(), ui.getPaddingTop(), ui.getPaddingRight(), ui.getPaddingBottom(),
            ((LynxFlattenUI) ui).getAlpha());
        subDrawInfo.markNeedGenerateMeaningfulPaintingArea(ui.needGenerateMeaningfulPaintingArea());
        tryAddInfoForSubDraw(subDrawInfo, ui);
        info.addSubDrawInfo(mCurrentDrawIndex, subDrawInfo);
      }
    }
  }

  @Override
  public void processLayoutChildren() {
    performLayoutChildrenUI();
  }

  @Override
  public void processMeasureChildren() {
    performMeasureChildrenUI();
  }

  private void tryAddInfoForSubDraw(ViewInfo.SubDrawInfo subDrawInfo, LynxBaseUI ui) {
    if (ui instanceof FlattenUIImage) {
      FlattenUIImage image = (FlattenUIImage) ui;
      subDrawInfo.setImageManager(image.getLynxImageManagerForViewInfo());
    }
    // We need TextLayout, since we need to support font family, inline image when reuse LynxEngine.
    if (ui instanceof FlattenUIText) {
      FlattenUIText text = (FlattenUIText) ui;
      subDrawInfo.setTextLayout(text.getTextLayout());
      subDrawInfo.setDrawOffset(text.getDrawOffsetLeft(), text.getDrawOffsetTop());
    }
  }

  protected View getRealParentView() {
    return mView;
  }

  public void onInsertChild(LynxBaseUI child, int index) {
    child.setOffsetDescendantRectToLynxView(getOffsetDescendantRectToLynxView());
    mChildren.add(index, child);
    child.setParent(this);
  }

  public void insertChildWhenRebuildView(LynxBaseUI child) {
    if (!(child instanceof LynxUI)) {
      return;
    }

    LynxBaseUI ui = mDrawHead;
    int nonFlattenIndex = 0;

    while (ui != null && child != ui) {
      if (ui instanceof LynxUI) {
        nonFlattenIndex++;
      }
      ui = ui.mNextDrawUI;
    }

    View childView = ((LynxUI<?>) child).getView();
    if (childView.getParent() != null) {
      if (childView.getParent() == mView) {
        childView.requestLayout();
        return;
      }
      ((ViewGroup) childView.getParent()).removeView(childView);
    }

    mView.addView(childView, nonFlattenIndex);
  }

  @Override
  public void insertChild(LynxBaseUI child, int index) {
    // View will be added after insert drawList to ensure the order of view tree.
    onInsertChild(child, index);
    // Subclass not overwrite insertChild or will call insertChild
    mIsInsertViewCalled = true;
  }

  public void insertView(LynxUI child) {
    if (mContext != null && mContext.isFallbackProcess()
        && child.getView().getParent() == getView()) {
      child.getView().requestLayout();
      return;
    }

    int i = -1;
    for (LynxBaseUI ui = mDrawHead; ui != null; ui = ui.mNextDrawUI) {
      if (ui instanceof LynxUI) {
        ++i;
      }
      if (ui == child) {
        break;
      }
    }

    if (child.mView.getParent() != null && child.mView.getParent() instanceof ViewGroup) {
      ((ViewGroup) (child.mView.getParent())).removeView(child.mView);
      onRemoveChildUI(child);
    }

    mView.addView(child.mView, i);
    onAddChildUI(child, i);
  }

  public boolean onRemoveChild(LynxBaseUI child) {
    if (!mChildren.remove(child)) {
      return false;
    }
    child.setParent(null);
    return true;
  }

  public void removeChild(LynxBaseUI child) {
    if (onRemoveChild(child)) {
      removeView(child);
    }
  }

  public void removeView(LynxBaseUI child) {
    if (child instanceof LynxUI) {
      mView.removeView(((LynxUI) child).mView);
      if (child instanceof UIList) {
        mView.removeView(((UIList) child).getContainer());
      }
      onRemoveChildUI((LynxUI) child);
    } else {
      // FlattenUI removed should invalidate
      invalidate();
    }
  }

  public void removeAll() {
    for (LynxBaseUI ui = mDrawHead; ui != null; ui = ui.mNextDrawUI) {
      ui.setDrawParent(null);
    }
    mDrawHead = null;

    for (LynxBaseUI child : mChildren) {
      child.setParent(null);
    }
    mChildren.clear();
    if (mView != null) {
      mView.removeAllViews();
    }
  }

  public void measureChildren() {
    for (LynxBaseUI child : mChildren) {
      child.measure();
    }
  }

  public void layoutChildren() {
    for (int index = 0; index < mChildren.size(); index++) {
      LynxBaseUI child = mChildren.get(index);
      if (!needCustomLayout()) {
        if (!child.isFlatten()) {
          ((LynxUI) child).layout();
        } else {
          ((LynxFlattenUI) child).layout(child.getOriginLeft(), child.getOriginTop(), null);
        }
      } else if (child instanceof UIGroup) {
        ((UIGroup) child).layoutChildren();
      }
    }
  }

  @Override
  public void measure() {
    if (!mView.isLayoutRequested()) {
      return;
    }
    measureChildren();
    super.measure();
  }

  @Override
  public void layout() {
    if (!mView.isLayoutRequested()) {
      return;
    }
    super.layout();
    layoutChildren();
  }
  
  /**
   * 在Android开始绘制所有子View之前调用的钩子方法
   * 这是三阶段绘制流程的第一阶段：准备阶段
   * 调用时机：UIBodyView.dispatchDraw() → beforeDispatchDraw()
   *
   * 核心作用：
   * 1. 重置绘制链表遍历指针，准备开始新一轮绘制
   * 2. 设置裁剪区域，处理圆角、溢出隐藏等样式
   * 3. 为混合渲染做好准备
   */
  @Override
  public void beforeDispatchDraw(final Canvas canvas) {
    // ========== 关键步骤1：重置绘制链表遍历状态 ==========
    // mCurrentDrawUI：当前遍历位置指针，指向绘制链表的当前节点
    // mDrawHead：绘制链表的头节点，包含所有需要绘制的UI（扁平+非扁平）
    // 每次绘制前重置指针，确保从链表头部开始遍历
    mCurrentDrawUI = mDrawHead;

    // mCurrentDrawIndex：当前绘制索引（用于调试或特殊场景）
    mCurrentDrawIndex = 0;

    // 检查是否有倾斜变换（skew/shear），影响裁剪区域计算
    boolean hasShear = getSkewX() != 0 || getSkewY() != 0;

    // ========== 关键步骤2：处理圆角裁剪 ==========
    // 条件：需要裁剪到圆角 或 （默认溢出可见且当前溢出隐藏且启用自动圆角裁剪）
    if (getClipToRadius()
        || (mContext.getDefaultOverflowVisible() && mOverflow == OVERFLOW_HIDDEN
            && enableAutoClipRadius())) {
      // 获取背景Drawable，用于获取圆角裁剪路径
      Drawable drawable = getLynxBackground() != null ? getLynxBackground().getDrawable() : null;
      if (drawable != null && drawable instanceof BackgroundDrawable) {
        // 从背景Drawable获取内部圆角裁剪路径
        Path path = ((BackgroundDrawable) drawable).getInnerClipPathForBorderRadius();
        if (path != null) {
          // 应用圆角裁剪路径到Canvas
          canvas.clipPath(path);
        } else if (hasShear) {
          // 如果有倾斜变换但没有圆角路径，使用矩形裁剪边界
          // 原因：倾斜变换需要手动处理裁剪边界
          canvas.clipRect(getClipBounds());
        }
      }
    }

    // ========== 关键步骤3：处理低版本Android的溢出裁剪 ==========
    // Android API 18（JELLY_BEAN_MR2）之前，setClipBounds不可用，需要手动裁剪
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
        && getOverflow() != OVERFLOW_XY) {
      // setClipBounds can not be used prior to API 18, force clip here
      int w = getWidth(), h = getHeight();
      int x = 0, y = 0;
      DisplayMetrics dm = mContext.getScreenMetrics();

      // 处理X轴溢出：扩展裁剪区域到屏幕宽度
      if ((getOverflow() & OVERFLOW_X) != 0) {
        x -= dm.widthPixels;          // 向左扩展一个屏幕宽度
        w += 2 * dm.widthPixels;      // 宽度增加两个屏幕宽度
      }

      // 处理Y轴溢出：扩展裁剪区域到屏幕高度
      if ((getOverflow() & OVERFLOW_Y) != 0) {
        y -= dm.heightPixels;         // 向上扩展一个屏幕高度
        h += 2 * dm.heightPixels;     // 高度增加两个屏幕高度
      }

      // 设置溢出裁剪矩形并应用到Canvas
      mOverflowClipRect.set(x, y, x + w, y + h);
      canvas.clipRect(mOverflowClipRect);
    }
  }

  /**
   * 在Android完成所有子View绘制之后调用的钩子方法
   * 这是三阶段绘制流程的第三阶段：收尾阶段
   * 调用时机：UIBodyView.dispatchDraw() → super.dispatchDraw()完成 → afterDispatchDraw()
   *
   * 核心作用：
   * 1. 绘制所有在Android子View之后出现的扁平UI
   * 2. 完成绘制链表的剩余遍历
   * 3. 确保所有扁平UI都被正确绘制
   *
   * 重要说明：
   * 大部分扁平UI的绘制发生在 beforeDrawChild() 钩子中（第二阶段），
   * 只有那些在所有Android子View之后出现的扁平UI才在这里绘制。
   * 
   * 
   * 核心是这个....., 两个语义不一样的..
   *   - afterDrawChild()：每个子View绘制后立即调用 ---> 用这个的话,你不知道后面还有其他子View
   *   - afterDispatchDraw()：所有子View绘制完成后调用
   *   
   *   
   *   画图画图画图...
   */
  @Override
  public void afterDispatchDraw(final Canvas canvas) {
    LynxBaseUI ui;

    // ========== 关键步骤：遍历绘制链表的剩余部分 ==========
    // 注意：这里使用 for 循环而不是递归，因为绘制链表已经是线性DFS顺序
    // mCurrentDrawUI：当前遍历位置指针，经过 beforeDrawChild() 多次调用后，
    //               指向第一个在所有Android子View之后出现的节点

    // 遍历从 mCurrentDrawUI 开始到链表末尾的所有节点
    for (ui = mCurrentDrawUI; ui != null; ui = ui.mNextDrawUI) {
      // 条件：节点是扁平UI 且 不是阴影代理（UIShadowProxy）
      // 扁平UI需要手动绘制，非扁平UI由Android系统自动绘制（已在第二阶段完成）
      // TODO 为什么需要判断这个UIShadowProxy呢??他有什么作用.
      if (ui.isFlatten() && !(ui instanceof UIShadowProxy)) {
        // 调用 drawChild() 方法手动绘制扁平UI
        // 这是 Lynx 半自绘框架的核心：手动绘制没有独立Android View的UI
        drawChild((LynxFlattenUI) ui, canvas);
      }

      // 注意：这里没有 else 分支处理非扁平UI，因为：
      // 1. 非扁平UI（有独立Android View）已经在第二阶段由Android系统绘制
      // 2. 如果还有非扁平UI在这里，说明它应该在某个Android子View之前出现，
      //    但 beforeDrawChild() 没有找到它，这可能是bug
    }

    // 遍历完成后，mCurrentDrawUI 应该为 null
    // 如果不是null，说明链表遍历没有完成，但循环已结束，可能是链表结构问题
  }

  @Override
  public void afterDraw(Canvas canvas) {}

  /**
   * 在开始任何绘制之前调用的钩子方法
   * 调用时机：在 dispatchDraw() 的最开始，beforeDispatchDraw() 之前
   *
   * 核心作用：
   * 1. 应用倾斜变换（skew/shear）到Canvas
   * 2. 应用自定义裁剪路径
   * 3. 这些变换会影响后续所有绘制操作
   */
  @Override
  public void beforeDraw(Canvas canvas) {
    // ========== 关键步骤1：处理倾斜变换 ==========
    // 倾斜变换（skew）是一种2D图形变换，使图形在某个方向上倾斜
    if (getSkewX() != 0 || getSkewY() != 0) {
      // 应用倾斜变换到Canvas
      // skewX: X轴倾斜角度（弧度）的正切值
      // skewY: Y轴倾斜角度（弧度）的正切值
      canvas.skew(getSkewX(), getSkewY());

      // 将锚点移回原位置（修正变换）
      // 倾斜变换公式：x' = x + y * tan(skewX), y' = y + x * tan(skewY)
      // 由于Canvas的skew()是以原点(0,0)为锚点，但UI可能有自己的旋转中心（pivot）
      // 这里通过平移将锚点移回UI的旋转中心
      canvas.translate(-mView.getPivotY() * getSkewX(), -mView.getPivotX() * getSkewY());
    }

    // ========== 关键步骤2：处理自定义裁剪路径 ==========
    // mClipPath：自定义裁剪路径，可以是非矩形的复杂形状
    if (mClipPath != null) {
      // 根据当前UI的宽高获取裁剪路径
      Path path = mClipPath.getPath(getWidth(), getHeight());
      if (path != null) {
        // 应用裁剪路径到Canvas
        // 后续所有绘制都会限制在这个路径范围内
        canvas.clipPath(path);
      }
    }
  }

  /**
   * 核心方法：在绘制Android子View之前，绘制需要在该子View之前显示的拍平UI
   *
   * 作用：协调Android原生View与Lynx拍平UI的混合绘制顺序
   * 原理：遍历绘制链表，绘制在目标子View之前的所有拍平UI
   *
   * @param canvas 绘制画布
   * @param child Android将要绘制的子View
   * @param drawingTime 绘制时间
   * @return 子View的边界矩形（用于裁剪等）
   */
  private Rect drawFlattenUIBefore(final Canvas canvas, final View child, final long drawingTime) {
    Rect bound = null;

    // 关键：遍历绘制链表（mCurrentDrawUI是当前遍历位置）
    // 绘制链表包含了所有需要绘制的UI节点（拍平UI + 某些非拍平UI）
    // 链表顺序 = Android DFS绘制顺序
    for (LynxBaseUI ui = mCurrentDrawUI; ui != null; ui = ui.mNextDrawUI) {
      if (!ui.isFlatten()) {
        // 情况1：当前节点是非拍平UI（LynxUI，有Android View）
        // 检查这个非拍平UI的Android View是否就是参数child
        if (((LynxUI) ui).getView() == child) {
          // 找到匹配的节点！这意味着：
          // 1. 在这个子View之前的所有拍平UI已经绘制完成
          // 2. 现在应该绘制这个子View（由Android负责）

          // 获取子View的边界矩形（用于后续的裁剪等操作）
          bound = ui.getBound();

          // 关键：更新当前遍历位置到下一个节点
          // 这样下次调用时，会从正确的位置继续遍历
          mCurrentDrawUI = ui.mNextDrawUI;

          // 跳出循环，让Android绘制这个子View
          break;
        }
        // 如果不是目标子View，继续遍历（这种情况应该不会发生）
      } else if (ui.isFlatten()) {
        // 情况2：当前节点是拍平UI（LynxFlattenUI，没有Android View）
        // 需要在Android绘制子View之前，先绘制这个拍平UI
        // 这样才能保证拍平UI在正确的Z轴顺序上

        // 调用drawChild方法绘制拍平UI
        drawChild((LynxFlattenUI) ui, canvas);

        // 继续遍历链表，绘制下一个拍平UI
        // 直到找到目标子View为止
      }
    }
    return bound;
  }

  /**
   * Android系统在绘制每个子View之前调用的钩子方法
   * 这是混合渲染的核心：在Android绘制原生View之前，先绘制应该出现在它前面的扁平UI
   *
   * 调用时机：Android ViewGroup.dispatchDraw() → 对每个子View → beforeDrawChild()
   *
   * 核心作用：
   * 1. 遍历绘制链表，绘制所有在目标子View之前出现的扁平UI
   * 2. 找到目标子View在链表中的对应节点
   * 3. 更新遍历指针，为下一个子View的绘制做好准备
   *
   * @param canvas 绘制画布
   * @param child Android将要绘制的子View
   * @param drawingTime 绘制时间戳
   * @return 子View的边界矩形（用于裁剪等优化）
   */
  @Override
  public Rect beforeDrawChild(final Canvas canvas, final View child, final long drawingTime) {
    // 调用核心绘制逻辑：绘制在child之前的所有扁平UI
    Rect bound = drawFlattenUIBefore(canvas, child, drawingTime);
    return bound;
  }

  /**
   * Android系统在绘制每个子View之后调用的钩子方法
   * 理论上可以在这里绘制在子View之后出现的扁平UI，
   * 但Lynx的设计选择在 beforeDrawChild() 中一次性绘制所有前面的扁平UI，
   * 所以这个方法通常是空的。
   *
   * 设计选择原因：
   * 1. 简化逻辑：所有扁平UI都在 beforeDrawChild() 中按链表顺序绘制
   * 2. 性能考虑：减少方法调用和状态切换
   * 3. 顺序保证：链表已经维护了正确的DFS顺序
   * 
   * 
   * 不在afterDrawChild中绘制，而是在afterDispatchDraw中绘制剩余虚拟节点
   * 
   * 
   * 
   * 
   * 为什么 Lynx 选择 afterDispatchDraw() 而不是 afterDrawChild()？
   *
   *   1. 状态简单：只需要一个指针 mCurrentDrawUI
   *   2. 语义明确："在所有子View之后" vs "在这个子View之后"
   *   3. 避免过早绘制：不会错误绘制应该在后面子View之前的扁平UI
   *   4. 性能优化：批量绘制，减少状态切换
   *   5. 实现简洁：代码更简单，维护更容易
   *   
   *     你的直觉是正确的：从单个View的角度，afterDrawChild 更符合语义。但从系统整体设计的角度，afterDispatchDraw 更合适，因为它：
   *   - 避免了绘制时机的不确定性
   *   - 简化了状态管理
   *   - 保证了绘制顺序的正确性
   *
   *   这是一个典型的工程权衡：牺牲局部的语义清晰度，换取整体的简单性和正确性。🎯
   *
   *
   * 
   * 如果..
   *   如果要在 afterDrawChild 中实现
   *
   *   理论上可以实现，但需要更复杂的状态管理：
   *
   *   // 复杂实现：在 afterDrawChild 中绘制
   *   public void afterDrawChild(Canvas canvas, View child, long drawingTime) {
   *       // 需要知道：这是否是最后一个子View？
   *       boolean isLastChild = (getChildIndex(child) == getChildCount() - 1);
   *
   *       if (isLastChild) {
   *           // 最后一个子View：绘制所有剩余扁平UI
   *           drawAllRemainingFlattenUI();
   *       } else {
   *           // 不是最后一个：绘制直到下一个Android子View之前的扁平UI
   *           View nextChild = getChildAt(getChildIndex(child) + 1);
   *           drawUntilNextAndroidView(nextChild);
   *       }
   *   }
   *
   *   复杂度：
   *   - 需要维护子View索引
   *   - 需要查找下一个子View
   *   - 边界条件复杂
   *   
   *   
   *    为什么 Lynx 选择 afterDispatchDraw() 而不是 afterDrawChild()？
   *
   *   1. 状态简单：只需要一个指针 mCurrentDrawUI
   *   2. 语义明确："在所有子View之后" vs "在这个子View之后"
   *   3. 避免过早绘制：不会错误绘制应该在后面子View之前的扁平UI
   *   4. 性能优化：批量绘制，减少状态切换
   *   5. 实现简洁：代码更简单，维护更容易
   * @param canvas 绘制画布
   * @param child Android刚刚绘制完成的子View
   * @param drawingTime 绘制时间戳
   *                    
   *                    
   *                    
   *                    这才是核心：：
   *                    afterDrawChild() 是在每个子View绘制后调用。。。。 如果从这里开始直接绘制剩下的View，你都不知道当前父VIewGroup里面，是否已经绘制完成了Android系统View。
   */
  @Override
  public void afterDrawChild(final Canvas canvas, final View child, final long drawingTime) {
    // 空实现：Lynx选择在 beforeDrawChild() 中处理所有扁平UI绘制
    // 这样可以简化逻辑，所有绘制都按链表顺序一次性处理

//    for (LynxBaseUI ui = mCurrentDrawUI; ui != null; ui = ui.mNextDrawUI) {
//      // 条件：节点是扁平UI 且 不是阴影代理（UIShadowProxy）
//      // 扁平UI需要手动绘制，非扁平UI由Android系统自动绘制（已在第二阶段完成）
//      // TODO 为什么需要判断这个UIShadowProxy呢??他有什么作用.
//      if (ui.isFlatten() && !(ui instanceof UIShadowProxy)) {
//        // 调用 drawChild() 方法手动绘制扁平UI
//        // 这是 Lynx 半自绘框架的核心：手动绘制没有独立Android View的UI
//        drawChild((LynxFlattenUI) ui, canvas);
//      }
//
//      // 注意：这里没有 else 分支处理非扁平UI，因为：
//      // 1. 非扁平UI（有独立Android View）已经在第二阶段由Android系统绘制
//      // 2. 如果还有非扁平UI在这里，说明它应该在某个Android子View之前出现，
//      //    但 beforeDrawChild() 没有找到它，这可能是bug
//    }
  }

  @Override
  public int getChildDrawingOrder(int childCount, int index) {
    if (mDrawingOrderHelper != null) {
      return mDrawingOrderHelper.getChildDrawingOrder(childCount, index);
    }
    return index;
  }

  @Override
  public boolean hasOverlappingRendering() {
    return hasOverlappingRenderingEnabled();
  }

  @Override
  public void performLayoutChildrenUI() {
    layoutChildren();
  }

  @Override
  public void performMeasureChildrenUI() {
    measureChildren();
  }

  /**
   * 手动绘制扁平UI（LynxFlattenUI）的核心方法
   * 这是Lynx半自绘框架的关键：手动绘制没有独立Android View的UI
   *
   * 调用时机：
   * 1. beforeDrawChild() → drawFlattenUIBefore() → drawChild() （主要路径）
   * 2. afterDispatchDraw() → drawChild() （收尾路径）
   *
   * 核心作用：
   * 1. 保存Canvas状态（save）
   * 2. 应用裁剪区域（clipRect）
   * 3. 调用扁平UI自身的绘制逻辑（innerDraw）
   * 4. 恢复Canvas状态（restore）
   *
   * 设计要点：
   * 1. 使用 save()/restore() 保证绘制不影响其他UI
   * 2. 裁剪确保绘制不超出边界
   * 3. 委托给 child.innerDraw() 实现具体绘制
   *
   * @param child 要绘制的扁平UI
   * @param canvas 绘制画布
   */
  protected void drawChild(LynxFlattenUI child, Canvas canvas) {
    // 获取扁平UI的边界矩形（位置和大小）
    Rect bound = child.getBound();

    // ========== 步骤1：保存Canvas状态 ==========
    // 保存当前Canvas的所有状态（变换矩阵、裁剪区域等）
    // 确保当前绘制不影响后续绘制
    canvas.save();

    // ========== 步骤2：应用裁剪区域 ==========
    // 如果扁平UI有明确的边界，裁剪到该边界
    // 防止绘制超出UI范围（重要！避免重叠、溢出等问题）
    if (bound != null) {
      canvas.clipRect(bound); // 处理裁剪
    }

    // ========== 步骤3：调用扁平UI自身的绘制逻辑 ==========
    // 委托给 LynxFlattenUI.innerDraw() 方法
    // 这是模板方法模式：父类定义流程，子类实现具体绘制
    child.innerDraw(canvas);

    // ========== 步骤4：恢复Canvas状态 ==========
    // 恢复之前保存的Canvas状态
    // 确保后续绘制不受当前UI的影响
    canvas.restore();
  }

  @Override
  public void destroy() {
    super.destroy();
    for (LynxBaseUI ui : mChildren) {
      ui.destroy();
    }
  }

  @Override
  public void onAttach() {
    super.onAttach();
    dispatchOnAttach();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    dispatchOnDetach();
  }

  public void dispatchOnAttach() {
    for (LynxBaseUI ui : mChildren) {
      ui.onAttach();
    }
  }

  public void dispatchOnDetach() {
    for (LynxBaseUI ui : mChildren) {
      ui.onDetach();
    }
  }

  public int getIndex(LynxBaseUI child) {
    return mChildren.indexOf(child);
  }

  public int getChildCount() {
    return mChildren.size();
  }

  @Override
  public LynxBaseUI getChildAt(int index) {
    return mChildren.get(index);
  }

  public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams childParams) {
    return null;
  }

  public boolean needCustomLayout() {
    return false;
  }

  public EventTarget findUIWithCustomLayout(float x, float y, UIGroup parent) {
    Map<View, LynxUI> children = new HashMap<>();
    for (int i = parent.getChildCount() - 1; i >= 0; i--) {
      LynxBaseUI child = parent.getChildAt(i);
      if (child instanceof UIShadowProxy) {
        child = ((UIShadowProxy) child).getChild();
      }
      if (child instanceof LynxUI) {
        children.put(((LynxUI) child).getView(), (LynxUI) child);
      } else {
        LLog.DTHROW(
            new RuntimeException("ui that need custom layout should not have flatten child!"));
      }
    }
    return findUIWithCustomLayoutByChildren(x, y, parent, children);
  }

  protected EventTarget findUIWithCustomLayoutByChildren(
      float x, float y, UIGroup parent, Map<View, LynxUI> children) {
    float[] eventCoords = new float[] {x, y};
    // eventCoords will be transformed to descendant's coordinate system
    LynxUI touchTarget =
        findTouchTargetOnViewChian(eventCoords, (ViewGroup) parent.getView(), children);

    if (touchTarget == null) {
      return parent;
    }

    if (touchTarget.needCustomLayout() && touchTarget instanceof UIGroup) {
      return ((UIGroup) touchTarget)
          .findUIWithCustomLayout(eventCoords[0], eventCoords[1], (UIGroup) touchTarget);
    }
    if (mContext.getEnableEventRefactor()) {
      return touchTarget.hitTest(eventCoords[0], eventCoords[1]);
    }
    return touchTarget.hitTest(
        eventCoords[0] + touchTarget.getScrollX(), eventCoords[1] + touchTarget.getScrollY());
  }

  private LynxUI findTouchTargetOnViewChian(
      float[] eventCoords, ViewGroup viewGroup, Map<View, LynxUI> relations) {
    LynxUI touchTarget = null;
    int childrenCount = viewGroup.getChildCount();
    for (int i = childrenCount - 1; i >= 0; i--) {
      View child = viewGroup.getChildAt(i);
      if (mContext.getEnableEventRefactor()) {
        float[] childPoint = new float[2];
        if (isTransformedTouchPointInView(eventCoords, viewGroup, child, childPoint, relations)) {
          if (relations.containsKey(child)) {
            touchTarget = relations.get(child);
            eventCoords[0] = childPoint[0];
            eventCoords[1] = childPoint[1];
          } else if (child instanceof ViewGroup) {
            touchTarget = findTouchTargetOnViewChian(childPoint, (ViewGroup) child, relations);
            if (touchTarget != null) {
              eventCoords[0] = childPoint[0];
              eventCoords[1] = childPoint[1];
            }
          }
          if (touchTarget == null) {
            continue;
          }
          return touchTarget;
        }
        continue;
      }

      PointF childPoint = mTempPoint;
      if (isTransformedTouchPointInView(
              eventCoords[0], eventCoords[1], viewGroup, child, childPoint)) {
        float prex = eventCoords[0];
        float prey = eventCoords[1];
        eventCoords[0] = childPoint.x;
        eventCoords[1] = childPoint.y;
        if (relations.containsKey(child)) {
          touchTarget = relations.get(child);
        } else if (child instanceof ViewGroup) {
          touchTarget = findTouchTargetOnViewChian(eventCoords, (ViewGroup) child, relations);
        }
        if (touchTarget == null) {
          eventCoords[0] = prex;
          eventCoords[1] = prey;
          continue;
        }
        return touchTarget;
      }
    }
    return touchTarget;
  }

  private static final float[] mEventCoords = new float[2];
  private static final PointF mTempPoint = new PointF();
  private static final float[] mMatrixTransformCoords = new float[2];
  private static final Matrix mInverseMatrix = new Matrix();

  protected boolean isTransformedTouchPointInView(float[] inPoint, View parent, View child,
      float[] outLocalPoint, Map<View, LynxUI> relations) {
    float[] point = getTargetPoint(
        inPoint[0], inPoint[1], parent.getScrollX(), parent.getScrollY(), child, child.getMatrix());
    outLocalPoint[0] = point[0];
    outLocalPoint[1] = point[1];
    if ((outLocalPoint[0] >= 0 && outLocalPoint[0] < (child.getRight() - child.getLeft()))
        && (outLocalPoint[1] >= 0 && outLocalPoint[1] < (child.getBottom() - child.getTop()))) {
      return true;
    }
    return false;
  }

  private boolean isTransformedTouchPointInView(
      float x, float y, ViewGroup parent, View child, PointF outLocalPoint) {
    float localX = x + parent.getScrollX() - child.getLeft();
    float localY = y + parent.getScrollY() - child.getTop();
    Matrix matrix = child.getMatrix();
    if (!matrix.isIdentity()) {
      float[] localXY = mMatrixTransformCoords;
      localXY[0] = localX;
      localXY[1] = localY;
      Matrix inverseMatrix = mInverseMatrix;
      matrix.invert(inverseMatrix);
      inverseMatrix.mapPoints(localXY);
      localX = localXY[0];
      localY = localXY[1];
    }
    if ((localX >= 0 && localX < (child.getRight() - child.getLeft()))
        && (localY >= 0 && localY < (child.getBottom() - child.getTop()))) {
      outLocalPoint.set(localX, localY);
      return true;
    }

    return false;
  }

  private void onAddChildUI(LynxUI child, int index) {
    if (!ENABLE_ZINDEX) {
      return;
    }
    mDrawingOrderHelper.handleAddView(child.getView());
    setChildrenDrawingOrderEnabledHelper(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
  }

  private void onRemoveChildUI(LynxUI child) {
    if (!ENABLE_ZINDEX) {
      return;
    }
    mDrawingOrderHelper.handleRemoveView(child.getView());
    setChildrenDrawingOrderEnabledHelper(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
  }

  public static void setViewZIndex(View view, int zIndex) {
    mZIndexHash.put(view, zIndex);
  }

  public static @Nullable Integer getViewZIndex(View view) {
    return mZIndexHash.get(view);
  }

  public void updateDrawingOrder() {
    mDrawingOrderHelper.update();
    setChildrenDrawingOrderEnabledHelper(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
    invalidate();
  }

  public View getAccessibilityHostView() {
    return mView;
  }

  private void setChildrenDrawingOrderEnabledHelper(boolean enable) {
    if (mView instanceof AndroidView) {
      ((AndroidView) mView).setChildrenDrawingOrderEnabled(enable);
    } else if (mView instanceof UIBody.UIBodyView) {
      ((UIBody.UIBodyView) mView).setChildrenDrawingOrderEnabled(enable);
    }
  }

  @Override
  public void setTranslationZ(float zValue) {
    super.setTranslationZ(zValue);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && zValue != getLastTranslateZ()) {
      setLastTranslateZ(zValue);
      setViewZIndex(mView, Math.round(zValue));
      UIParent parent = getParent();
      if (parent instanceof UIGroup) {
        ((UIGroup) parent).updateDrawingOrder();
      }
    }
  }
}

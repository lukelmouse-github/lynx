// Copyright 2023 The Lynx Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.lynx.tasm.behavior.ui;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;


/**
 * 绘制子View的hook, 作用是在Android原生绘制流程中插入Lynx的绘制逻辑
 * 
 * 根ViewGroup（UIBodyView）持有钩子对象
 */
public interface IDrawChildHook {
  interface IDrawChildHookBinding {
    void bindDrawChildHook(IDrawChildHook hook);
  }
  
  // View自身的hook. ---> 没有什么作用, 不用来绘制其他虚拟节点, 只是用来应用Canvas变换和裁剪
  void beforeDraw(Canvas canvas);
  void afterDraw(Canvas canvas);
  
  
  // ViewGroup 之前, beforeDispatchDraw()：准备阶段 - 重置状态、设置裁剪、为混合渲染做准备
  void beforeDispatchDraw(Canvas canvas);
  // ViewGroup 之后, afterDispatchDraw()：收尾阶段 - 绘制所有在 Android 子 View 之后出现的扁平 UI
  void afterDispatchDraw(Canvas canvas);
  
  // 每个 Android 子 View 绘制前   | 绘制在该子 View 之前的扁平 UI  | 核心实现：drawFlattenUIBefore()      |
  Rect beforeDrawChild(Canvas canvas, View child, long drawingTime);
  // 每个 Android 子 View 绘制后   | 绘制在该子 View 之后的扁平 UI  | 空实现：选择在 afterDispatchDraw() 中处理
  void afterDrawChild(Canvas canvas, View child, long drawingTime);

  
  int getChildDrawingOrder(int childCount, int index);

  boolean hasOverlappingRendering();

  void performLayoutChildrenUI();

  void performMeasureChildrenUI();
}

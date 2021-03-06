/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.stubs;

import consulo.util.dataholder.Key;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectObjectProcedure;
import gnu.trove.TObjectProcedure;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class ObjectStubTree<T extends Stub> {
  private static final Key<ObjectStubTree> STUB_TO_TREE_REFERENCE = Key.create("stub to tree reference");
  protected final ObjectStubBase myRoot;
  private String myDebugInfo;
  private final List<T> myPlainList;

  public ObjectStubTree(@Nonnull final ObjectStubBase root, final boolean withBackReference) {
    myRoot = root;
    myPlainList = enumerateStubs(root);
    if (withBackReference) {
      myRoot.putUserData(STUB_TO_TREE_REFERENCE, this); // This will prevent soft references to stub tree to be collected before all of the stubs are collected.
    }
  }

  @Nonnull
  public Stub getRoot() {
    return myRoot;
  }

  @Nonnull
  public List<T> getPlainList() {
    return myPlainList;
  }

  @Nonnull
  List<T> getPlainListFromAllRoots() {
    return getPlainList();
  }

  @Deprecated
  @Nonnull
  public Map<StubIndexKey, Map<Object, int[]>> indexStubTree() {
    return indexStubTree(key -> TObjectHashingStrategy.CANONICAL);
  }

  @Nonnull
  public Map<StubIndexKey, Map<Object, int[]>> indexStubTree(@Nonnull Function<StubIndexKey<?, ?>, TObjectHashingStrategy<?>> keyHashingStrategyFunction) {
    StubIndexSink sink = new StubIndexSink(keyHashingStrategyFunction);
    final List<T> plainList = getPlainListFromAllRoots();
    for (int i = 0, plainListSize = plainList.size(); i < plainListSize; i++) {
      final Stub stub = plainList.get(i);
      sink.myStubIdx = i;
      StubSerializationUtil.getSerializer(stub).indexStub(stub, sink);
    }

    return sink.getResult();
  }

  @Nonnull
  protected List<T> enumerateStubs(@Nonnull Stub root) {
    List<T> result = new ArrayList<>();
    //noinspection unchecked
    enumerateStubsInto(root, (List)result);
    return result;
  }

  private static void enumerateStubsInto(@Nonnull Stub root, @Nonnull List<? super Stub> result) {
    ((ObjectStubBase)root).id = result.size();
    result.add(root);
    List<? extends Stub> childrenStubs = root.getChildrenStubs();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < childrenStubs.size(); i++) {
      Stub child = childrenStubs.get(i);
      enumerateStubsInto(child, result);
    }
  }

  public void setDebugInfo(@Nonnull String info) {
    ObjectStubTree ref = getStubTree(myRoot);
    if (ref != null) {
      assert ref == this;
      info += "; with backReference";
    }
    myDebugInfo = info;
  }

  @Nullable
  public static ObjectStubTree getStubTree(@Nonnull ObjectStubBase root) {
    return root.getUserData(STUB_TO_TREE_REFERENCE);
  }

  public String getDebugInfo() {
    return myDebugInfo;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{myDebugInfo='" + myDebugInfo + '\'' + ", myRoot=" + myRoot + '}' + hashCode();
  }

  private static class StubIndexSink implements IndexSink, TObjectProcedure<Map<Object, int[]>>, TObjectObjectProcedure<Object, int[]> {
    private final THashMap<StubIndexKey, Map<Object, int[]>> myResult = new THashMap<>();
    private final Function<StubIndexKey<?, ?>, TObjectHashingStrategy<?>> myHashingStrategyFunction;
    private int myStubIdx;
    private Map<Object, int[]> myProcessingMap;

    private StubIndexSink(@Nonnull Function<StubIndexKey<?, ?>, TObjectHashingStrategy<?>> hashingStrategyFunction) {
      myHashingStrategyFunction = hashingStrategyFunction;
    }

    @Override
    public void occurrence(@Nonnull final StubIndexKey indexKey, @Nonnull final Object value) {
      Map<Object, int[]> map = myResult.get(indexKey);
      if (map == null) {
        map = new THashMap<>((TObjectHashingStrategy<Object>)myHashingStrategyFunction.fun(indexKey));
        myResult.put(indexKey, map);
      }

      int[] list = map.get(value);
      if (list == null) {
        map.put(value, new int[]{myStubIdx});
      }
      else {
        int lastNonZero = ArrayUtil.lastIndexOfNot(list, 0);
        if (lastNonZero >= 0 && list[lastNonZero] == myStubIdx) {
          // second and subsequent occurrence calls for the same value are no op
          return;
        }
        int lastZero = lastNonZero + 1;

        if (lastZero == list.length) {
          list = ArrayUtil.realloc(list, Math.max(4, list.length << 1));
          map.put(value, list);
        }
        list[lastZero] = myStubIdx;
      }
    }

    @Nonnull
    public Map<StubIndexKey, Map<Object, int[]>> getResult() {
      myResult.forEachValue(this);
      return myResult;
    }

    @Override
    public boolean execute(Map<Object, int[]> object) {
      myProcessingMap = object;
      ((THashMap<Object, int[]>)object).forEachEntry(this);
      return true;
    }

    @Override
    public boolean execute(Object a, int[] b) {
      if (b.length == 1) return true;
      int firstZero = ArrayUtil.indexOf(b, 0);
      if (firstZero != -1) {
        int[] shorterList = ArrayUtil.realloc(b, firstZero);
        myProcessingMap.put(a, shorterList);
      }
      return true;
    }
  }
}

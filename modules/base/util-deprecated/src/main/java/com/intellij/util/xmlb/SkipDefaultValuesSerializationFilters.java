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
package com.intellij.util.xmlb;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.ReflectionUtil;
import gnu.trove.THashMap;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

public class SkipDefaultValuesSerializationFilters extends SerializationFilterBase {
  private final Map<Class<?>, Object> myDefaultBeans = new THashMap<Class<?>, Object>();

  @Override
  protected boolean accepts(@Nonnull Accessor accessor, @Nonnull Object bean, @Nullable Object beanValue) {
    Object defValue = accessor.read(getDefaultBean(bean));
    if (defValue instanceof Element && beanValue instanceof Element) {
      return !JDOMUtil.areElementsEqual((Element)beanValue, (Element)defValue);
    }
    else {
      return !Comparing.equal(beanValue, defValue);
    }
  }

  @Nonnull
  Object getDefaultBean(@Nonnull Object bean) {
    Class<?> c = bean.getClass();
    Object o = myDefaultBeans.get(c);
    if (o == null) {
      o = ReflectionUtil.newInstance(c);
      configure(o);

      myDefaultBeans.put(c, o);
    }
    return o;
  }

  /**
   * Override to put your own default object configuration
   */
  protected void configure(@Nonnull Object o) {
  }
}

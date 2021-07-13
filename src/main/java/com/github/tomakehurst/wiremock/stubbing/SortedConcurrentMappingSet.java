/*
 * Copyright (C) 2011 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.stubbing;

import static com.google.common.collect.Iterables.removeIf;

import com.google.common.base.Predicate;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public class SortedConcurrentMappingSet implements Iterable<StubMapping> {

  private AtomicLong insertionCount;
  private ConcurrentSkipListSet<StubMapping> mappingSet;

  public SortedConcurrentMappingSet() {
    insertionCount = new AtomicLong();
    mappingSet =
        new ConcurrentSkipListSet<StubMapping>(sortedByPriorityThenReverseInsertionOrder());
  }

  private Comparator<StubMapping> sortedByPriorityThenReverseInsertionOrder() {
    return new Comparator<StubMapping>() {
      public int compare(StubMapping one, StubMapping two) {
        int priorityComparison = one.comparePriorityWith(two);
        if (priorityComparison != 0) {
          return priorityComparison;
        }

        return Long.compare(two.getInsertionIndex(), one.getInsertionIndex());
      }
    };
  }

  @Override
  public Iterator<StubMapping> iterator() {
    return mappingSet.iterator();
  }

  public void add(StubMapping mapping) {
    mapping.setInsertionIndex(insertionCount.getAndIncrement());
    mappingSet.add(mapping);
  }

  public boolean remove(final StubMapping mappingToRemove) {
    boolean removedByUuid =
        removeIf(
            mappingSet,
            new Predicate<StubMapping>() {
              @Override
              public boolean apply(StubMapping mapping) {
                return mappingToRemove.getUuid() != null
                    && mapping.getUuid() != null
                    && mappingToRemove.getUuid().equals(mapping.getUuid());
              }
            });

    boolean removedByRequestPattern =
        !removedByUuid
            && removeIf(
                mappingSet,
                new Predicate<StubMapping>() {
                  @Override
                  public boolean apply(StubMapping mapping) {
                    return mappingToRemove.getRequest().equals(mapping.getRequest());
                  }
                });

    return removedByUuid || removedByRequestPattern;
  }

  public boolean replace(StubMapping existingStubMapping, StubMapping newStubMapping) {

    if (mappingSet.remove(existingStubMapping)) {
      mappingSet.add(newStubMapping);
      return true;
    }
    return false;
  }

  public void clear() {
    mappingSet.clear();
  }

  @Override
  public String toString() {
    return mappingSet.toString();
  }
}

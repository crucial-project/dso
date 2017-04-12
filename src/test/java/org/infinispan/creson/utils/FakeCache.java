package org.infinispan.creson.utils;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Pierre Sutra
 */
public class FakeCache<K, V> implements BasicCache<K, V> {

   @Override public String getName() {
      return "fake";  // TODO: Customise this generated block
   }

   @Override public String getVersion() {
      return null;  // TODO: Customise this generated block
   }

   @Override public int size() {
      return 0;  // TODO: Customise this generated block
   }

   @Override public boolean isEmpty() {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean containsKey(Object key) {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean containsValue(Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V get(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
      // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit,
         long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit,
         long maxIdleTime,
         TimeUnit maxIdleTimeUnit) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V remove(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void putAll(Map<? extends K, ? extends V> m) {
      // TODO: Customise this generated block
   }

   @Override public void clear() {
      // TODO: Customise this generated block
   }

   @Override public Set<K> keySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override public Collection<V> values() {
      return null;  // TODO: Customise this generated block
   }

   @Override public Set<Entry<K, V>> entrySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
         TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan,
         TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan,
         TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Void> clearAsync() {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit,
         long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> removeAsync(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> replaceAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit,
         long maxIdle,
         TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan,
         TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan,
         TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override public NotifyingFuture<V> getAsync(K key) {
      return null;  // TODO: Customise this generated block
   }

   @Override public V putIfAbsent(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public boolean remove(Object key, Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override public boolean replace(K key, V oldValue, V newValue) {
      return false;  // TODO: Customise this generated block
   }

   @Override public V replace(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override public void start() {
      // TODO: Customise this generated block
   }

   @Override public void stop() {
      // TODO: Customise this generated block
   }
}
